package com.techstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TekraApiService {

    private final RestTemplate restTemplate;

    @Value("${tekra.api.base-url:https://tekra.bg/shop/api}")
    private String baseUrl;

    @Value("${tekra.api.access-token}")
    private String accessToken;

    @Value("${tekra.api.enabled:false}")
    private boolean tekraApiEnabled;

    /** TEKRA answers 429 after a handful of quick requests, so pages of one category are spaced out. */
    @Value("${tekra.api.page-delay-ms:10000}")
    private long pageDelayMs;

    /** First wait after a 429, doubled on each retry; TEKRA locks for longer periods. */
    @Value("${tekra.api.retry-delay-ms:60000}")
    private long retryDelayMs;

    private final Map<String, List<Map<String, Object>>> productsCache = new HashMap<>();
    private long cacheTimestamp = 0;
    // Long enough for the nightly products sync to reuse what the parameters sync just read: with paging,
    // a second read of every category doubles the requests TEKRA rate-limits, and the two syncs would see
    // different feeds.
    private static final long CACHE_DURATION_MS = 30 * 60 * 1000;

    /** Products per page asked of the feed; a page of TEKRA holds at most 100. */
    static final int PAGE_SIZE = 100;
    /** Guards against a feed that never stops sending new products; far above any category today. */
    static final int MAX_PAGES = 30;

    /**
     * Get categories using JSON parsing (categories return JSON)
     */
    public List<Map<String, Object>> getCategoriesRaw() {
        if (!tekraApiEnabled) {
            log.warn("Tekra API is disabled");
            return new ArrayList<>();
        }

        try {
            log.info("Fetching categories from Tekra API");

            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("action", "categories")
                    .queryParam("access_token_feed", accessToken)
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                log.error("Received null response from Tekra API");
                return new ArrayList<>();
            }

            List<Map<String, Object>> categories = extractTekraCategoriesFromResponse(response);
            log.info("Extracted {} categories from Tekra response", categories.size());
            return categories;

        } catch (Exception e) {
            log.error("Error fetching categories from Tekra API", e);
            return new ArrayList<>();
        }
    }

    /**
     * Test API connectivity
     */
    public boolean testConnectionRaw() {
        if (!tekraApiEnabled) {
            return false;
        }

        try {
            List<Map<String, Object>> categories = getCategoriesRaw();
            boolean isConnected = !categories.isEmpty();
            log.info("Tekra API connection test: {}", isConnected ? "SUCCESS" : "FAILED");
            return isConnected;
        } catch (Exception e) {
            log.error("Tekra API connection test failed", e);
            return false;
        }
    }

    // JSON parsing for categories (same as before)
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractTekraCategoriesFromResponse(Map<String, Object> response) {
        List<Map<String, Object>> categories = new ArrayList<>();

        try {
            Object dataObj = response.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                Object categoriesObj = dataMap.get("categories");
                if (categoriesObj instanceof List) {
                    List<?> categoriesList = (List<?>) categoriesObj;
                    for (Object item : categoriesList) {
                        if (item instanceof Map) {
                            categories.add((Map<String, Object>) item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting categories from Tekra response", e);
        }

        return categories;
    }

    /**
     * Extract all unique parameters from Tekra products XML
     */
    public Map<String, Set<String>> extractTekraParametersFromProducts(String categorySlug) {
        Map<String, Set<String>> parametersMap = new HashMap<>();

        try {
            List<Map<String, Object>> products = getProductsRaw(categorySlug);

            for (Map<String, Object> product : products) {
                // Extract all prop_* fields as parameters
                for (Map.Entry<String, Object> entry : product.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (key.startsWith("prop_") && value != null) {
                        String parameterName = key.substring(5); // Remove "prop_" prefix
                        List<?> texts = value instanceof List<?> list ? list : List.of(value);

                        for (Object text : texts) {
                            String parameterValue = text.toString().trim();
                            if (!parameterValue.isEmpty()) {
                                parametersMap.computeIfAbsent(parameterName, k -> new HashSet<>()).add(parameterValue);
                            }
                        }
                    }
                }
            }

            log.info("Extracted {} unique parameters from Tekra products", parametersMap.size());

        } catch (Exception e) {
            log.error("Error extracting parameters from Tekra products", e);
        }

        return parametersMap;
    }

    /**
     * Extract all unique manufacturers from Tekra products
     */
    public Set<String> extractTekraManufacturersFromProducts(String categorySlug) {
        Set<String> manufacturers = new HashSet<>();

        try {
            List<Map<String, Object>> products = getProductsRaw(categorySlug);

            for (Map<String, Object> product : products) {
                String manufacturer = getStringValue(product, "manufacturer");
                if (manufacturer != null && !manufacturer.isEmpty()) {
                    manufacturers.add(manufacturer);
                }
            }

            log.info("Extracted {} unique manufacturers from Tekra products: {}",
                    manufacturers.size(), manufacturers);

        } catch (Exception e) {
            log.error("Error extracting manufacturers from Tekra products", e);
        }

        return manufacturers;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }

    List<Map<String, Object>> parseProductsFromXML(String xmlResponse) {
        List<Map<String, Object>> products = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes("UTF-8")));

            NodeList itemNodes = document.getElementsByTagName("item");
            log.info("Found {} product items in XML", itemNodes.getLength());

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node itemNode = itemNodes.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element itemElement = (Element) itemNode;
                    Map<String, Object> product = extractProductFromXMLElement(itemElement);
                    if (product != null) {
                        products.add(product);

                        // Log first product for debugging
                        if (i == 0) {
                            log.info("First product fields: {}", product.keySet());
                            log.info("First product sample: name={}, sku={}, price={}",
                                    product.get("name"), product.get("sku"), product.get("price"));
                        }
                    }
                }
            }

        } catch (Exception e) {
            // An empty list here read as "the category has no products", and the sync then hid them all.
            throw new IllegalStateException("Tekra products XML could not be parsed", e);
        }

        return products;
    }

    private Map<String, Object> extractProductFromXMLElement(Element itemElement) {
        Map<String, Object> product = new HashMap<>();

        try {
            // Extract all child elements
            NodeList childNodes = itemElement.getChildNodes();

            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) childNode;
                    String tagName = childElement.getTagName();
                    String textContent = childElement.getTextContent();

                    // Handle special cases
                    if ("gallery".equals(tagName)) {
                        product.put(tagName, extractGalleryImages(childElement));
                    } else if ("files".equals(tagName)) {
                        product.put(tagName, extractFiles(childElement));
                    } else if (textContent != null && !textContent.trim().isEmpty()) {
                        putText(product, tagName, textContent.trim());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error extracting product from XML element", e);
            return null;
        }

        return product;
    }

    /**
     * A property tag ({@code prop_*}) can repeat inside one item, each occurrence a value of its own; they
     * are kept as a list. Before, each occurrence overwrote the previous one and only the last value reached
     * the shop. Other tags keep the last value, as they always did.
     */
    @SuppressWarnings("unchecked")
    static void putText(Map<String, Object> product, String tagName, String text) {
        Object previous = product.get(tagName);
        if (previous == null || !tagName.startsWith("prop_")) {
            product.put(tagName, text);
        } else if (previous instanceof List<?> values) {
            ((List<String>) values).add(text);
        } else {
            product.put(tagName, new ArrayList<>(List.of(previous.toString(), text)));
        }
    }

    private List<String> extractGalleryImages(Element galleryElement) {
        List<String> images = new ArrayList<>();
        NodeList imageNodes = galleryElement.getElementsByTagName("image");

        for (int i = 0; i < imageNodes.getLength(); i++) {
            String imageUrl = imageNodes.item(i).getTextContent();
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                images.add(imageUrl.trim());
            }
        }

        return images;
    }

    private List<Map<String, String>> extractFiles(Element filesElement) {
        List<Map<String, String>> files = new ArrayList<>();
        NodeList fileNodes = filesElement.getElementsByTagName("file");

        for (int i = 0; i < fileNodes.getLength(); i++) {
            if (fileNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element fileElement = (Element) fileNodes.item(i);
                Map<String, String> file = new HashMap<>();

                NodeList fileChildNodes = fileElement.getChildNodes();
                for (int j = 0; j < fileChildNodes.getLength(); j++) {
                    Node fileChildNode = fileChildNodes.item(j);
                    if (fileChildNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element fileChildElement = (Element) fileChildNode;
                        file.put(fileChildElement.getTagName(), fileChildElement.getTextContent());
                    }
                }

                if (!file.isEmpty()) {
                    files.add(file);
                }
            }
        }

        return files;
    }

    /**
     * Get raw products XML response for debugging
     */
    public String getRawProductsXML(String categorySlug) {
        if (!tekraApiEnabled) {
            return "Tekra API is disabled";
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("action", "browse")
                    .queryParam("catSlug", categorySlug)
                    .queryParam("page", 1)
                    .queryParam("perPage", 5)
                    .queryParam("allProducts", 0)
                    .queryParam("in_stock", 1)
                    .queryParam("out_of_stock", 1)
                    .queryParam("order", "bestsellers")
                    .queryParam("feed", 1)
                    .queryParam("access_token_feed", accessToken)
                    .toUriString();

            return restTemplate.getForObject(url, String.class);

        } catch (Exception e) {
            log.error("Error getting raw products XML", e);
            return "Error: " + e.getMessage();
        }
    }

    public void clearCache() {
        productsCache.clear();
        cacheTimestamp = 0;
        log.info("Cleared Tekra API products cache");
    }

    /**
     * Check if cache is still valid
     */
    private boolean isCacheValid() {
        return (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS;
    }

    /**
     * Every product of a TEKRA category, page by page. Only the first page used to be read, so no
     * category went past 100 products — "IP системи" has 391. Paging stops at a short page, or at a
     * page that brings no new SKU (what TEKRA ignoring {@code page} would look like).
     *
     * @throws RuntimeException when a page cannot be read. A partial list is not returned: the product
     *                          sync marks every product it did not see as unavailable.
     */
    public List<Map<String, Object>> getProductsRaw(String categorySlug) {
        if (!tekraApiEnabled) {
            log.warn("Tekra API is disabled");
            return new ArrayList<>();
        }

        // Check cache first
        if (isCacheValid() && productsCache.containsKey(categorySlug)) {
            log.debug("Returning cached products for category: {}", categorySlug);
            return productsCache.get(categorySlug);
        }

        List<Map<String, Object>> products = new ArrayList<>();
        Set<String> skus = new HashSet<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            if (page > 1) {
                pause(pageDelayMs);
            }
            List<Map<String, Object>> pageProducts = fetchProductsPage(categorySlug, page);

            int newSkus = 0;
            for (Map<String, Object> product : pageProducts) {
                Object sku = product.get("sku");
                if (sku == null || skus.add(sku.toString())) {
                    products.add(product);
                    if (sku != null) newSkus++;
                }
            }
            log.info("Tekra category '{}': page {} gave {} products, {} new", categorySlug, page,
                    pageProducts.size(), newSkus);

            if (pageProducts.size() < PAGE_SIZE || newSkus == 0) {
                break;
            }
            if (page == MAX_PAGES) {
                log.warn("Tekra category '{}': stopped after {} pages with more to come", categorySlug, MAX_PAGES);
            }
        }

        productsCache.put(categorySlug, products);
        cacheTimestamp = System.currentTimeMillis();

        log.info("Extracted {} products for Tekra category '{}' (cached)", products.size(), categorySlug);
        return products;
    }

    private List<Map<String, Object>> fetchProductsPage(String categorySlug, int page) {
        int maxRetries = 3;
        long delayMs = retryDelayMs;

        for (int attempt = 1; ; attempt++) {
            try {
                log.info("Fetching products for category: {}, page {} (attempt {}/{})",
                        categorySlug, page, attempt, maxRetries);

                String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                        .queryParam("action", "browse")
                        .queryParam("catSlug", categorySlug)
                        .queryParam("page", page)
                        .queryParam("perPage", PAGE_SIZE)
                        .queryParam("allProducts", 0)
                        .queryParam("in_stock", 1)
                        .queryParam("out_of_stock", 1)
                        .queryParam("order", "bestsellers")
                        .queryParam("feed", 1)
                        .queryParam("access_token_feed", accessToken)
                        .toUriString();

                String xmlResponse = restTemplate.getForObject(url, String.class);
                if (xmlResponse == null) {
                    throw new IllegalStateException(
                            "Empty response from Tekra for category '" + categorySlug + "', page " + page);
                }
                return parseProductsFromXML(xmlResponse);

            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt >= maxRetries) {
                    log.error("Tekra API still returning 429 after {} attempts for category '{}', page {}",
                            maxRetries, categorySlug, page);
                    throw e; // propagate so caller can apply cooldown before next category
                }
                log.warn("Tekra API rate limited (429) for category '{}', page {}, attempt {}/{}. Waiting {} ms before retry.",
                        categorySlug, page, attempt, maxRetries, delayMs);
                pause(delayMs);
                delayMs *= 2;
            }
        }
    }

    private static void pause(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the Tekra API", e);
        }
    }
}