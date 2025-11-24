package com.techstore.service.sync;

import com.techstore.entity.*;
import com.techstore.enums.Platform;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.*;
import com.techstore.service.MostApiService;
import com.techstore.util.LogHelper;
import com.techstore.util.SyncHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.techstore.util.LogHelper.LOG_STATUS_FAILED;
import static com.techstore.util.LogHelper.LOG_STATUS_SUCCESS;

@Service
@RequiredArgsConstructor
@Slf4j
public class MostSyncService {

    private final MostApiService mostApiService;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;
    private final ParameterRepository parameterRepository;
    private final ParameterOptionRepository parameterOptionRepository;
    private final EntityManager entityManager;
    private final SyncHelper syncHelper;
    private final LogHelper logHelper;

    private static final String USD_TO_BGN_RATE = "1.80"; // Приблизителен курс USD -> BGN
    private static final String EUR_TO_BGN_RATE = "1.95583"; // Фиксиран курс EUR -> BGN

    @Transactional
    public void syncMostManufacturers() {
        String syncType = "MOST_MANUFACTURERS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Most manufacturers synchronization");

            Set<String> externalManufacturers = mostApiService.extractUniqueManufacturers();

            if (externalManufacturers.isEmpty()) {
                log.warn("No manufacturers found in Most API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No manufacturers found", startTime);
                return;
            }

            // Get existing manufacturers
            Map<String, Manufacturer> existingManufacturers = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (existing, duplicate) -> {
                                log.warn("Duplicate manufacturer: {}, IDs: {} and {}, keeping first",
                                        existing.getName(), existing.getId(), duplicate.getId());
                                return existing;
                            }
                    ));

            long created = 0, updated = 0;

            for (String manufacturerName : externalManufacturers) {
                try {
                    String normalizedName = normalizeManufacturerName(manufacturerName);
                    Manufacturer manufacturer = existingManufacturers.get(normalizedName);

                    if (manufacturer == null) {
                        manufacturer = createMostManufacturer(manufacturerName);
                        manufacturer = manufacturerRepository.save(manufacturer);
                        existingManufacturers.put(normalizedName, manufacturer);
                        created++;
                        log.debug("Created manufacturer: {}", manufacturerName);
                    } else {
                        updated++;
                        log.debug("Manufacturer already exists: {}", manufacturerName);
                    }

                } catch (Exception e) {
                    log.error("Error processing manufacturer {}: {}", manufacturerName, e.getMessage());
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    (long) externalManufacturers.size(), created, updated, 0, null, startTime);
            log.info("Most manufacturers sync completed - Created: {}, Updated: {}", created, updated);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Most manufacturers synchronization", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void syncMostCategories() {
        String syncType = "MOST_CATEGORIES";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Most categories synchronization");

            Set<String> externalCategories = mostApiService.extractUniqueCategories();

            if (externalCategories.isEmpty()) {
                log.warn("No categories found in Most API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No categories found", startTime);
                return;
            }

            // Get existing categories
            Map<String, Category> existingCategories = categoryRepository.findAll()
                    .stream()
                    .filter(c -> c.getNameBg() != null && !c.getNameBg().isEmpty())
                    .collect(Collectors.toMap(
                            c -> normalizeCategoryName(c.getNameBg()),
                            c -> c,
                            (existing, duplicate) -> existing
                    ));

            long created = 0, updated = 0, reused = 0;

            for (String categoryName : externalCategories) {
                try {
                    String normalizedName = normalizeCategoryName(categoryName);
                    Category category = existingCategories.get(normalizedName);

                    if (category == null) {
                        category = createMostCategory(categoryName);
                        category = categoryRepository.save(category);
                        existingCategories.put(normalizedName, category);
                        created++;
                        log.debug("Created category: {}", categoryName);
                    } else {
                        // Category exists - mark as reused
                        reused++;
                        log.debug("Reusing existing category: {}", categoryName);
                    }

                } catch (Exception e) {
                    log.error("Error processing category {}: {}", categoryName, e.getMessage());
                }
            }

            String message = reused > 0 ? String.format("Reused %d existing categories", reused) : null;
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    (long) externalCategories.size(), created, updated, 0, message, startTime);
            log.info("Most categories sync completed - Created: {}, Reused: {}", created, reused);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Most categories synchronization", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void syncMostParameters() {
        String syncType = "MOST_PARAMETERS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Most parameters synchronization");

            Map<String, Set<String>> parametersMap = mostApiService.extractUniqueParameters();

            if (parametersMap.isEmpty()) {
                log.warn("No parameters found in Most API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No parameters found", startTime);
                return;
            }

            long totalCreated = 0, totalUpdated = 0, totalOptionsCreated = 0;

            // Group parameters by category
            List<Map<String, Object>> allProducts = mostApiService.getAllProducts();
            Map<String, Map<String, Set<String>>> categorizedParameters = groupParametersByCategory(allProducts);

            for (Map.Entry<String, Map<String, Set<String>>> catEntry : categorizedParameters.entrySet()) {
                String categoryName = catEntry.getKey();
                Map<String, Set<String>> categoryParams = catEntry.getValue();

                try {
                    Optional<Category> categoryOpt = findCategoryByName(categoryName);

                    if (categoryOpt.isEmpty()) {
                        log.warn("Category not found: {}", categoryName);
                        continue;
                    }

                    Category category = categoryOpt.get();
                    log.info("Processing {} parameters for category: {}", categoryParams.size(), categoryName);

                    // Get existing parameters for this category
                    Map<String, Parameter> existingParameters = parameterRepository.findByCategoryId(category.getId())
                            .stream()
                            .filter(p -> p.getNameBg() != null)
                            .collect(Collectors.toMap(
                                    p -> normalizeParameterName(p.getNameBg()),
                                    p -> p,
                                    (existing, duplicate) -> existing
                            ));

                    for (Map.Entry<String, Set<String>> paramEntry : categoryParams.entrySet()) {
                        try {
                            String paramName = paramEntry.getKey();
                            Set<String> paramValues = paramEntry.getValue();

                            String normalizedName = normalizeParameterName(paramName);
                            Parameter parameter = existingParameters.get(normalizedName);

                            if (parameter == null) {
                                parameter = createMostParameter(paramName, category);
                                parameter = parameterRepository.save(parameter);
                                existingParameters.put(normalizedName, parameter);
                                totalCreated++;
                            } else {
                                totalUpdated++;
                            }

                            // Sync parameter options
                            int optionsCreated = syncParameterOptions(parameter, paramValues);
                            totalOptionsCreated += optionsCreated;

                        } catch (Exception e) {
                            log.error("Error processing parameter {}: {}", paramEntry.getKey(), e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    log.error("Error processing parameters for category {}: {}", categoryName, e.getMessage());
                }
            }

            String message = String.format("Parameters: %d created, %d updated. Options: %d created",
                    totalCreated, totalUpdated, totalOptionsCreated);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    totalCreated + totalUpdated, totalCreated, totalUpdated, 0, message, startTime);
            log.info("Most parameters sync completed - Parameters: {} created, {} updated. Options: {} created",
                    totalCreated, totalUpdated, totalOptionsCreated);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Most parameters synchronization", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void syncMostProducts() {
        String syncType = "MOST_PRODUCTS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Most products synchronization");

            List<Map<String, Object>> allProducts = mostApiService.getAllProducts();

            if (allProducts.isEmpty()) {
                log.warn("No products found in Most API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No products found", startTime);
                return;
            }

            // Load manufacturers map
            Map<String, Manufacturer> manufacturersMap = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null)
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            long totalCreated = 0, totalUpdated = 0, totalErrors = 0, skippedNoCategory = 0;

            for (int i = 0; i < allProducts.size(); i++) {
                Map<String, Object> rawProduct = allProducts.get(i);

                try {
                    String code = (String) rawProduct.get("code");
                    String name = (String) rawProduct.get("name");

                    if (code == null || code.isEmpty() || name == null || name.isEmpty()) {
                        log.debug("Skipping product with missing code or name");
                        totalErrors++;
                        continue;
                    }

                    // Find category
                    Category category = findProductCategory(rawProduct);
                    if (category == null) {
                        log.warn("No category found for product: {}", code);
                        skippedNoCategory++;
                        continue;
                    }

                    // Find or create product
                    Product product = findOrCreateProduct(code, name);
                    boolean isNew = (product.getId() == null);

                    // Update product fields
                    updateProductFromMost(product, rawProduct, category, manufacturersMap);

                    // Save product
                    product = productRepository.save(product);

                    if (isNew) {
                        totalCreated++;
                    } else {
                        totalUpdated++;
                    }

                    // Set parameters
                    setMostParametersToProduct(product, rawProduct);
                    productRepository.save(product);

                    if ((i + 1) % 50 == 0) {
                        log.info("Progress: {}/{} products processed", i + 1, allProducts.size());
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    totalErrors++;
                    log.error("Error processing product: {}", e.getMessage());
                }
            }

            String message = String.format("Created: %d, Updated: %d, Skipped (No Category): %d, Errors: %d",
                    totalCreated, totalUpdated, skippedNoCategory, totalErrors);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    totalCreated + totalUpdated, totalCreated, totalUpdated, totalErrors, message, startTime);
            log.info("Most products sync completed - {}", message);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Most products synchronization", e);
            throw new RuntimeException(e);
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private Manufacturer createMostManufacturer(String name) {
        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName(name);
        manufacturer.setInformationName(name);
        manufacturer.setPlatform(Platform.MOST);
        return manufacturer;
    }

    private Category createMostCategory(String name) {
        Category category = new Category();
        category.setNameBg(name);
        category.setNameEn(name);
        category.setPlatform(Platform.MOST);
        category.setShow(true);
        category.setSortOrder(0);

        // Generate unique slug
        String baseSlug = syncHelper.createSlugFromName(name);
        String uniqueSlug = generateUniqueSlug(baseSlug, category);
        category.setSlug(uniqueSlug);

        return category;
    }

    private Parameter createMostParameter(String name, Category category) {
        Parameter parameter = new Parameter();
        parameter.setNameBg(name);
        parameter.setNameEn(name);
        parameter.setCategory(category);
        parameter.setPlatform(Platform.MOST);
        parameter.setOrder(50);
        return parameter;
    }

    private int syncParameterOptions(Parameter parameter, Set<String> optionValues) {
        int created = 0;

        try {
            Map<String, ParameterOption> existingOptions = parameterOptionRepository
                    .findByParameterIdOrderByOrderAsc(parameter.getId())
                    .stream()
                    .filter(opt -> opt.getNameBg() != null)
                    .collect(Collectors.toMap(
                            opt -> normalizeParameterValue(opt.getNameBg()),
                            opt -> opt,
                            (existing, duplicate) -> existing
                    ));

            int orderCounter = existingOptions.size();

            for (String optionValue : optionValues) {
                if (optionValue == null || optionValue.trim().isEmpty() || "-".equals(optionValue.trim())) {
                    continue;
                }

                String normalizedValue = normalizeParameterValue(optionValue);
                ParameterOption option = existingOptions.get(normalizedValue);

                if (option == null) {
                    option = new ParameterOption();
                    option.setParameter(parameter);
                    option.setNameBg(optionValue);
                    option.setNameEn(optionValue);
                    option.setOrder(orderCounter++);

                    parameterOptionRepository.save(option);
                    created++;
                }
            }

        } catch (Exception e) {
            log.error("Error syncing parameter options for {}: {}", parameter.getNameBg(), e.getMessage());
        }

        return created;
    }

    private Map<String, Map<String, Set<String>>> groupParametersByCategory(List<Map<String, Object>> products) {
        Map<String, Map<String, Set<String>>> categorizedParams = new HashMap<>();

        for (Map<String, Object> product : products) {
            String categoryName = getCategoryNameFromProduct(product);
            if (categoryName == null) continue;

            @SuppressWarnings("unchecked")
            Map<String, String> properties = (Map<String, String>) product.get("properties");
            if (properties == null) continue;

            categorizedParams.putIfAbsent(categoryName, new HashMap<>());
            Map<String, Set<String>> categoryParams = categorizedParams.get(categoryName);

            for (Map.Entry<String, String> prop : properties.entrySet()) {
                String paramName = prop.getKey();
                String paramValue = prop.getValue();

                if (paramValue != null && !paramValue.isEmpty() && !"-".equals(paramValue)) {
                    categoryParams.putIfAbsent(paramName, new HashSet<>());
                    categoryParams.get(paramName).add(paramValue);
                }
            }
        }

        return categorizedParams;
    }

    private String getCategoryNameFromProduct(Map<String, Object> product) {
        String subcategory = (String) product.get("subcategory");
        if (subcategory != null && !subcategory.isEmpty()) {
            return subcategory;
        }
        return (String) product.get("category");
    }

    private Category findProductCategory(Map<String, Object> product) {
        // Try subcategory first (more specific)
        String subcategory = (String) product.get("subcategory");
        if (subcategory != null && !subcategory.isEmpty()) {
            Optional<Category> categoryOpt = findCategoryByName(subcategory);
            if (categoryOpt.isPresent()) {
                return categoryOpt.get();
            }
        }

        // Fallback to main category
        String category = (String) product.get("category");
        if (category != null && !category.isEmpty()) {
            Optional<Category> categoryOpt = findCategoryByName(category);
            if (categoryOpt.isPresent()) {
                return categoryOpt.get();
            }
        }

        return null;
    }

    private Optional<Category> findCategoryByName(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return Optional.empty();
        }

        String normalizedName = normalizeCategoryName(categoryName);

        return categoryRepository.findAll().stream()
                .filter(c -> c.getNameBg() != null)
                .filter(c -> normalizedName.equals(normalizeCategoryName(c.getNameBg())))
                .findFirst();
    }

    private Product findOrCreateProduct(String code, String name) {
        List<Product> existing = productRepository.findProductsBySkuCode(code);

        if (!existing.isEmpty()) {
            if (existing.size() > 1) {
                log.warn("Found {} duplicates for SKU: {}, keeping first", existing.size(), code);
                for (int i = 1; i < existing.size(); i++) {
                    productRepository.delete(existing.get(i));
                }
            }
            return existing.get(0);
        }

        Product product = new Product();
        product.setSku(code);
        product.setReferenceNumber(code);
        return product;
    }

    private void updateProductFromMost(Product product, Map<String, Object> rawProduct,
                                       Category category, Map<String, Manufacturer> manufacturersMap) {
        // Basic fields
        product.setCategory(category);
        product.setPlatform(Platform.MOST);

        String name = (String) rawProduct.get("name");
        product.setNameBg(name);
        product.setNameEn(name);

        // Status
        String productStatus = (String) rawProduct.get("product_status");
        boolean isAvailable = "Наличен".equals(productStatus);
        product.setStatus(isAvailable ? ProductStatus.AVAILABLE : ProductStatus.NOT_AVAILABLE);
        product.setShow(isAvailable);

        // Price
        String priceStr = (String) rawProduct.get("price");
        String currency = (String) rawProduct.get("currency");

        if (priceStr != null && !priceStr.isEmpty()) {
            try {
                BigDecimal price = new BigDecimal(priceStr);
                BigDecimal priceInBGN = convertPriceToBGN(price, currency);
                product.setPriceClient(priceInBGN);
            } catch (NumberFormatException e) {
                log.warn("Invalid price format: {}", priceStr);
            }
        }

        // Manufacturer
        String manufacturerName = (String) rawProduct.get("manufacturer");
        if (manufacturerName != null && !manufacturerName.isEmpty()) {
            String normalizedManufacturer = normalizeManufacturerName(manufacturerName);
            Manufacturer manufacturer = manufacturersMap.get(normalizedManufacturer);
            if (manufacturer != null) {
                product.setManufacturer(manufacturer);
            } else {
                log.warn("Manufacturer not found: {}", manufacturerName);
            }
        }

        // Images
        String mainImage = (String) rawProduct.get("main_picture_url");
        if (mainImage != null && !mainImage.isEmpty()) {
            product.setPrimaryImageUrl(mainImage);
        }

        @SuppressWarnings("unchecked")
        List<String> gallery = (List<String>) rawProduct.get("gallery");
        if (gallery != null && gallery.size() > 1) {
            List<String> additionalImages = gallery.subList(1, gallery.size());
            if (product.getAdditionalImages() != null) {
                product.getAdditionalImages().clear();
                product.getAdditionalImages().addAll(additionalImages);
            } else {
                product.setAdditionalImages(new ArrayList<>(additionalImages));
            }
        }

        product.calculateFinalPrice();
    }

    private void setMostParametersToProduct(Product product, Map<String, Object> rawProduct) {
        try {
            if (product.getCategory() == null) {
                log.warn("Product {} has no category, cannot set parameters", product.getSku());
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, String> properties = (Map<String, String>) rawProduct.get("properties");
            if (properties == null || properties.isEmpty()) {
                return;
            }

            Set<ProductParameter> productParameters = new HashSet<>();

            for (Map.Entry<String, String> prop : properties.entrySet()) {
                try {
                    String paramName = prop.getKey();
                    String paramValue = prop.getValue();

                    if (paramValue == null || paramValue.isEmpty() || "-".equals(paramValue)) {
                        continue;
                    }

                    // Find parameter
                    Optional<Parameter> parameterOpt = findParameterByName(paramName, product.getCategory());
                    if (parameterOpt.isEmpty()) {
                        continue;
                    }

                    Parameter parameter = parameterOpt.get();

                    // Find or create parameter option
                    ParameterOption option = findOrCreateParameterOption(parameter, paramValue);
                    if (option == null) {
                        continue;
                    }

                    ProductParameter productParam = new ProductParameter();
                    productParam.setProduct(product);
                    productParam.setParameter(parameter);
                    productParam.setParameterOption(option);
                    productParameters.add(productParam);

                } catch (Exception e) {
                    log.error("Error mapping parameter {} for product {}: {}",
                            prop.getKey(), product.getSku(), e.getMessage());
                }
            }

            product.setProductParameters(productParameters);

        } catch (Exception e) {
            log.error("Error setting Most parameters for product {}: {}", product.getSku(), e.getMessage());
        }
    }

    private Optional<Parameter> findParameterByName(String paramName, Category category) {
        String normalizedName = normalizeParameterName(paramName);

        return parameterRepository.findByCategoryId(category.getId())
                .stream()
                .filter(p -> p.getNameBg() != null)
                .filter(p -> normalizedName.equals(normalizeParameterName(p.getNameBg())))
                .findFirst();
    }

    private ParameterOption findOrCreateParameterOption(Parameter parameter, String value) {
        try {
            String normalizedValue = normalizeParameterValue(value);

            // Try to find existing
            List<ParameterOption> options = parameterOptionRepository
                    .findByParameterIdOrderByOrderAsc(parameter.getId());

            for (ParameterOption opt : options) {
                if (opt.getNameBg() != null &&
                        normalizedValue.equals(normalizeParameterValue(opt.getNameBg()))) {
                    return opt;
                }
            }

            // Create new
            ParameterOption newOption = new ParameterOption();
            newOption.setParameter(parameter);
            newOption.setNameBg(value);
            newOption.setNameEn(value);
            newOption.setOrder(options.size());

            return parameterOptionRepository.save(newOption);

        } catch (Exception e) {
            log.error("Error finding/creating parameter option: {}", e.getMessage());
            return null;
        }
    }

    private BigDecimal convertPriceToBGN(BigDecimal price, String currency) {
        if (price == null) {
            return BigDecimal.ZERO;
        }

        if ("BGN".equalsIgnoreCase(currency)) {
            return price;
        }

        if ("USD".equalsIgnoreCase(currency)) {
            return price.multiply(new BigDecimal(USD_TO_BGN_RATE));
        }

        if ("EUR".equalsIgnoreCase(currency)) {
            return price.multiply(new BigDecimal(EUR_TO_BGN_RATE));
        }

        // Default: assume BGN
        log.warn("Unknown currency: {}, assuming BGN", currency);
        return price;
    }

    private String generateUniqueSlug(String baseSlug, Category category) {
        if (!categoryRepository.existsBySlugAndIdNot(baseSlug, category.getId())) {
            return baseSlug;
        }

        int counter = 1;
        String numberedSlug;
        do {
            numberedSlug = baseSlug + "-" + counter;
            counter++;
        } while (categoryRepository.existsBySlugAndIdNot(numberedSlug, category.getId()));

        return numberedSlug;
    }

    private String normalizeManufacturerName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String normalizeCategoryName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String normalizeParameterName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String normalizeParameterValue(String value) {
        if (value == null) return "";
        return value.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}