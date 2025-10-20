package com.techstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
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
public class AsbisApiService {

    private final RestTemplate restTemplate;

    @Value("${asbis.api.base-url:https://services.it4profit.com/product/bg/714}")
    private String baseUrl;

    @Value("${asbis.api.username}")
    private String username;

    @Value("${asbis.api.password}")
    private String password;

    @Value("${asbis.api.enabled:false}")
    private boolean asbisApiEnabled;

    private final Map<String, List<Map<String, Object>>> productsCache = new HashMap<>();
    private long cacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000;

    public boolean testConnection() {
        if (!asbisApiEnabled) {
            log.warn("Asbis API is disabled");
            return false;
        }

        try {
            String xmlResponse = getRawProductListXML();
            boolean isConnected = xmlResponse != null && !xmlResponse.isEmpty()
                    && xmlResponse.contains("<ProductCatalog");
            log.info("Asbis API connection test: {}", isConnected ? "SUCCESS" : "FAILED");
            return isConnected;
        } catch (Exception e) {
            log.error("Asbis API connection test failed", e);
            return false;
        }
    }

    public String getRawProductListXML() {
        if (!asbisApiEnabled) {
            return "Asbis API is disabled";
        }

        try {
            String url = buildApiUrl("ProductList.xml");
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("Error getting raw Asbis XML", e);
            return "Error: " + e.getMessage();
        }
    }

    public List<Map<String, Object>> getAllProducts() {
        if (!asbisApiEnabled) {
            log.warn("Asbis API is disabled");
            return new ArrayList<>();
        }

        if (isCacheValid() && productsCache.containsKey("all_products")) {
            log.debug("Returning cached Asbis products");
            return productsCache.get("all_products");
        }

        try {
            log.info("Fetching all products from Asbis API");
            String xmlResponse = getRawProductListXML();

            if (xmlResponse == null || xmlResponse.isEmpty()) {
                log.error("Received null or empty XML response");
                return new ArrayList<>();
            }

            List<Map<String, Object>> products = parseProductsFromXML(xmlResponse);

            productsCache.put("all_products", products);
            cacheTimestamp = System.currentTimeMillis();

            log.info("Fetched {} products from Asbis API", products.size());
            return products;

        } catch (Exception e) {
            log.error("Error fetching products from Asbis API", e);
            return new ArrayList<>();
        }
    }

    /**
     * Extract categories with hierarchy from ProductType field
     * ProductType format: "Level1 - Level2 - Level3"
     */
    public List<Map<String, Object>> extractCategories() {
        try {
            List<Map<String, Object>> products = getAllProducts();

            // Use TreeSet to maintain unique categories per level
            Set<String> allProductTypes = new HashSet<>();

            for (Map<String, Object> product : products) {
                String productType = getString(product, "producttype");
                if (productType != null) {
                    allProductTypes.add(productType);
                }
            }

            List<Map<String, Object>> categories = buildCategoryHierarchy(allProductTypes);

            log.info("Extracted {} hierarchical categories from Asbis products", categories.size());
            return categories;

        } catch (Exception e) {
            log.error("Error extracting categories from Asbis products", e);
            return new ArrayList<>();
        }
    }

    /**
     * Build hierarchical category structure
     * Format: "Level1 - Level2 - Level3"
     */
    private List<Map<String, Object>> buildCategoryHierarchy(Set<String> productTypes) {
        List<Map<String, Object>> categories = new ArrayList<>();

        // Track processed categories at each level to avoid duplicates
        Set<String> processedLevel1 = new HashSet<>();
        Set<String> processedLevel2 = new HashSet<>();
        Set<String> processedLevel3 = new HashSet<>();

        String delimiter = " - "; // Asbis uses " - " as delimiter

        for (String productType : productTypes) {
            String[] parts = productType.split(delimiter);

            // Level 1 - Root category
            if (parts.length >= 1) {
                String level1 = parts[0].trim();

                if (!processedLevel1.contains(level1)) {
                    Map<String, Object> category = new HashMap<>();
                    category.put("id", level1);
                    category.put("name", level1);
                    category.put("level", 1);
                    category.put("parent", null);
                    category.put("fullPath", level1);
                    categories.add(category);
                    processedLevel1.add(level1);
                }
            }

            // Level 2 - Subcategory
            if (parts.length >= 2) {
                String level1 = parts[0].trim();
                String level2 = parts[1].trim();
                String level2Key = level1 + "|" + level2; // Unique key

                if (!processedLevel2.contains(level2Key)) {
                    Map<String, Object> category = new HashMap<>();
                    category.put("id", level2Key);
                    category.put("name", level2);
                    category.put("level", 2);
                    category.put("parent", level1);
                    category.put("fullPath", level1 + " - " + level2);
                    categories.add(category);
                    processedLevel2.add(level2Key);
                }
            }

            // Level 3 - Sub-subcategory
            if (parts.length >= 3) {
                String level1 = parts[0].trim();
                String level2 = parts[1].trim();
                String level3 = parts[2].trim();
                String level2Key = level1 + "|" + level2;
                String level3Key = level1 + "|" + level2 + "|" + level3; // Unique key

                if (!processedLevel3.contains(level3Key)) {
                    Map<String, Object> category = new HashMap<>();
                    category.put("id", level3Key);
                    category.put("name", level3);
                    category.put("level", 3);
                    category.put("parent", level2Key);
                    category.put("parentName", level2);
                    category.put("fullPath", level1 + " - " + level2 + " - " + level3);
                    categories.add(category);
                    processedLevel3.add(level3Key);
                }
            }
        }

        log.info("Built category hierarchy: L1={}, L2={}, L3={}",
                processedLevel1.size(), processedLevel2.size(), processedLevel3.size());

        return categories;
    }

    public Set<String> extractManufacturers() {
        try {
            List<Map<String, Object>> products = getAllProducts();
            Set<String> manufacturers = new HashSet<>();

            for (Map<String, Object> product : products) {
                String vendor = getString(product, "vendor");
                if (vendor != null && !vendor.isEmpty()) {
                    manufacturers.add(vendor);
                }
            }

            log.info("Extracted {} unique manufacturers from Asbis products", manufacturers.size());
            return manufacturers;

        } catch (Exception e) {
            log.error("Error extracting manufacturers from Asbis products", e);
            return new HashSet<>();
        }
    }

    public Map<String, Set<String>> extractParameters() {
        Map<String, Set<String>> parametersMap = new HashMap<>();

        try {
            List<Map<String, Object>> products = getAllProducts();

            for (Map<String, Object> product : products) {
                Object attrListObj = product.get("attrlist");
                if (attrListObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> attrList = (Map<String, String>) attrListObj;

                    for (Map.Entry<String, String> entry : attrList.entrySet()) {
                        String paramName = entry.getKey();
                        String paramValue = entry.getValue();

                        if (paramValue != null && !paramValue.trim().isEmpty()) {
                            parametersMap.computeIfAbsent(paramName, k -> new HashSet<>()).add(paramValue);
                        }
                    }
                }
            }

            log.info("Extracted {} unique parameters from Asbis products", parametersMap.size());

        } catch (Exception e) {
            log.error("Error extracting parameters from Asbis products", e);
        }

        return parametersMap;
    }

    private List<Map<String, Object>> parseProductsFromXML(String xmlResponse) {
        List<Map<String, Object>> products = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes("UTF-8")));

            NodeList productNodes = document.getElementsByTagName("Product");
            log.info("Found {} Product nodes in XML", productNodes.getLength());

            for (int i = 0; i < productNodes.getLength(); i++) {
                Node productNode = productNodes.item(i);
                if (productNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element productElement = (Element) productNode;
                    Map<String, Object> product = extractProductFromXMLElement(productElement);
                    if (product != null && !product.isEmpty()) {
                        products.add(product);

                        if (i < 3) {
                            log.debug("Sample product #{}: code={}, vendor={}, name={}",
                                    i + 1,
                                    product.get("productcode"),
                                    product.get("vendor"),
                                    product.get("productdescription"));
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error parsing products XML", e);
        }

        return products;
    }

    private Map<String, Object> extractProductFromXMLElement(Element productElement) {
        Map<String, Object> product = new HashMap<>();

        try {
            // Extract simple fields
            product.put("productcode", getElementText(productElement, "ProductCode"));
            product.put("vendor", getElementText(productElement, "Vendor"));
            product.put("producttype", getElementText(productElement, "ProductType"));
            product.put("productcategory", getElementText(productElement, "ProductCategory"));
            product.put("productdescription", getElementText(productElement, "ProductDescription"));
            product.put("image", getElementText(productElement, "Image"));
            product.put("productcard", getElementText(productElement, "ProductCard"));

            // Extract AttrList (parameters)
            NodeList attrListNodes = productElement.getElementsByTagName("AttrList");
            if (attrListNodes.getLength() > 0) {
                Element attrListElement = (Element) attrListNodes.item(0);
                Map<String, String> attributes = extractAttrList(attrListElement);
                product.put("attrlist", attributes);
            }

            // Extract Images
            NodeList imagesNodes = productElement.getElementsByTagName("Images");
            if (imagesNodes.getLength() > 0) {
                Element imagesElement = (Element) imagesNodes.item(0);
                List<String> imagesList = extractImages(imagesElement);
                product.put("images", imagesList);
            }

        } catch (Exception e) {
            log.error("Error extracting product from XML element", e);
            return null;
        }

        return product;
    }

    private Map<String, String> extractAttrList(Element attrListElement) {
        Map<String, String> attributes = new HashMap<>();

        NodeList elementNodes = attrListElement.getElementsByTagName("element");
        for (int i = 0; i < elementNodes.getLength(); i++) {
            if (elementNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) elementNodes.item(i);
                String name = element.getAttribute("Name");
                String value = element.getAttribute("Value");

                if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
                    attributes.put(name, value);
                }
            }
        }

        return attributes;
    }

    private List<String> extractImages(Element imagesElement) {
        List<String> images = new ArrayList<>();
        NodeList imageNodes = imagesElement.getElementsByTagName("Image");

        for (int i = 0; i < imageNodes.getLength(); i++) {
            String imageUrl = imageNodes.item(i).getTextContent();
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                images.add(imageUrl.trim());
            }
        }

        return images;
    }

    private String getElementText(Element parent, String tagName) {
        try {
            NodeList nodes = parent.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                String text = nodes.item(0).getTextContent();
                return text != null ? text.trim() : null;
            }
        } catch (Exception e) {
            log.debug("Error getting element text for tag: {}", tagName);
        }
        return null;
    }

    private String buildApiUrl(String endpoint) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl + "/" + endpoint)
                .queryParam("USERNAME", username)
                .queryParam("PASSWORD", password)
                .toUriString();
    }

    private String sanitizeUrl(String url) {
        if (url == null) return null;
        return url.replaceAll("PASSWORD=[^&]+", "PASSWORD=***");
    }

    public void clearCache() {
        productsCache.clear();
        cacheTimestamp = 0;
        log.info("Cleared Asbis API products cache");
    }

    private boolean isCacheValid() {
        return (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS;
    }

    /**
     * Get category statistics by level
     */
    public Map<String, Object> getCategoryStatistics() {
        try {
            List<Map<String, Object>> categories = extractCategories();

            long level1Count = categories.stream()
                    .filter(cat -> getInteger(cat, "level") == 1)
                    .count();

            long level2Count = categories.stream()
                    .filter(cat -> getInteger(cat, "level") == 2)
                    .count();

            long level3Count = categories.stream()
                    .filter(cat -> getInteger(cat, "level") == 3)
                    .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", categories.size());
            stats.put("level1", level1Count);
            stats.put("level2", level2Count);
            stats.put("level3", level3Count);
            stats.put("breakdown", Map.of(
                    "rootCategories", level1Count,
                    "subcategories", level2Count,
                    "subSubcategories", level3Count
            ));

            return stats;

        } catch (Exception e) {
            log.error("Error getting category statistics", e);
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", e.getMessage());
            return errorStats;
        }
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            String strValue = value.toString().trim().replaceAll("[^0-9.]", "");
            return new BigDecimal(strValue);
        } catch (Exception e) {
            log.debug("Could not parse BigDecimal from value: {}", value);
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}