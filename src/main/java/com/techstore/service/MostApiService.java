package com.techstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MostApiService {

    private final RestTemplate restTemplate;

    @Value("${most.api.url:https://most.traveldatabank.biz/ProductXML}")
    private String apiUrl;

    @Value("${most.api.enabled:false}")
    private boolean mostApiEnabled;

    private List<Map<String, Object>> productsCache;
    private long cacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    /**
     * Test API connectivity
     */
    public boolean testConnection() {
        if (!mostApiEnabled) {
            log.warn("Most API is disabled");
            return false;
        }

        try {
            List<Map<String, Object>> products = getAllProducts();
            boolean isConnected = !products.isEmpty();
            log.info("Most API connection test: {}", isConnected ? "SUCCESS" : "FAILED");
            return isConnected;
        } catch (Exception e) {
            log.error("Most API connection test failed", e);
            return false;
        }
    }

    /**
     * Get all products from Most API with caching
     */
    public List<Map<String, Object>> getAllProducts() {
        if (!mostApiEnabled) {
            log.warn("Most API is disabled");
            return new ArrayList<>();
        }

        // Check cache
        if (isCacheValid() && productsCache != null) {
            log.debug("Returning cached products from Most API");
            return productsCache;
        }

        try {
            log.info("Fetching products from Most API: {}", apiUrl);

            String xmlResponse = restTemplate.getForObject(apiUrl, String.class);

            if (xmlResponse == null || xmlResponse.isEmpty()) {
                log.error("Received null or empty XML response from Most API");
                return new ArrayList<>();
            }

            List<Map<String, Object>> products = parseProductsFromXML(xmlResponse);

            // Update cache
            productsCache = products;
            cacheTimestamp = System.currentTimeMillis();

            log.info("Successfully fetched {} products from Most API", products.size());
            return products;

        } catch (Exception e) {
            log.error("Error fetching products from Most API", e);
            return new ArrayList<>();
        }
    }

    /**
     * Parse XML response and extract products
     */
    private List<Map<String, Object>> parseProductsFromXML(String xmlResponse) {
        List<Map<String, Object>> products = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes("UTF-8")));

            NodeList productNodes = document.getElementsByTagName("product");
            log.info("Found {} product nodes in Most XML", productNodes.getLength());

            for (int i = 0; i < productNodes.getLength(); i++) {
                Node productNode = productNodes.item(i);
                if (productNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element productElement = (Element) productNode;
                    Map<String, Object> product = extractProductFromElement(productElement);
                    if (product != null && !product.isEmpty()) {
                        products.add(product);
                    }
                }
            }

            log.info("Successfully parsed {} products from Most XML", products.size());

        } catch (Exception e) {
            log.error("Error parsing Most XML", e);
        }

        return products;
    }

    /**
     * Extract product data from XML element
     */
    private Map<String, Object> extractProductFromElement(Element productElement) {
        Map<String, Object> product = new HashMap<>();

        try {
            // Basic fields
            product.put("id", getElementText(productElement, "id"));
            product.put("uid", getElementText(productElement, "uid"));
            product.put("code", getElementText(productElement, "code"));
            product.put("name", getElementText(productElement, "name"));
            product.put("searchstring", getElementText(productElement, "searchstring"));
            product.put("product_status", getElementText(productElement, "product_status"));
            product.put("haspromo", getElementText(productElement, "haspromo"));
            product.put("general_description", getElementText(productElement, "general_description"));

            // Category information
            product.put("classname", getElementText(productElement, "classname"));
            product.put("classname_full", getElementText(productElement, "classname_full"));
            product.put("class_id", getElementText(productElement, "class_id"));

            // Pricing
            product.put("price", getElementText(productElement, "price"));
            product.put("currency", getElementText(productElement, "currency"));

            // Images
            product.put("main_picture_url", getElementText(productElement, "main_picture_url"));
            product.put("gallery", extractGallery(productElement));

            // Manufacturer and category
            Element manufacturerElement = (Element) productElement.getElementsByTagName("manufacturer").item(0);
            if (manufacturerElement != null) {
                product.put("manufacturer_id", manufacturerElement.getAttribute("id"));
                product.put("manufacturer", manufacturerElement.getTextContent());
            }

            Element categoryElement = (Element) productElement.getElementsByTagName("category").item(0);
            if (categoryElement != null) {
                product.put("category_id", categoryElement.getAttribute("id"));
                product.put("category", categoryElement.getTextContent());
            }

            Element subcategoryElement = (Element) productElement.getElementsByTagName("subcategory").item(0);
            if (subcategoryElement != null) {
                product.put("subcategory_id", subcategoryElement.getAttribute("id"));
                product.put("subcategory", subcategoryElement.getTextContent());
            }

            // Part number
            product.put("partnum", getElementText(productElement, "partnum"));
            product.put("vendor_url", getElementText(productElement, "vendor_url"));

            // Properties (parameters)
            product.put("properties", extractProperties(productElement));

        } catch (Exception e) {
            log.error("Error extracting product from element", e);
            return null;
        }

        return product;
    }

    /**
     * Extract gallery images
     */
    private List<String> extractGallery(Element productElement) {
        List<String> gallery = new ArrayList<>();

        try {
            NodeList galleryNodes = productElement.getElementsByTagName("gallery");
            if (galleryNodes.getLength() > 0) {
                Element galleryElement = (Element) galleryNodes.item(0);
                NodeList pictureNodes = galleryElement.getElementsByTagName("picture");

                for (int i = 0; i < pictureNodes.getLength(); i++) {
                    Element pictureElement = (Element) pictureNodes.item(i);
                    String pictureUrl = getElementText(pictureElement, "picture_url");
                    if (pictureUrl != null && !pictureUrl.isEmpty()) {
                        gallery.add(pictureUrl);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting gallery", e);
        }

        return gallery;
    }

    /**
     * Extract properties (parameters)
     */
    private Map<String, String> extractProperties(Element productElement) {
        Map<String, String> properties = new LinkedHashMap<>();

        try {
            NodeList propertiesNodes = productElement.getElementsByTagName("properties");
            if (propertiesNodes.getLength() > 0) {
                Element propertiesElement = (Element) propertiesNodes.item(0);
                NodeList propertyNodes = propertiesElement.getElementsByTagName("property");

                for (int i = 0; i < propertyNodes.getLength(); i++) {
                    Element propertyElement = (Element) propertyNodes.item(i);
                    String name = propertyElement.getAttribute("name");
                    String value = propertyElement.getTextContent();

                    if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
                        properties.put(name, value);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting properties", e);
        }

        return properties;
    }

    /**
     * Extract unique categories from all products
     */
    public Set<String> extractUniqueCategories() {
        Set<String> categories = new HashSet<>();

        try {
            List<Map<String, Object>> products = getAllProducts();

            for (Map<String, Object> product : products) {
                String category = (String) product.get("category");
                String subcategory = (String) product.get("subcategory");

                if (category != null && !category.isEmpty()) {
                    categories.add(category);
                }
                if (subcategory != null && !subcategory.isEmpty()) {
                    categories.add(subcategory);
                }
            }

            log.info("Extracted {} unique categories from Most products", categories.size());

        } catch (Exception e) {
            log.error("Error extracting categories", e);
        }

        return categories;
    }

    /**
     * Extract unique manufacturers from all products
     */
    public Set<String> extractUniqueManufacturers() {
        Set<String> manufacturers = new HashSet<>();

        try {
            List<Map<String, Object>> products = getAllProducts();

            for (Map<String, Object> product : products) {
                String manufacturer = (String) product.get("manufacturer");
                if (manufacturer != null && !manufacturer.isEmpty()) {
                    manufacturers.add(manufacturer);
                }
            }

            log.info("Extracted {} unique manufacturers from Most products", manufacturers.size());

        } catch (Exception e) {
            log.error("Error extracting manufacturers", e);
        }

        return manufacturers;
    }

    /**
     * Extract unique parameters from all products
     */
    public Map<String, Set<String>> extractUniqueParameters() {
        Map<String, Set<String>> parametersMap = new HashMap<>();

        try {
            List<Map<String, Object>> products = getAllProducts();

            for (Map<String, Object> product : products) {
                @SuppressWarnings("unchecked")
                Map<String, String> properties = (Map<String, String>) product.get("properties");

                if (properties != null) {
                    for (Map.Entry<String, String> entry : properties.entrySet()) {
                        String paramName = entry.getKey();
                        String paramValue = entry.getValue();

                        if (paramValue != null && !paramValue.isEmpty() && !"-".equals(paramValue)) {
                            parametersMap.computeIfAbsent(paramName, k -> new HashSet<>()).add(paramValue);
                        }
                    }
                }
            }

            log.info("Extracted {} unique parameters from Most products", parametersMap.size());

        } catch (Exception e) {
            log.error("Error extracting parameters", e);
        }

        return parametersMap;
    }

    /**
     * Get products by category
     */
    public List<Map<String, Object>> getProductsByCategory(String categoryName) {
        List<Map<String, Object>> categoryProducts = new ArrayList<>();

        try {
            List<Map<String, Object>> allProducts = getAllProducts();

            for (Map<String, Object> product : allProducts) {
                String productCategory = (String) product.get("category");
                String productSubcategory = (String) product.get("subcategory");

                if (categoryName.equals(productCategory) || categoryName.equals(productSubcategory)) {
                    categoryProducts.add(product);
                }
            }

            log.debug("Found {} products for category: {}", categoryProducts.size(), categoryName);

        } catch (Exception e) {
            log.error("Error getting products by category", e);
        }

        return categoryProducts;
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        productsCache = null;
        cacheTimestamp = 0;
        log.info("Cleared Most API products cache");
    }

    /**
     * Check if cache is valid
     */
    private boolean isCacheValid() {
        return productsCache != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS;
    }

    /**
     * Helper method to get element text content
     */
    private String getElementText(Element parent, String tagName) {
        try {
            NodeList nodeList = parent.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                String text = nodeList.item(0).getTextContent();
                return text != null ? text.trim() : null;
            }
        } catch (Exception e) {
            log.trace("Error getting element text for tag: {}", tagName);
        }
        return null;
    }
}