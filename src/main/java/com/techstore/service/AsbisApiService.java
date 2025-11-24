package com.techstore.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Value("${asbis.api.retry-attempts:3}")
    private int retryAttempts;

    @Value("${asbis.api.retry-delay:2000}")
    private long retryDelay;

    private final Cache<String, List<Map<String, Object>>> asbisProductsCache;

    public AsbisApiService(
            @Qualifier("asbisRestTemplate") RestTemplate restTemplate,
            Cache<String, List<Map<String, Object>>> asbisProductsCache) {
        this.restTemplate = restTemplate;
        this.asbisProductsCache = asbisProductsCache;
    }

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

        int attempt = 0;
        Exception lastException = null;

        while (attempt < retryAttempts) {
            try {
                String url = buildApiUrl("ProductList.xml");
                log.info("Fetching ProductList.xml (attempt {}/{})", attempt + 1, retryAttempts);

                String response = restTemplate.getForObject(url, String.class);

                if (response != null && !response.isEmpty()) {
                    log.info("✓ Successfully fetched ProductList.xml on attempt {}", attempt + 1);
                    return response;
                }

            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt < retryAttempts) {
                    // Exponential backoff: 2s → 4s → 8s
                    long delay = retryDelay * (long) Math.pow(2, attempt - 1);
                    log.warn("⟳ Retry attempt {}/{} after {}ms: {}",
                            attempt + 1, retryAttempts, delay, e.getMessage());

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted during retry delay");
                        break;
                    }
                }
            }
        }

        log.error("✗ Failed to fetch ProductList.xml after {} attempts", retryAttempts);
        if (lastException != null) {
            throw new RuntimeException("Failed to fetch ProductList.xml after " + retryAttempts + " attempts", lastException);
        }
        return "Error: " + (lastException != null ? lastException.getMessage() : "Unknown error");
    }

    public List<Map<String, Object>> getAllProducts() {
        if (!asbisApiEnabled) {
            log.warn("Asbis API is disabled");
            return new ArrayList<>();
        }

        // ✅ Thread-safe cache lookup with automatic loading
        return asbisProductsCache.get("all_products", key -> {
            try {
                log.info("Cache miss - Fetching all products from Asbis API");
                String xmlResponse = getRawProductListXML();

                if (xmlResponse == null || xmlResponse.isEmpty()) {
                    log.error("Received null or empty XML response");
                    return new ArrayList<>();
                }

                List<Map<String, Object>> products = parseProductsFromXML(xmlResponse);
                log.info("✓ Cached {} products from Asbis API", products.size());

                return products;

            } catch (Exception e) {
                log.error("Error fetching products from Asbis API", e);
                return new ArrayList<>();
            }
        });
    }

    /**
     * Extract categories from Asbis products
     * Structure:
     * - Level 1: ProductCategory (e.g. "Игри и мултимедия")
     * - Level 2: ProductType (e.g. "Гейминг слушалки")
     */
    public List<Map<String, Object>> extractCategories() {
        try {
            List<Map<String, Object>> products = getAllProducts();

            // Track unique categories
            Map<String, Map<String, Object>> level1Map = new LinkedHashMap<>();
            Map<String, Map<String, Object>> level2Map = new LinkedHashMap<>();

            for (Map<String, Object> product : products) {
                String productCategory = getString(product, "productcategory"); // Level 1
                String productType = getString(product, "producttype");         // Level 2

                // Skip if both are null
                if (productCategory == null && productType == null) {
                    continue;
                }

                // Create Level 1 (ProductCategory - root)
                if (productCategory != null && !productCategory.trim().isEmpty()) {
                    if (!level1Map.containsKey(productCategory)) {
                        Map<String, Object> cat = new HashMap<>();
                        cat.put("id", productCategory);
                        cat.put("name", productCategory);
                        cat.put("level", 1);
                        cat.put("parent", null);
                        cat.put("fullPath", productCategory);
                        level1Map.put(productCategory, cat);
                    }
                }

                // Create Level 2 (ProductType - child of ProductCategory)
                if (productType != null && !productType.trim().isEmpty() &&
                        productCategory != null && !productCategory.trim().isEmpty()) {

                    String level2Key = productCategory + "|" + productType;

                    if (!level2Map.containsKey(level2Key)) {
                        Map<String, Object> cat = new HashMap<>();
                        cat.put("id", level2Key);
                        cat.put("name", productType);
                        cat.put("level", 2);
                        cat.put("parent", productCategory); // Points to Level 1 ID
                        cat.put("parentName", productCategory);
                        cat.put("fullPath", productCategory + " / " + productType);
                        level2Map.put(level2Key, cat);
                    }
                }
            }

            List<Map<String, Object>> categories = new ArrayList<>();
            categories.addAll(level1Map.values());
            categories.addAll(level2Map.values());

            log.info("Extracted {} hierarchical categories from Asbis products", categories.size());
            log.info("  Level 1 (ProductCategory): {} categories", level1Map.size());
            log.info("  Level 2 (ProductType): {} categories", level2Map.size());

            // Show sample categories
            if (!level1Map.isEmpty()) {
                log.info("Sample Level 1 categories: {}",
                        level1Map.keySet().stream().limit(5).toList());
            }
            if (!level2Map.isEmpty()) {
                log.info("Sample Level 2 categories: {}",
                        level2Map.values().stream().limit(5)
                                .map(c -> c.get("name"))
                                .toList());
            }

            return categories;

        } catch (Exception e) {
            log.error("Error extracting categories from Asbis products", e);
            return new ArrayList<>();
        }
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

    public String getRawPriceAvailXML() {
        if (!asbisApiEnabled) {
            return "Asbis API is disabled";
        }

        int attempt = 0;
        Exception lastException = null;

        while (attempt < retryAttempts) {
            try {
                String url = buildApiUrl("PriceAvail.xml");
                log.info("Fetching PriceAvail.xml (attempt {}/{})", attempt + 1, retryAttempts);

                String response = restTemplate.getForObject(url, String.class);

                if (response != null && !response.isEmpty()) {
                    log.info("✓ Successfully fetched PriceAvail.xml on attempt {}", attempt + 1);
                    return response;
                }

            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt < retryAttempts) {
                    long delay = retryDelay * (long) Math.pow(2, attempt - 1);
                    log.warn("⟳ Retry PriceAvail attempt {}/{} after {}ms: {}",
                            attempt + 1, retryAttempts, delay, e.getMessage());

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("✗ Failed to fetch PriceAvail.xml after {} attempts", retryAttempts);
        if (lastException != null) {
            throw new RuntimeException("Failed to fetch PriceAvail.xml", lastException);
        }
        return "Error: " + (lastException != null ? lastException.getMessage() : "Unknown error");
    }

    /**
     * Get all price and availability data from Asbis API
     * Returns a list of maps containing:
     * - productcode: Product SKU
     * - price: Current price
     * - stock: Stock quantity
     * - availability: Availability status
     * - availabilitydate: Date when product will be available
     */
    public List<Map<String, Object>> getAllPriceAvailability() {
        if (!asbisApiEnabled) {
            log.warn("Asbis API is disabled");
            return new ArrayList<>();
        }

        try {
            log.info("Fetching price and availability data from Asbis API");
            long startTime = System.currentTimeMillis();

            String xmlResponse = getRawPriceAvailXML();

            if (xmlResponse == null || xmlResponse.isEmpty()) {
                log.error("Received null or empty PriceAvail XML response");
                return new ArrayList<>();
            }

            List<Map<String, Object>> priceData = parsePriceAvailFromXML(xmlResponse);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Fetched {} price/availability records from Asbis API in {}ms",
                    priceData.size(), duration);

            return priceData;

        } catch (Exception e) {
            log.error("Error fetching price/availability from Asbis API", e);
            return new ArrayList<>();
        }
    }

    /**
     * Parse price and availability data from XML
     */
    private List<Map<String, Object>> parsePriceAvailFromXML(String xmlResponse) {
        List<Map<String, Object>> priceData = new ArrayList<>();

        try {
            // ✅ Validate XML
            if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
                log.error("Empty PriceAvail XML response");
                return priceData;
            }

            String trimmedXml = xmlResponse.trim();
            if (!trimmedXml.startsWith("<?xml") && !trimmedXml.startsWith("<")) {
                log.error("Invalid PriceAvail XML format");
                return priceData;
            }

            // ✅ Configure secure parsing
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();

            // Add error handler
            builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
                public void warning(org.xml.sax.SAXParseException e) {
                    log.warn("PriceAvail XML warning: {}", e.getMessage());
                }
                public void error(org.xml.sax.SAXParseException e) {
                    log.error("PriceAvail XML error: {}", e.getMessage());
                }
                public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
                    throw e;
                }
            });

            Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes("UTF-8")));
            document.getDocumentElement().normalize();

            NodeList productNodes = document.getElementsByTagName("Product");
            log.info("✓ Found {} Product nodes in PriceAvail XML", productNodes.getLength());

            for (int i = 0; i < productNodes.getLength(); i++) {
                Node productNode = productNodes.item(i);
                if (productNode.getNodeType() == Node.ELEMENT_NODE) {
                    try {
                        Element productElement = (Element) productNode;
                        Map<String, Object> priceItem = extractPriceAvailFromElement(productElement);

                        if (priceItem != null && !priceItem.isEmpty()) {
                            priceData.add(priceItem);

                            if (i < 3) {
                                log.debug("Sample price data #{}: code={}, price={}, stock={}, availability={}",
                                        i + 1,
                                        priceItem.get("productcode"),
                                        priceItem.get("price"),
                                        priceItem.get("stock"),
                                        priceItem.get("availability"));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error extracting price data at index {}: {}", i, e.getMessage());
                    }
                }
            }

        } catch (org.xml.sax.SAXParseException e) {
            log.error("✗ PriceAvail XML parsing failed at line {}: {}",
                    e.getLineNumber(), e.getMessage());
        } catch (Exception e) {
            log.error("✗ Error parsing PriceAvail XML: {}", e.getMessage(), e);
        }

        return priceData;
    }


    /**
     * Extract price and availability data from single XML element
     */
    private Map<String, Object> extractPriceAvailFromElement(Element element) {
        Map<String, Object> data = new HashMap<>();

        try {
            // Extract product code (mandatory)
            String productCode = getElementText(element, "ProductCode");
            if (productCode == null || productCode.trim().isEmpty()) {
                log.warn("Skipping price record with missing ProductCode");
                return null;
            }
            data.put("productcode", productCode);

            // Extract price
            String priceStr = getElementText(element, "Price");
            if (priceStr != null && !priceStr.trim().isEmpty()) {
                try {
                    BigDecimal price = new BigDecimal(priceStr.trim().replaceAll("[^0-9.]", ""));
                    data.put("price", price);
                } catch (Exception e) {
                    log.debug("Could not parse price for product {}: {}", productCode, priceStr);
                    data.put("price", null);
                }
            }

            // Extract stock quantity
            String stockStr = getElementText(element, "Stock");
            if (stockStr != null && !stockStr.trim().isEmpty()) {
                try {
                    Integer stock = Integer.parseInt(stockStr.trim());
                    data.put("stock", stock);
                } catch (Exception e) {
                    log.debug("Could not parse stock for product {}: {}", productCode, stockStr);
                    data.put("stock", 0);
                }
            } else {
                data.put("stock", 0);
            }

            // Extract availability status
            String availability = getElementText(element, "Availability");
            data.put("availability", availability);

            // Extract availability date
            String availDate = getElementText(element, "AvailabilityDate");
            data.put("availabilitydate", availDate);

            // Additional fields that might exist
            data.put("currency", getElementText(element, "Currency"));
            data.put("warehouse", getElementText(element, "Warehouse"));

        } catch (Exception e) {
            log.error("Error extracting price/availability from XML element", e);
            return null;
        }

        return data;
    }

    /**
     * Get price and availability statistics
     */
    public Map<String, Object> getPriceAvailabilityStatistics() {
        try {
            List<Map<String, Object>> priceData = getAllPriceAvailability();

            long totalRecords = priceData.size();
            long withPrice = priceData.stream()
                    .filter(p -> p.get("price") != null)
                    .count();
            long inStock = priceData.stream()
                    .filter(p -> {
                        Object stock = p.get("stock");
                        return stock != null && ((Integer) stock) > 0;
                    })
                    .count();
            long outOfStock = totalRecords - inStock;

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRecords", totalRecords);
            stats.put("withPrice", withPrice);
            stats.put("inStock", inStock);
            stats.put("outOfStock", outOfStock);

            return stats;

        } catch (Exception e) {
            log.error("Error getting price/availability statistics", e);
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", e.getMessage());
            return errorStats;
        }
    }

    private List<Map<String, Object>> parseProductsFromXML(String xmlResponse) {
        List<Map<String, Object>> products = new ArrayList<>();

        try {
            // ✅ Validate XML
            if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
                log.error("Empty XML response from Asbis API");
                return products;
            }

            String trimmedXml = xmlResponse.trim();
            if (!trimmedXml.startsWith("<?xml") && !trimmedXml.startsWith("<")) {
                log.error("Invalid XML format");
                return products;
            }

            // ✅ Configure secure XML parsing
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes("UTF-8")));
            document.getDocumentElement().normalize();

            // ✅ Find Product nodes
            NodeList productNodes = document.getElementsByTagName("Product");
            log.info("✓ Successfully parsed XML, found {} Product nodes", productNodes.getLength());

            if (productNodes.getLength() == 0) {
                log.error("No Product nodes found in XML!");
                return products;
            }

            // ✅ Parse each product
            for (int i = 0; i < productNodes.getLength(); i++) {
                Node productNode = productNodes.item(i);

                if (productNode.getNodeType() == Node.ELEMENT_NODE) {
                    try {
                        Element productElement = (Element) productNode;
                        Map<String, Object> product = new HashMap<>();

                        // Extract simple fields
                        product.put("productcode", getElementText(productElement, "ProductCode"));
                        product.put("vendor", getElementText(productElement, "Vendor"));
                        product.put("producttype", getElementText(productElement, "ProductType"));
                        product.put("productcategory", getElementText(productElement, "ProductCategory"));
                        product.put("productdescription", getElementText(productElement, "ProductDescription"));

                        // Fix image URL (remove space after "https:")
                        String imageUrl = getElementText(productElement, "Image");
                        if (imageUrl != null) {
                            imageUrl = imageUrl.replace("https: //", "https://")
                                    .replace("http: //", "http://");
                            product.put("image", imageUrl);
                        }

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

                        // Only add if we have at least productcode
                        if (product.get("productcode") != null) {
                            products.add(product);

                            // Log first 3 products for debugging
                            if (i < 3) {
                                log.info("Sample product #{}: code={}, vendor={}, category={}, type={}",
                                        i + 1,
                                        product.get("productcode"),
                                        product.get("vendor"),
                                        product.get("productcategory"),
                                        product.get("producttype"));
                            }
                        }

                    } catch (Exception e) {
                        log.error("Error extracting product at index {}: {}", i, e.getMessage());
                        // Continue with next product
                    }
                }
            }

            log.info("✓ Successfully extracted {} products from XML", products.size());

        } catch (org.xml.sax.SAXParseException e) {
            log.error("✗ XML parsing failed at line {}, column {}: {}",
                    e.getLineNumber(), e.getColumnNumber(), e.getMessage());
        } catch (Exception e) {
            log.error("✗ Error parsing products XML: {}", e.getMessage(), e);
        }

        return products;
    }

    /**
     * Force refresh products from API (clears cache)
     */
    public List<Map<String, Object>> forceRefreshProducts() {
        log.info("Force refreshing products from Asbis API (clearing cache)");
        asbisProductsCache.invalidate("all_products");
        return getAllProducts();
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
                // Fix URL spacing issue
                imageUrl = imageUrl.trim()
                        .replace("https: //", "https://")
                        .replace("http: //", "http://");
                images.add(imageUrl);
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

    public void clearCache() {
        asbisProductsCache.invalidateAll();
        log.info("✓ Cleared Asbis API products cache");
    }

    public Map<String, Object> getCacheStatistics() {
        var stats = asbisProductsCache.stats();

        Map<String, Object> cacheStats = new HashMap<>();
        cacheStats.put("hitCount", stats.hitCount());
        cacheStats.put("missCount", stats.missCount());
        cacheStats.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
        cacheStats.put("evictionCount", stats.evictionCount());
        cacheStats.put("size", asbisProductsCache.estimatedSize());

        return cacheStats;
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

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", categories.size());
            stats.put("level1", level1Count);
            stats.put("level2", level2Count);
            stats.put("breakdown", Map.of(
                    "productCategories", level1Count,
                    "productTypes", level2Count
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