package com.techstore.service.sync;

import com.techstore.entity.Category;
import com.techstore.entity.Manufacturer;
import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import com.techstore.entity.Product;
import com.techstore.entity.ProductParameter;
import com.techstore.entity.SyncLog;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ParameterOptionRepository;
import com.techstore.repository.ParameterRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.service.AsbisApiService;
import com.techstore.util.LogHelper;
import com.techstore.util.SyncHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.techstore.util.LogHelper.LOG_STATUS_FAILED;
import static com.techstore.util.LogHelper.LOG_STATUS_SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsbisSyncService {

    private final AsbisApiService asbisApiService;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;
    private final ParameterRepository parameterRepository;
    private final ParameterOptionRepository parameterOptionRepository;
    private final EntityManager entityManager;
    private final LogHelper logHelper;
    private final SyncHelper syncHelper;

    // ===========================================
    // CATEGORIES SYNC - CORRECT 2-LEVEL STRUCTURE
    // ===========================================

    @Transactional
    public void syncAsbisCategories() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_CATEGORIES");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== STARTING Asbis 2-level categories synchronization ===");
            log.info("Strategy: ProductCategory (Level 1) -> ProductType (Level 2)");

            List<Map<String, Object>> allProducts = asbisApiService.getAllProducts();

            if (allProducts.isEmpty()) {
                log.warn("No products from Asbis API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No products found", startTime);
                return;
            }

            log.info("Analyzing {} products to extract category hierarchy", allProducts.size());

            // ✅ STEP 1: Extract unique ProductCategory (Level 1) and build mapping
            Map<String, Set<String>> categoryToSubcategories = new HashMap<>();

            for (Map<String, Object> product : allProducts) {
                String mainCategory = getString(product, "productcategory");
                String subCategory = getString(product, "producttype");

                if (mainCategory != null && subCategory != null) {
                    categoryToSubcategories.putIfAbsent(mainCategory, new HashSet<>());
                    categoryToSubcategories.get(mainCategory).add(subCategory);
                }
            }

            log.info("Found {} main categories (ProductCategory)", categoryToSubcategories.size());
            int totalSubcategories = categoryToSubcategories.values().stream()
                    .mapToInt(Set::size).sum();
            log.info("Found {} total subcategories (ProductType)", totalSubcategories);

            long created = 0, updated = 0, skipped = 0;

            // ✅ STEP 2: Create/Update Level 1 categories (ProductCategory)
            log.info("=== STEP 1: Processing Level 1 categories (ProductCategory) ===");

            Map<String, Long> mainCategoryNameToId = new HashMap<>();

            for (String mainCategoryName : categoryToSubcategories.keySet()) {
                try {
                    if (mainCategoryName == null || mainCategoryName.trim().isEmpty()) {
                        skipped++;
                        continue;
                    }

                    // Find or create main category (no parent)
                    Category mainCategory = findOrCreateMainCategory(mainCategoryName);
                    mainCategory = categoryRepository.saveAndFlush(mainCategory);

                    mainCategoryNameToId.put(mainCategoryName, mainCategory.getId());

                    if (mainCategory.getCreatedAt().equals(mainCategory.getUpdatedAt())) {
                        created++;
                        log.info("✓ Created Level 1: '{}' (ID: {})", mainCategoryName, mainCategory.getId());
                    } else {
                        updated++;
                        log.info("✓ Updated Level 1: '{}' (ID: {})", mainCategoryName, mainCategory.getId());
                    }

                } catch (Exception e) {
                    log.error("✗ Error processing main category '{}': {}", mainCategoryName, e.getMessage(), e);
                    skipped++;
                }
            }

            entityManager.clear();
            log.info("=== Level 1 COMPLETE: {} main categories processed ===", mainCategoryNameToId.size());

            // ✅ STEP 3: Create/Update Level 2 categories (ProductType)
            log.info("=== STEP 2: Processing Level 2 categories (ProductType) ===");

            for (Map.Entry<String, Set<String>> entry : categoryToSubcategories.entrySet()) {
                String mainCategoryName = entry.getKey();
                Set<String> subCategories = entry.getValue();

                Long mainCategoryId = mainCategoryNameToId.get(mainCategoryName);
                if (mainCategoryId == null) {
                    log.error("✗ Main category '{}' not found, skipping its subcategories", mainCategoryName);
                    skipped += subCategories.size();
                    continue;
                }

                // Load parent category (managed entity)
                Category parentCategory = categoryRepository.findById(mainCategoryId).orElse(null);
                if (parentCategory == null) {
                    log.error("✗ Parent category with ID {} not found in DB", mainCategoryId);
                    skipped += subCategories.size();
                    continue;
                }

                log.info("Processing {} subcategories for parent '{}'", subCategories.size(), mainCategoryName);

                for (String subCategoryName : subCategories) {
                    try {
                        if (subCategoryName == null || subCategoryName.trim().isEmpty()) {
                            skipped++;
                            continue;
                        }

                        // Find or create subcategory with parent
                        Category subCategory = findOrCreateSubCategory(subCategoryName, parentCategory);
                        subCategory = categoryRepository.saveAndFlush(subCategory);

                        if (subCategory.getCreatedAt().equals(subCategory.getUpdatedAt())) {
                            created++;
                            log.info("✓ Created Level 2: '{}' (ID: {}, parent: '{}')",
                                    subCategoryName, subCategory.getId(), mainCategoryName);
                        } else {
                            updated++;
                            log.info("✓ Updated Level 2: '{}' (ID: {}, parent: '{}')",
                                    subCategoryName, subCategory.getId(), mainCategoryName);
                        }

                    } catch (Exception e) {
                        log.error("✗ Error processing subcategory '{}' under '{}': {}",
                                subCategoryName, mainCategoryName, e.getMessage(), e);
                        skipped++;
                    }
                }
            }

            entityManager.clear();
            log.info("=== Level 2 COMPLETE ===");

            long totalCategories = mainCategoryNameToId.size() + totalSubcategories;

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalCategories,
                    created, updated, skipped,
                    skipped > 0 ? String.format("Skipped %d categories", skipped) : null, startTime);

            log.info("=== SYNC COMPLETE ===");
            log.info("Total: {}, Created: {}, Updated: {}, Skipped: {}",
                    totalCategories, created, updated, skipped);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("=== SYNC FAILED ===", e);
            throw e;
        }
    }

    /**
     * Find or create MAIN category (Level 1 - ProductCategory)
     * No parent, asbisId format: "main:CategoryName"
     */
    private Category findOrCreateMainCategory(String categoryName) {
        String asbisId = "main:" + categoryName;

        // Check by asbisId
        List<Category> byAsbisId = categoryRepository.findAll()
                .stream()
                .filter(cat -> asbisId.equals(cat.getAsbisId()))
                .collect(Collectors.toList());

        if (!byAsbisId.isEmpty()) {
            Category category = byAsbisId.get(0);
            category.setParent(null); // Ensure it's root
            log.debug("Found existing main category by asbisId: {}", categoryName);
            return category;
        }

        // Check by normalized name (no parent)
        String normalizedName = normalizeCategoryName(categoryName);

        for (Category cat : categoryRepository.findAll()) {
            if (cat.getParent() == null) { // Only root categories
                String catNormalized = normalizeCategoryName(cat.getNameBg());
                if (normalizedName.equals(catNormalized)) {
                    // Reuse existing root category
                    cat.setAsbisId(asbisId);
                    cat.setAsbisCode(asbisId);
                    log.info("✓ REUSING existing root category '{}' as Asbis main category", categoryName);
                    return cat;
                }
            }
        }

        // Create new main category
        log.info("✓ CREATING new main category: '{}'", categoryName);

        Category category = new Category();
        category.setNameBg(categoryName);
        category.setNameEn(categoryName);
        category.setAsbisId(asbisId);
        category.setAsbisCode(asbisId);
        category.setParent(null);
        category.setShow(true);
        category.setSortOrder(0);

        String slug = generateSlug(categoryName, null);
        category.setSlug(slug);

        return category;
    }

    /**
     * Find or create SUBCATEGORY (Level 2 - ProductType)
     * Has parent, asbisId format: "sub:ParentName:SubcategoryName"
     */
    private Category findOrCreateSubCategory(String categoryName, Category parentCategory) {
        String asbisId = "sub:" + parentCategory.getNameBg() + ":" + categoryName;

        // Check by asbisId
        List<Category> byAsbisId = categoryRepository.findAll()
                .stream()
                .filter(cat -> asbisId.equals(cat.getAsbisId()))
                .collect(Collectors.toList());

        if (!byAsbisId.isEmpty()) {
            Category category = byAsbisId.get(0);
            category.setParent(parentCategory); // Ensure correct parent
            log.debug("Found existing subcategory by asbisId: {}", categoryName);
            return category;
        }

        // Check by normalized name + parent match
        String normalizedName = normalizeCategoryName(categoryName);

        for (Category cat : categoryRepository.findAll()) {
            String catNormalized = normalizeCategoryName(cat.getNameBg());

            if (normalizedName.equals(catNormalized)) {
                // Check parent match
                if (cat.getParent() != null &&
                        cat.getParent().getId().equals(parentCategory.getId())) {
                    // Reuse existing subcategory
                    cat.setAsbisId(asbisId);
                    cat.setAsbisCode(asbisId);
                    cat.setParent(parentCategory);
                    log.info("✓ REUSING existing subcategory '{}' under parent '{}'",
                            categoryName, parentCategory.getNameBg());
                    return cat;
                }
            }
        }

        // Create new subcategory
        log.info("✓ CREATING new subcategory: '{}' under parent '{}'",
                categoryName, parentCategory.getNameBg());

        Category category = new Category();
        category.setNameBg(categoryName);
        category.setNameEn(categoryName);
        category.setAsbisId(asbisId);
        category.setAsbisCode(asbisId);
        category.setParent(parentCategory);
        category.setShow(true);
        category.setSortOrder(0);

        String slug = generateSlug(categoryName, parentCategory);
        category.setSlug(slug);

        return category;
    }

    /**
     * Generate unique slug with parent hierarchy
     */
    private String generateSlug(String categoryName, Category parentCategory) {
        String baseSlug = syncHelper.createSlugFromName(categoryName);

        if (parentCategory == null) {
            // Root category
            if (!slugExists(baseSlug, null)) {
                return baseSlug;
            }
            return baseSlug + "-asbis";
        }

        // Child category - hierarchical slug
        String parentSlug = parentCategory.getSlug();
        if (parentSlug == null) {
            parentSlug = syncHelper.createSlugFromName(parentCategory.getNameBg());
        }

        String hierarchicalSlug = parentSlug + "-" + baseSlug;

        if (!slugExists(hierarchicalSlug, parentCategory)) {
            return hierarchicalSlug;
        }

        // Add counter if exists
        int counter = 2;
        String numberedSlug;
        do {
            numberedSlug = hierarchicalSlug + "-" + counter;
            counter++;
        } while (slugExists(numberedSlug, parentCategory) && counter < 100);

        return numberedSlug;
    }

    /**
     * Check if slug exists with specific parent
     */
    private boolean slugExists(String slug, Category expectedParent) {
        List<Category> existing = categoryRepository.findAll()
                .stream()
                .filter(cat -> slug.equals(cat.getSlug()))
                .collect(Collectors.toList());

        if (existing.isEmpty()) {
            return false;
        }

        for (Category cat : existing) {
            if (expectedParent == null && cat.getParent() == null) {
                return true;
            }
            if (expectedParent != null && cat.getParent() != null) {
                if (expectedParent.getId().equals(cat.getParent().getId())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Normalize category name for comparison
     */
    private String normalizeCategoryName(String name) {
        if (name == null) return "";

        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-zа-я0-9\\s]+", "")
                .replaceAll("\\s+", " ")
                .replaceAll("\\b(категория|category|продукти|products)\\b", "")
                .trim();
    }

    // ===========================================
    // MANUFACTURERS SYNC
    // ===========================================

    @Transactional
    public void syncAsbisManufacturers() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_MANUFACTURERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Asbis manufacturers synchronization");

            Set<String> asbisManufacturers = asbisApiService.extractManufacturers();

            if (asbisManufacturers.isEmpty()) {
                log.warn("No manufacturers extracted");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No manufacturers found", startTime);
                return;
            }

            Map<String, Manufacturer> existingManufacturers = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (a, b) -> a
                    ));

            long created = 0, updated = 0, errors = 0;

            for (String manufacturerName : asbisManufacturers) {
                try {
                    String normalizedName = normalizeManufacturerName(manufacturerName);
                    Manufacturer manufacturer = existingManufacturers.get(normalizedName);

                    if (manufacturer == null) {
                        manufacturer = new Manufacturer();
                        manufacturer.setName(manufacturerName);
                        manufacturer.setAsbisCode(manufacturerName);
                        manufacturer = manufacturerRepository.save(manufacturer);
                        existingManufacturers.put(normalizedName, manufacturer);
                        created++;
                    } else {
                        if (manufacturer.getAsbisCode() == null) {
                            manufacturer.setAsbisCode(manufacturerName);
                            manufacturerRepository.save(manufacturer);
                        }
                        updated++;
                    }

                } catch (Exception e) {
                    errors++;
                    log.error("Error processing manufacturer {}: {}", manufacturerName, e.getMessage());
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, (long) asbisManufacturers.size(),
                    created, updated, errors,
                    errors > 0 ? String.format("%d errors", errors) : null, startTime);

            log.info("Manufacturers sync complete: Created={}, Updated={}", created, updated);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Manufacturers sync failed", e);
            throw e;
        }
    }

    private String normalizeManufacturerName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    // ===========================================
    // PARAMETERS SYNC
    // ===========================================

    @Transactional
    public void syncAsbisParameters() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_PARAMETERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Asbis parameters synchronization");

            Map<String, Set<String>> asbisParameters = asbisApiService.extractParameters();

            if (asbisParameters.isEmpty()) {
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No parameters found", startTime);
                return;
            }

            Category generalCategory = findOrCreateGeneralAsbisCategory();

            long totalProcessed = 0, totalCreated = 0, totalUpdated = 0, totalErrors = 0;
            long optionsCreated = 0, optionsUpdated = 0;

            Map<String, Parameter> existingParameters = parameterRepository
                    .findByCategoryId(generalCategory.getId())
                    .stream()
                    .collect(Collectors.toMap(
                            p -> p.getAsbisKey() != null ? p.getAsbisKey() : p.getNameBg(),
                            p -> p,
                            (a, b) -> a
                    ));

            for (Map.Entry<String, Set<String>> entry : asbisParameters.entrySet()) {
                try {
                    String paramKey = entry.getKey();
                    Set<String> values = entry.getValue();

                    Parameter parameter = existingParameters.get(paramKey);
                    boolean isNew = false;

                    if (parameter == null) {
                        parameter = new Parameter();
                        parameter.setCategory(generalCategory);
                        parameter.setAsbisKey(paramKey);
                        parameter.setNameBg(paramKey);
                        parameter.setNameEn(paramKey);
                        parameter.setOrder(existingParameters.size());
                        isNew = true;
                    }

                    parameter = parameterRepository.save(parameter);

                    if (isNew) {
                        totalCreated++;
                        existingParameters.put(paramKey, parameter);
                    } else {
                        totalUpdated++;
                    }

                    // Sync options
                    Map<String, ParameterOption> existingOptions = parameterOptionRepository
                            .findByParameterIdOrderByOrderAsc(parameter.getId())
                            .stream()
                            .filter(opt -> opt.getNameBg() != null)
                            .collect(Collectors.toMap(
                                    ParameterOption::getNameBg,
                                    o -> o,
                                    (a, b) -> a
                            ));

                    int order = existingOptions.size();
                    for (String value : values) {
                        try {
                            if (value == null || value.trim().isEmpty()) continue;

                            // Clean and limit length
                            String cleanedValue = cleanValue(value);

                            if (cleanedValue.length() > 250) {
                                cleanedValue = cleanedValue.substring(0, 247) + "...";
                            }

                            ParameterOption option = existingOptions.get(cleanedValue);

                            if (option == null) {
                                option = new ParameterOption();
                                option.setParameter(parameter);
                                option.setNameBg(cleanedValue);
                                option.setNameEn(cleanedValue);
                                option.setOrder(order++);
                                parameterOptionRepository.save(option);
                                optionsCreated++;
                            } else {
                                optionsUpdated++;
                            }
                        } catch (Exception e) {
                            totalErrors++;
                            log.error("Error processing option '{}' for parameter '{}': {}",
                                    value, paramKey, e.getMessage());
                        }
                    }

                    totalProcessed++;

                } catch (Exception e) {
                    totalErrors++;
                    log.error("Error processing parameter {}: {}", entry.getKey(), e.getMessage());
                }
            }

            String message = String.format("Params: %d created, %d updated. Options: %d created, %d updated",
                    totalCreated, totalUpdated, optionsCreated, optionsUpdated);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed,
                    totalCreated, totalUpdated, totalErrors, message, startTime);

            log.info("Parameters sync complete: {}", message);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Parameters sync failed", e);
            throw e;
        }
    }

    private Category findOrCreateGeneralAsbisCategory() {
        Optional<Category> existing = categoryRepository.findAll()
                .stream()
                .filter(cat -> "Asbis Products".equals(cat.getNameEn()))
                .findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        Category category = new Category();
        category.setNameBg("Продукти Asbis");
        category.setNameEn("Asbis Products");
        category.setSlug("asbis-products");
        category.setAsbisCode("GENERAL");
        category.setShow(true);
        category.setSortOrder(999);

        return categoryRepository.save(category);
    }

    // ===========================================
    // PRODUCTS SYNC - UPDATED FOR 2-LEVEL STRUCTURE
    // ===========================================

    @Transactional
    public void syncAsbisProducts() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_PRODUCTS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== Starting Asbis products synchronization ===");

            List<Map<String, Object>> allProducts = asbisApiService.getAllProducts();

            if (allProducts.isEmpty()) {
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No products found", startTime);
                return;
            }

            // Build category mapping: ProductCategory + ProductType -> Category
            Map<String, Category> categoryMap = buildCategoryMap();
            Map<String, Manufacturer> manufacturersMap = prepareManufacturersMap();

            long totalProcessed = 0, created = 0, updated = 0, errors = 0, skipped = 0;

            for (int i = 0; i < allProducts.size(); i++) {
                Map<String, Object> rawProduct = allProducts.get(i);

                try {
                    String productCode = getString(rawProduct, "productcode");

                    if (productCode == null) {
                        errors++;
                        continue;
                    }

                    Product product = findOrCreateProduct(productCode, rawProduct,
                            categoryMap, manufacturersMap);

                    if (product == null) {
                        skipped++;
                        continue;
                    }

                    boolean isNew = (product.getId() == null);
                    product = productRepository.save(product);

                    if (isNew) {
                        created++;
                    } else {
                        updated++;
                    }

                    if (product.getCategory() != null) {
                        mapProductParameters(product, rawProduct);
                        productRepository.save(product);
                    }

                    totalProcessed++;

                    if (totalProcessed % 20 == 0) {
                        log.info("Progress: {}/{}", totalProcessed, allProducts.size());
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    errors++;
                    log.error("Error processing product: {}", e.getMessage());
                }
            }

            String message = String.format("Total: %d, Created: %d, Updated: %d, Skipped: %d, Errors: %d",
                    totalProcessed, created, updated, skipped, errors);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed,
                    created, updated, errors, message, startTime);

            log.info("Products sync complete: {}", message);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Products sync failed", e);
            throw e;
        }
    }

    /**
     * Build category map: "ProductCategory|ProductType" -> Category entity
     */
    private Map<String, Category> buildCategoryMap() {
        Map<String, Category> map = new HashMap<>();

        List<Category> allCategories = categoryRepository.findAll();

        for (Category cat : allCategories) {
            if (cat.getAsbisId() != null) {
                // Format: "main:CategoryName" or "sub:ParentName:SubcategoryName"
                if (cat.getAsbisId().startsWith("main:")) {
                    String mainName = cat.getAsbisId().substring(5);
                    map.put(mainName, cat);
                } else if (cat.getAsbisId().startsWith("sub:")) {
                    String[] parts = cat.getAsbisId().substring(4).split(":", 2);
                    if (parts.length == 2) {
                        String parentName = parts[0];
                        String subName = parts[1];
                        String key = parentName + "|" + subName;
                        map.put(key, cat);
                    }
                }
            }
        }

        log.info("Built category map with {} entries", map.size());
        return map;
    }

    private Product findOrCreateProduct(String productCode, Map<String, Object> rawProduct,
                                        Map<String, Category> categoryMap,
                                        Map<String, Manufacturer> manufacturersMap) {

        // Find existing
        Optional<Product> existing = productRepository.findAll()
                .stream()
                .filter(p -> productCode.equals(p.getAsbisCode()))
                .findFirst();

        Product product = existing.orElse(new Product());

        // Update fields
        product.setAsbisCode(productCode);
        product.setAsbisId(productCode);

        if (product.getSku() == null || product.getSku().isEmpty()) {
            product.setSku(productCode);
        }
        if (product.getReferenceNumber() == null) {
            product.setReferenceNumber(productCode);
        }

        String description = getString(rawProduct, "productdescription");
        if (description != null) {
            product.setNameBg(description);
            product.setNameEn(description);
        }

        // ✅ Find category using ProductCategory + ProductType
        String productCategory = getString(rawProduct, "productcategory");
        String productType = getString(rawProduct, "producttype");

        Category category = findCategoryForProduct(productCategory, productType, categoryMap);
        if (category != null) {
            product.setCategory(category);
            log.debug("Product '{}' mapped to category '{}'", productCode, category.getNameBg());
        } else {
            log.warn("Category not found for product '{}' (category='{}', type='{}')",
                    productCode, productCategory, productType);
        }

        // Find manufacturer
        String vendor = getString(rawProduct, "vendor");
        if (vendor != null) {
            Manufacturer manufacturer = manufacturersMap.get(normalizeManufacturerName(vendor));
            if (manufacturer != null) {
                product.setManufacturer(manufacturer);
            }
        }

        // Images
        Object imagesObj = rawProduct.get("images");
        if (imagesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> images = (List<String>) imagesObj;
            if (!images.isEmpty()) {
                product.setPrimaryImageUrl(images.get(0));
                if (images.size() > 1) {
                    if (product.getAdditionalImages() != null) {
                        product.getAdditionalImages().clear();
                        product.getAdditionalImages().addAll(images.subList(1, images.size()));
                    } else {
                        product.setAdditionalImages(new ArrayList<>(images.subList(1, images.size())));
                    }
                }
            }
        }

        product.setShow(true);
        product.setStatus(ProductStatus.AVAILABLE);
        product.calculateFinalPrice();

        return product;
    }

    /**
     * Find category for product using ProductCategory and ProductType
     * Priority: ProductType (Level 2) > ProductCategory (Level 1)
     */
    private Category findCategoryForProduct(String productCategory, String productType,
                                            Map<String, Category> categoryMap) {
        if (productCategory == null) {
            return null;
        }

        // ✅ Try to find subcategory (ProductType under ProductCategory)
        if (productType != null) {
            String key = productCategory + "|" + productType;
            Category subCategory = categoryMap.get(key);
            if (subCategory != null) {
                return subCategory;
            }
        }

        // ✅ Fallback to main category (ProductCategory)
        Category mainCategory = categoryMap.get(productCategory);
        if (mainCategory != null) {
            return mainCategory;
        }

        return null;
    }

    private void mapProductParameters(Product product, Map<String, Object> rawProduct) {
        try {
            Object attrListObj = rawProduct.get("attrlist");
            if (!(attrListObj instanceof Map)) return;

            @SuppressWarnings("unchecked")
            Map<String, String> attrList = (Map<String, String>) attrListObj;

            if (attrList.isEmpty()) return;

            Category generalCategory = findOrCreateGeneralAsbisCategory();
            Set<ProductParameter> productParameters = new HashSet<>();

            int mappedCount = 0;
            int skippedCount = 0;

            for (Map.Entry<String, String> attr : attrList.entrySet()) {
                String paramName = attr.getKey();
                String paramValue = attr.getValue();

                if (paramValue == null || paramValue.trim().isEmpty()) {
                    skippedCount++;
                    continue;
                }

                paramValue = cleanValue(paramValue);

                Parameter parameter = findParameter(paramName, generalCategory);
                if (parameter == null) {
                    skippedCount++;
                    continue;
                }

                ParameterOption option = findOrCreateOption(parameter, paramValue);
                if (option == null) {
                    skippedCount++;
                    continue;
                }

                ProductParameter pp = new ProductParameter();
                pp.setProduct(product);
                pp.setParameter(parameter);
                pp.setParameterOption(option);
                productParameters.add(pp);

                mappedCount++;
            }

            product.setProductParameters(productParameters);

            if (mappedCount > 0) {
                log.debug("Mapped {} parameters for product {} (skipped: {})",
                        mappedCount, product.getSku(), skippedCount);
            }

        } catch (Exception e) {
            log.error("Error mapping parameters for product {}: {}",
                    product.getSku(), e.getMessage());
            product.setProductParameters(new HashSet<>());
        }
    }

    private Parameter findParameter(String name, Category category) {
        return parameterRepository.findAll()
                .stream()
                .filter(p -> name.equals(p.getAsbisKey()) &&
                        category.getId().equals(p.getCategory().getId()))
                .findFirst()
                .orElse(null);
    }

    private ParameterOption findOrCreateOption(Parameter parameter, String value) {
        try {
            if (value == null || value.isEmpty()) {
                return null;
            }

            if (value.length() > 250) {
                value = value.substring(0, 247) + "...";
            }

            // ✅ CRITICAL FIX: Зареди всички options за този параметър и филтрирай
            // Това избягва проблема с query-то findByParameterAndNameBg което връща multiple results
            List<ParameterOption> allOptions = parameterOptionRepository
                    .findByParameterIdOrderByOrderAsc(parameter.getId());

            // Търси exact match по име
            String finalValue1 = value;
            Optional<ParameterOption> existingOption = allOptions.stream()
                    .filter(opt -> finalValue1.equals(opt.getNameBg()))
                    .findFirst();

            if (existingOption.isPresent()) {
                return existingOption.get();
            }

            // ✅ SECOND CHECK: Преди да създадем, flush-ни и провери отново в базата
            // Това предотвратява race conditions
            entityManager.flush();

            List<ParameterOption> recheck = parameterOptionRepository
                    .findByParameterIdOrderByOrderAsc(parameter.getId());

            String finalValue = value;
            Optional<ParameterOption> recheckMatch = recheck.stream()
                    .filter(opt -> finalValue.equals(opt.getNameBg()))
                    .findFirst();

            if (recheckMatch.isPresent()) {
                return recheckMatch.get();
            }

            // Създай нова option
            ParameterOption option = new ParameterOption();
            option.setParameter(parameter);
            option.setNameBg(value);
            option.setNameEn(value);
            option.setOrder(recheck.size());

            option = parameterOptionRepository.save(option);
            entityManager.flush(); // ✅ Flush веднага след създаване

            return option;

        } catch (Exception e) {
            log.error("Error creating option for parameter '{}' with value '{}': {}",
                    parameter.getNameBg(), value, e.getMessage());
            return null;
        }
    }

    private String cleanValue(String value) {
        if (value == null) return null;

        String cleaned = value.replace("&lt;br/&gt;", ", ")
                .replace("<br/>", ", ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.length() > 250) {
            cleaned = cleaned.substring(0, 247) + "...";
        }

        return cleaned;
    }

    private Map<String, Manufacturer> prepareManufacturersMap() {
        return manufacturerRepository.findAll()
                .stream()
                .filter(m -> m.getName() != null)
                .collect(Collectors.toMap(
                        m -> normalizeManufacturerName(m.getName()),
                        m -> m,
                        (a, b) -> a
                ));
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }
}