package com.techstore.service.sync;

import com.techstore.entity.*;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.*;
import com.techstore.service.AsbisApiService;
import com.techstore.util.LogHelper;
import com.techstore.util.SyncHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.techstore.util.LogHelper.LOG_STATUS_FAILED;
import static com.techstore.util.LogHelper.LOG_STATUS_SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsbisSyncService {

    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;
    private final ParameterRepository parameterRepository;
    private final ParameterOptionRepository parameterOptionRepository;
    private final EntityManager entityManager;
    private final AsbisApiService asbisApiService;
    private final LogHelper logHelper;
    private final SyncHelper syncHelper;

    @Value("${app.sync.batch-size:50}")
    private int batchSize;

    // ===========================================
    // CATEGORIES SYNC
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
            log.info("=== STARTING Asbis manufacturers synchronization ===");

            // Measure API call time
            long apiStart = System.currentTimeMillis();
            Set<String> externalManufacturers = asbisApiService.extractManufacturers();
            long apiDuration = System.currentTimeMillis() - apiStart;

            log.info("✓ Asbis API call completed in {}ms, returned {} manufacturers",
                    apiDuration, externalManufacturers.size());

            if (externalManufacturers.isEmpty()) {
                log.warn("No manufacturers returned from Asbis API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No manufacturers found", startTime);
                return;
            }

            // Load existing manufacturers by name
            Map<String, Manufacturer> existingManufacturers = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            Manufacturer::getName,
                            m -> m,
                            (existing, duplicate) -> {
                                log.warn("Duplicate manufacturer name: {}, keeping first (IDs: {} and {})",
                                        existing.getName(), existing.getId(), duplicate.getId());
                                return existing;
                            }
                    ));

            log.info("Found {} existing manufacturers in database", existingManufacturers.size());

            long created = 0, updated = 0, errors = 0;
            int processed = 0;

            for (String manufacturerName : externalManufacturers) {
                try {
                    if (manufacturerName == null || manufacturerName.trim().isEmpty()) {
                        log.warn("Skipping manufacturer with empty name");
                        errors++;
                        continue;
                    }

                    Manufacturer manufacturer = existingManufacturers.get(manufacturerName);

                    if (manufacturer == null) {
                        manufacturer = createAsbisManufacturer(manufacturerName);
                        manufacturer = manufacturerRepository.save(manufacturer);
                        existingManufacturers.put(manufacturerName, manufacturer);
                        created++;
                        log.debug("✓ Created manufacturer: {}", manufacturerName);
                    } else {
                        updated++;
                    }

                    processed++;

                    if (processed % 100 == 0) {
                        log.info("Progress: {}/{} manufacturers processed (created: {}, updated: {})",
                                processed, externalManufacturers.size(), created, updated);
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    errors++;
                    log.error("Error processing manufacturer '{}': {}", manufacturerName, e.getMessage());
                }
            }

            entityManager.flush();
            entityManager.clear();

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    (long) externalManufacturers.size(), created, updated, errors,
                    errors > 0 ? String.format("Completed with %d errors", errors) : null, startTime);

            log.info("=== COMPLETE: Asbis manufacturers sync finished in {}ms ===",
                    System.currentTimeMillis() - startTime);
            log.info("Total: {}, Created: {}, Updated: {}, Errors: {}",
                    processed, created, updated, errors);

        } catch (Exception e) {
            log.error("=== FAILED: Asbis manufacturers synchronization ===", e);
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0,
                    e.getMessage(), startTime);
            throw e;
        }
    }

    // ===========================================
    // PARAMETERS SYNC
    // ===========================================

    @Transactional
    public void syncAsbisParameters() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_PARAMETERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== STARTING Asbis parameters synchronization ===");

            // Measure API call time
            long apiStart = System.currentTimeMillis();
            Map<String, Set<String>> parametersMap = asbisApiService.extractParameters();
            long apiDuration = System.currentTimeMillis() - apiStart;

            log.info("✓ Asbis API call completed in {}ms, returned {} parameter types",
                    apiDuration, parametersMap.size());

            if (parametersMap.isEmpty()) {
                log.warn("No parameters returned from Asbis API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No parameters found", startTime);
                return;
            }

            // Show sample parameters
            log.info("Sample parameters: {}",
                    parametersMap.keySet().stream().limit(10).collect(Collectors.joining(", ")));

            // Get all Asbis categories to associate parameters with
            List<Category> asbisCategories = categoryRepository.findAll().stream()
                    .filter(cat -> cat.getAsbisId() != null)
                    .toList();

            if (asbisCategories.isEmpty()) {
                log.warn("No Asbis categories found in database");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No Asbis categories found", startTime);
                return;
            }

            log.info("Found {} Asbis categories to associate parameters with", asbisCategories.size());

            // For Asbis, we'll create global parameters and associate them with root category
            // Root category = category without parent
            Category rootCategory = asbisCategories.stream()
                    .filter(cat -> cat.getParent() == null)
                    .findFirst()
                    .orElse(null);

            if (rootCategory == null) {
                log.warn("No root category found, using first available category");
                rootCategory = asbisCategories.get(0);
            }

            // Validate root category
            if (rootCategory.getId() == null) {
                log.error("Root category has null ID - cannot proceed");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0,
                        "Root category has null ID", startTime);
                return;
            }

            log.info("Using category '{}' (ID: {}, AsbisID: {}) for global Asbis parameters",
                    rootCategory.getNameBg(), rootCategory.getId(), rootCategory.getAsbisId());

            // Load existing parameters for this category
            Map<String, Parameter> existingParameters;
            try {
                existingParameters = parameterRepository
                        .findByCategoryId(rootCategory.getId())
                        .stream()
                        .collect(Collectors.toMap(
                                p -> p.getAsbisKey() != null ? p.getAsbisKey() : p.getNameBg(),
                                p -> p,
                                (existing, duplicate) -> {
                                    log.warn("Duplicate parameter key: {}, keeping first", existing.getAsbisKey());
                                    return existing;
                                }
                        ));
                log.info("Found {} existing parameters for root category", existingParameters.size());
            } catch (Exception e) {
                log.error("Error loading existing parameters: {}", e.getMessage(), e);
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0,
                        "Error loading existing parameters: " + e.getMessage(), startTime);
                return;
            }

            long totalProcessed = 0, totalCreated = 0, totalUpdated = 0, totalErrors = 0;
            long totalOptionsCreated = 0, totalOptionsUpdated = 0;

            int paramIndex = 0;
            for (Map.Entry<String, Set<String>> paramEntry : parametersMap.entrySet()) {
                try {
                    String parameterKey = paramEntry.getKey();
                    Set<String> parameterValues = paramEntry.getValue();

                    if (parameterKey == null || parameterKey.trim().isEmpty()) {
                        log.warn("Skipping parameter with null/empty key");
                        totalErrors++;
                        continue;
                    }

                    Parameter parameter = existingParameters.get(parameterKey);
                    boolean isNew = (parameter == null);

                    if (isNew) {
                        try {
                            parameter = createAsbisParameter(parameterKey, rootCategory);
                            if (parameter == null) {
                                log.error("createAsbisParameter returned null for key: {}", parameterKey);
                                totalErrors++;
                                continue;
                            }
                            totalCreated++;
                        } catch (Exception e) {
                            log.error("Error creating parameter for key '{}': {}", parameterKey, e.getMessage(), e);
                            totalErrors++;
                            continue;
                        }
                    } else {
                        totalUpdated++;
                    }

                    try {
                        parameter = parameterRepository.save(parameter);
                        if (parameter.getId() == null) {
                            log.error("Parameter save returned null ID for key: {}", parameterKey);
                            totalErrors++;
                            continue;
                        }
                        existingParameters.put(parameterKey, parameter);
                    } catch (Exception e) {
                        log.error("Error saving parameter '{}': {}", parameterKey, e.getMessage(), e);
                        totalErrors++;
                        continue;
                    }

                    // Sync parameter options
                    try {
                        ParameterOptionResult optionResult = syncAsbisParameterOptions(
                                parameter, parameterValues);
                        totalOptionsCreated += optionResult.created;
                        totalOptionsUpdated += optionResult.updated;
                    } catch (Exception e) {
                        log.error("Error syncing options for parameter '{}': {}", parameterKey, e.getMessage(), e);
                        // Continue - parameter is saved, just options failed
                    }

                    totalProcessed++;
                    paramIndex++;

                    if (paramIndex % 20 == 0) {
                        log.info("Progress: {}/{} parameters processed (created: {}, updated: {}, errors: {})",
                                paramIndex, parametersMap.size(), totalCreated, totalUpdated, totalErrors);
                        try {
                            entityManager.flush();
                            entityManager.clear();
                        } catch (Exception e) {
                            log.error("Error during flush/clear at param {}: {}", paramIndex, e.getMessage(), e);
                        }
                    }

                } catch (Exception e) {
                    totalErrors++;
                    log.error("Unexpected error processing parameter '{}': {}",
                            paramEntry.getKey(), e.getMessage(), e);
                }
            }

            entityManager.flush();
            entityManager.clear();

            String message = String.format(
                    "Parameters: %d created, %d updated. Options: %d created, %d updated",
                    totalCreated, totalUpdated, totalOptionsCreated, totalOptionsUpdated);
            if (totalErrors > 0) {
                message += String.format(". %d errors occurred", totalErrors);
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed,
                    totalCreated, totalUpdated, totalErrors, message, startTime);

            log.info("=== COMPLETE: Asbis parameters sync finished in {}ms ===",
                    System.currentTimeMillis() - startTime);
            log.info("Total: {} params, {} options", totalCreated + totalUpdated,
                    totalOptionsCreated + totalOptionsUpdated);

        } catch (Exception e) {
            log.error("=== FAILED: Asbis parameters synchronization ===", e);
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0,
                    e.getMessage(), startTime);
            throw e;
        }
    }

    // ===========================================
    // PRODUCTS SYNC - WITH BATCH PROCESSING
    // ===========================================

    @Transactional
    public void syncAsbisProducts() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_PRODUCTS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== STARTING Asbis products synchronization ===");

            // STEP 1: Fetch all products from API
            log.info("STEP 1: Fetching products from Asbis API...");
            long apiStart = System.currentTimeMillis();
            List<Map<String, Object>> allProducts = asbisApiService.getAllProducts();
            long apiDuration = System.currentTimeMillis() - apiStart;

            log.info("✓ Asbis API call completed in {}ms ({:.1f} min), returned {} products",
                    apiDuration, apiDuration / 60000.0, allProducts.size());

            if (allProducts.isEmpty()) {
                log.warn("No products returned from Asbis API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No products found", startTime);
                return;
            }

            // Show sample products
            log.info("Sample products:");
            allProducts.stream().limit(3).forEach(p ->
                    log.info("  - Code: {}, Vendor: {}, Type: {}, Name: {}",
                            getString(p, "productcode"),
                            getString(p, "vendor"),
                            getString(p, "producttype"),
                            getString(p, "productdescription"))
            );

            // STEP 2: Load lookup maps
            log.info("STEP 2: Loading categories and manufacturers...");
            long lookupStart = System.currentTimeMillis();

            // Categories by Asbis ID (which is the fullPath or generated ID)
            Map<String, Category> categoriesByAsbisId = categoryRepository.findAll().stream()
                    .filter(cat -> cat.getAsbisId() != null && !cat.getAsbisId().isEmpty())
                    .collect(Collectors.toMap(
                            Category::getAsbisId,
                            cat -> cat,
                            (existing, duplicate) -> existing
                    ));

            // Also create a map by fullPath for matching products
            Map<String, Category> categoriesByFullPath = new HashMap<>();
            for (Category cat : categoriesByAsbisId.values()) {
                String fullPath = cat.getCategoryPath();
                if (fullPath != null) {
                    categoriesByFullPath.put(fullPath, cat);
                }
            }

            Map<String, Manufacturer> manufacturersMap = manufacturerRepository.findAll().stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            Manufacturer::getName,
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            long lookupDuration = System.currentTimeMillis() - lookupStart;
            log.info("✓ Lookup maps loaded in {}ms: {} categories, {} manufacturers",
                    lookupDuration, categoriesByAsbisId.size(), manufacturersMap.size());

            // STEP 3: Process products in batches
            log.info("STEP 3: Processing {} products in batches of {}...",
                    allProducts.size(), batchSize);

            List<List<Map<String, Object>>> batches = partitionList(allProducts, batchSize);
            log.info("Created {} batches for processing", batches.size());

            long totalProcessed = 0, totalCreated = 0, totalUpdated = 0, totalErrors = 0;
            long totalSkippedNoCategory = 0;

            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<Map<String, Object>> batch = batches.get(batchIndex);
                long batchStart = System.currentTimeMillis();

                try {
                    log.info("=== Processing batch [{}/{}] with {} products ===",
                            batchIndex + 1, batches.size(), batch.size());

                    BatchResult result = processProductsBatch(batch, categoriesByAsbisId,
                            categoriesByFullPath, manufacturersMap);

                    totalProcessed += result.processed;
                    totalCreated += result.created;
                    totalUpdated += result.updated;
                    totalErrors += result.errors;
                    totalSkippedNoCategory += result.skippedNoCategory;

                    long batchDuration = System.currentTimeMillis() - batchStart;
                    double productsPerSecond = result.processed / (batchDuration / 1000.0);

                    log.info("✓ Batch [{}/{}] complete in {}ms ({:.1f} products/sec): " +
                                    "processed={}, created={}, updated={}, errors={}, skipped={}",
                            batchIndex + 1, batches.size(), batchDuration, productsPerSecond,
                            result.processed, result.created, result.updated,
                            result.errors, result.skippedNoCategory);

                    // Flush and clear after each batch
                    entityManager.flush();
                    entityManager.clear();

                    // Overall progress log
                    double progressPercent = ((double)(batchIndex + 1) / batches.size()) * 100;
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    long estimatedTotal = (long)(elapsedTime / progressPercent * 100);
                    long estimatedRemaining = estimatedTotal - elapsedTime;

                    log.info("=== OVERALL PROGRESS: {}/{} products ({:.1f}%) ===",
                            totalProcessed, allProducts.size(), progressPercent);
                    log.info("Elapsed: {:.1f}min, Estimated remaining: {:.1f}min, Total ETA: {:.1f}min",
                            elapsedTime / 60000.0, estimatedRemaining / 60000.0, estimatedTotal / 60000.0);
                    log.info("Current speed: {:.0f} products/min", (totalProcessed * 60000.0) / elapsedTime);

                } catch (Exception e) {
                    log.error("Error processing batch {}: {}", batchIndex + 1, e.getMessage(), e);
                    totalErrors += batch.size();
                }
            }

            String message = String.format(
                    "Total: %d, Created: %d, Updated: %d, Skipped (No Category): %d, Errors: %d",
                    totalProcessed, totalCreated, totalUpdated, totalSkippedNoCategory, totalErrors);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed,
                    totalCreated, totalUpdated, totalErrors, message, startTime);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("=== COMPLETE: Asbis products sync finished ===");
            log.info("Total time: {}ms ({:.1f} min)", totalDuration, totalDuration / 60000.0);
            log.info("Statistics: Created={}, Updated={}, Errors={}, Skipped={}",
                    totalCreated, totalUpdated, totalErrors, totalSkippedNoCategory);
            log.info("Average speed: {:.0f} products/min",
                    (totalProcessed * 60000.0) / totalDuration);

        } catch (Exception e) {
            log.error("=== FAILED: Asbis products synchronization ===", e);
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0,
                    e.getMessage(), startTime);
            throw e;
        }
    }

    // ===========================================
    // BATCH PROCESSING HELPER
    // ===========================================

    private BatchResult processProductsBatch(List<Map<String, Object>> batch,
                                             Map<String, Category> categoriesByAsbisId,
                                             Map<String, Category> categoriesByFullPath,
                                             Map<String, Manufacturer> manufacturersMap) {
        long processed = 0, created = 0, updated = 0, errors = 0, skippedNoCategory = 0;

        for (Map<String, Object> rawProduct : batch) {
            try {
                String productCode = getString(rawProduct, "productcode");
                String productDescription = getString(rawProduct, "productdescription");
                String productType = getString(rawProduct, "producttype"); // Full category path

                if (productCode == null || productDescription == null) {
                    log.debug("Skipping product with missing code or description");
                    errors++;
                    continue;
                }

                // Find category by productType (which is the full path like "Level1 - Level2 - Level3")
                Category category = findCategoryByProductType(productType, categoriesByAsbisId, categoriesByFullPath);

                if (category == null) {
                    log.warn("✗ Skipping product '{}' ({}): category not found for type: '{}'",
                            productDescription, productCode, productType);
                    skippedNoCategory++;
                    continue;
                }

                // Find or create product
                Product product = findOrCreateAsbisProduct(productCode, rawProduct, category);

                // Find manufacturer
                String vendorName = getString(rawProduct, "vendor");
                if (vendorName != null && !vendorName.isEmpty()) {
                    Manufacturer manufacturer = manufacturersMap.get(vendorName);
                    if (manufacturer != null) {
                        product.setManufacturer(manufacturer);
                    }
                }

                boolean isNew = (product.getId() == null);

                // Update product fields
                updateAsbisProductFields(product, rawProduct, category);

                // Set parameters from AttrList
                setParametersToAsbisProduct(product, rawProduct);

                // Save product
                product = productRepository.save(product);

                if (isNew) {
                    created++;
                    log.debug("✓ Created product: {} ({})", productDescription, productCode);
                } else {
                    updated++;
                    log.debug("✓ Updated product: {} ({})", productDescription, productCode);
                }

                processed++;

                // Periodic flush within batch
                if (processed % 20 == 0) {
                    entityManager.flush();
                }

            } catch (Exception e) {
                errors++;
                log.error("Error processing product {}: {}",
                        getString(rawProduct, "productcode"), e.getMessage());
            }
        }

        return new BatchResult(processed, created, updated, errors, skippedNoCategory);
    }

    // ===========================================
    // HELPER METHODS
    // ===========================================

    private Category findCategoryByProductType(String productType,
                                               Map<String, Category> categoriesByAsbisId,
                                               Map<String, Category> categoriesByFullPath) {
        if (productType == null || productType.isEmpty()) {
            return null;
        }

        // Try exact match by full path
        Category category = categoriesByFullPath.get(productType);
        if (category != null) {
            return category;
        }

        // Try to find by generated ID (Level3 key or Level2 key)
        String[] parts = productType.split(" - ");
        if (parts.length == 3) {
            String level3Key = parts[0].trim() + "|" + parts[1].trim() + "|" + parts[2].trim();
            category = categoriesByAsbisId.get(level3Key);
            if (category != null) {
                return category;
            }
        }

        if (parts.length >= 2) {
            String level2Key = parts[0].trim() + "|" + parts[1].trim();
            category = categoriesByAsbisId.get(level2Key);
            if (category != null) {
                return category;
            }
        }

        if (parts.length >= 1) {
            String level1Key = parts[0].trim();
            category = categoriesByAsbisId.get(level1Key);
            if (category != null) {
                return category;
            }
        }

        return null;
    }

    private Category createAsbisCategoryFromExternal(Map<String, Object> extCategory,
                                                     Map<String, Category> existingCategoriesById) {
        Category category = new Category();

        String asbisId = getString(extCategory, "id");
        String name = getString(extCategory, "name");
        String parentId = getString(extCategory, "parent");
        String fullPath = getString(extCategory, "fullPath");

        // Validate lengths (after migration: asbisId=500, names=255, categoryPath already 500 in entity)
        validateStringLength(asbisId, "Category Asbis ID", CATEGORY_ID_MAX_LENGTH);
        validateStringLength(name, "Category name", 255);

        category.setAsbisId(asbisId);
        category.setNameBg(name);
        category.setNameEn(name);
        category.setCategoryPath(fullPath);  // Already 500 chars in entity

        // Set parent if exists
        if (parentId != null && !parentId.isEmpty()) {
            Category parent = existingCategoriesById.get(parentId);
            if (parent != null) {
                category.setParent(parent);
            }
        }

        String slug = syncHelper.createSlugFromName(name);
        category.setSlug(generateUniqueSlugForAsbis(slug, category));

        return category;
    }

    private void updateAsbisCategoryFromExternal(Category category, Map<String, Object> extCategory,
                                                 Map<String, Category> existingCategoriesById) {
        String name = getString(extCategory, "name");
        String parentId = getString(extCategory, "parent");
        String fullPath = getString(extCategory, "fullPath");

        if (name != null) {
            validateStringLength(name, "Category name", 255);
            category.setNameBg(name);
            category.setNameEn(name);
        }

        if (fullPath != null) {
            category.setCategoryPath(fullPath);  // Already 500 chars in entity
        }

        // Update parent if changed
        if (parentId != null && !parentId.isEmpty()) {
            Category parent = existingCategoriesById.get(parentId);
            if (parent != null && !parent.equals(category.getParent())) {
                category.setParent(parent);
            }
        }
    }

    private Manufacturer createAsbisManufacturer(String name) {
        validateStringLength(name, "Manufacturer name", MANUFACTURER_NAME_MAX_LENGTH);

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName(name);
        manufacturer.setInformationName(name);
        return manufacturer;
    }

    private Parameter createAsbisParameter(String parameterKey, Category category) {
        if (parameterKey == null || parameterKey.trim().isEmpty()) {
            log.error("Cannot create parameter with null/empty key");
            return null;
        }

        if (category == null || category.getId() == null) {
            log.error("Cannot create parameter '{}' - category is null or has no ID", parameterKey);
            return null;
        }

        try {
            // Validate length - after migration this is 500 chars
            validateStringLength(parameterKey, "Parameter key", PARAM_KEY_MAX_LENGTH);

            Parameter parameter = new Parameter();
            parameter.setAsbisKey(parameterKey);
            parameter.setCategory(category);
            parameter.setNameBg(parameterKey);
            parameter.setNameEn(parameterKey);
            parameter.setOrder(50); // Default order

            log.debug("Created parameter object: key={}, categoryId={}", parameterKey, category.getId());
            return parameter;
        } catch (Exception e) {
            log.error("Error creating parameter object for key '{}': {}", parameterKey, e.getMessage(), e);
            return null;
        }
    }

    private ParameterOptionResult syncAsbisParameterOptions(Parameter parameter, Set<String> values) {
        long created = 0, updated = 0;

        if (parameter == null || parameter.getId() == null) {
            log.error("Cannot sync parameter options - parameter is null or has no ID");
            return new ParameterOptionResult(0, 0);
        }

        if (values == null || values.isEmpty()) {
            log.debug("No parameter values to sync for parameter: {}", parameter.getNameBg());
            return new ParameterOptionResult(0, 0);
        }

        try {
            Map<String, ParameterOption> existingOptions = parameterOptionRepository
                    .findByParameterIdOrderByOrderAsc(parameter.getId())
                    .stream()
                    .filter(opt -> opt.getNameBg() != null && !opt.getNameBg().isEmpty())
                    .collect(Collectors.toMap(
                            ParameterOption::getNameBg,
                            o -> o,
                            (existing, duplicate) -> {
                                log.warn("Duplicate parameter option for parameter '{}': {}",
                                        parameter.getNameBg(), existing.getNameBg());
                                return existing;
                            }
                    ));

            int orderCounter = existingOptions.size();

            for (String value : values) {
                try {
                    if (value == null || value.trim().isEmpty()) {
                        continue;
                    }

                    // Validate length - after migration this is 1000 chars (the critical fix!)
                    validateStringLength(value, "Parameter option value for '" + parameter.getNameBg() + "'",
                            PARAM_OPTION_MAX_LENGTH);

                    ParameterOption option = existingOptions.get(value);

                    if (option == null) {
                        option = new ParameterOption();
                        option.setParameter(parameter);
                        option.setNameBg(value);
                        option.setNameEn(value);
                        option.setOrder(orderCounter++);

                        try {
                            parameterOptionRepository.save(option);
                            created++;
                        } catch (Exception e) {
                            log.error("Error saving parameter option (length: {}) for parameter '{}': {}",
                                    value.length(), parameter.getNameBg(), e.getMessage());
                        }
                    } else {
                        updated++;
                    }
                } catch (Exception e) {
                    log.error("Error processing parameter option (length: {}): {}",
                            value != null ? value.length() : 0, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error syncing parameter options for parameter '{}': {}",
                    parameter.getNameBg(), e.getMessage(), e);
        }

        return new ParameterOptionResult(created, updated);
    }

    private Product findOrCreateAsbisProduct(String productCode, Map<String, Object> rawProduct, Category category) {
        List<Product> existing = productRepository.findProductsBySkuCode(productCode);
        Product product;

        if (!existing.isEmpty()) {
            product = existing.get(0);
            if (existing.size() > 1) {
                log.warn("Found {} duplicates for product code: {}, keeping first", existing.size(), productCode);
                for (int i = 1; i < existing.size(); i++) {
                    productRepository.delete(existing.get(i));
                }
            }
        } else {
            product = new Product();
            product.setSku(productCode);
        }

        product.setCategory(category);
        return product;
    }

    private void updateAsbisProductFields(Product product, Map<String, Object> rawData, Category category) {
        String productCode = getString(rawData, "productcode");
        String productDescription = getString(rawData, "productdescription");
        String productCategory = getString(rawData, "productcategory");

        // Validate lengths (note: product names are TEXT in entity, so no limit there)
        validateStringLength(productCode, "Product code", 255);
        validateStringLength(productCategory, "Product model", PRODUCT_MODEL_MAX_LENGTH);

        product.setReferenceNumber(productCode);
        product.setNameBg(productDescription);  // TEXT field - no length limit
        product.setNameEn(productDescription);  // TEXT field - no length limit

        if (productCategory != null) {
            product.setModel(productCategory);
        }

        // Set image
        String imageUrl = getString(rawData, "image");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            product.setPrimaryImageUrl(imageUrl);
        }

        // Set additional images
        Object imagesObj = rawData.get("images");
        if (imagesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> imagesList = (List<String>) imagesObj;
            if (!imagesList.isEmpty()) {
                if (product.getAdditionalImages() != null) {
                    product.getAdditionalImages().clear();
                    product.getAdditionalImages().addAll(imagesList);
                } else {
                    product.setAdditionalImages(new ArrayList<>(imagesList));
                }
            }
        }

        // For now, set default status (Asbis XML doesn't have stock/price info in this format)
        product.setShow(true);
        product.setStatus(ProductStatus.AVAILABLE);

        product.calculateFinalPrice();
    }

    private void setParametersToAsbisProduct(Product product, Map<String, Object> rawProduct) {
        if (product.getCategory() == null) {
            log.warn("Product {} has no category, cannot set parameters", product.getSku());
            return;
        }

        Set<ProductParameter> productParameters = new HashSet<>();

        Object attrListObj = rawProduct.get("attrlist");
        if (!(attrListObj instanceof Map)) {
            product.setProductParameters(productParameters);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> attrList = (Map<String, String>) attrListObj;

        int mappedCount = 0;
        int notFoundCount = 0;

        for (Map.Entry<String, String> attrEntry : attrList.entrySet()) {
            try {
                String parameterKey = attrEntry.getKey();
                String parameterValue = attrEntry.getValue();

                if (parameterValue == null || parameterValue.trim().isEmpty()) {
                    continue;
                }

                // Find parameter by Asbis key
                Optional<Parameter> parameterOpt = parameterRepository
                        .findByCategoryId(product.getCategory().getId())
                        .stream()
                        .filter(p -> parameterKey.equals(p.getAsbisKey()))
                        .findFirst();

                if (parameterOpt.isEmpty()) {
                    notFoundCount++;
                    continue;
                }

                Parameter parameter = parameterOpt.get();

                // Find or create parameter option
                ParameterOption option = findOrCreateAsbisParameterOption(parameter, parameterValue);
                if (option == null) {
                    notFoundCount++;
                    continue;
                }

                ProductParameter productParam = new ProductParameter();
                productParam.setProduct(product);
                productParam.setParameter(parameter);
                productParam.setParameterOption(option);
                productParameters.add(productParam);

                mappedCount++;

            } catch (Exception e) {
                log.error("Error mapping parameter {} for product {}: {}",
                        attrEntry.getKey(), product.getSku(), e.getMessage());
                notFoundCount++;
            }
        }

        product.setProductParameters(productParameters);

        if (mappedCount > 0) {
            log.debug("Product {} parameter mapping: {} mapped, {} not found",
                    product.getSku(), mappedCount, notFoundCount);
        }
    }

    private ParameterOption findOrCreateAsbisParameterOption(Parameter parameter, String value) {
        try {
            // Validate length - after migration this is 1000 chars
            validateStringLength(value, "Parameter option value for '" + parameter.getNameBg() + "'",
                    PARAM_OPTION_MAX_LENGTH);

            // Try to find existing option
            Optional<ParameterOption> option = parameterOptionRepository
                    .findByParameterAndNameBg(parameter, value);

            if (option.isPresent()) {
                return option.get();
            }

            // Create new option
            ParameterOption newOption = new ParameterOption();
            newOption.setParameter(parameter);
            newOption.setNameBg(value);
            newOption.setNameEn(value);

            List<ParameterOption> existingOptions = parameterOptionRepository
                    .findByParameterIdOrderByOrderAsc(parameter.getId());
            newOption.setOrder(existingOptions.size());

            return parameterOptionRepository.save(newOption);

        } catch (Exception e) {
            log.error("Error finding/creating parameter option (length: {}) for {} = {}: {}",
                    value != null ? value.length() : 0, parameter.getNameBg(),
                    value != null && value.length() > 100 ? value.substring(0, 100) + "..." : value,
                    e.getMessage());
            return null;
        }
    }

    private String generateUniqueSlugForAsbis(String baseSlug, Category category) {
        if (!slugExistsInDatabase(baseSlug, category.getId())) {
            return baseSlug;
        }

        int counter = 1;
        String numberedSlug;
        do {
            numberedSlug = baseSlug + "-" + counter;
            counter++;
        } while (slugExistsInDatabase(numberedSlug, category.getId()) && counter < 100);

        return numberedSlug;
    }

    private boolean slugExistsInDatabase(String slug, Long excludeId) {
        List<Category> existing = categoryRepository.findAll().stream()
                .filter(cat -> slug.equals(cat.getSlug()))
                .toList();

        if (existing.isEmpty()) {
            return false;
        }

        if (excludeId != null) {
            existing = existing.stream()
                    .filter(cat -> !cat.getId().equals(excludeId))
                    .toList();
        }

        return !existing.isEmpty();
    }

    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
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
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Validate string length and log warning if exceeds database column limit
     * Note: For TEXT columns, we only log warnings for extremely long values (> 10000 chars)
     * @param str String to validate
     * @param fieldName Field name for logging
     * @param maxLength Maximum allowed length (database column size, or -1 for TEXT)
     * @return Original string (no truncation)
     */
    private String validateStringLength(String str, String fieldName, int maxLength) {
        if (str == null) {
            return null;
        }

        // -1 means TEXT column (unlimited)
        if (maxLength == -1) {
            // Only warn for VERY long values
            if (str.length() > 10000) {
                log.warn("⚠️ {} is very long: {} chars. Value: '{}'...",
                        fieldName, str.length(),
                        str.substring(0, Math.min(100, str.length())));
            }
            return str;
        }

        // For VARCHAR columns
        if (str.length() > maxLength) {
            log.warn("⚠️ {} exceeds max length: {} chars (max: {}). Value: '{}'...",
                    fieldName, str.length(), maxLength,
                    str.substring(0, Math.min(100, str.length())));
            log.warn("⚠️ This value will likely cause a database error! Consider increasing column size.");
        }
        return str;
    }

    // Database column size constants (after migration)
    private static final int PARAM_NAME_MAX_LENGTH = 500;      // parameters.name_bg/name_en
    private static final int PARAM_KEY_MAX_LENGTH = 500;       // parameters.asbis_key/tekra_key
    private static final int PARAM_OPTION_MAX_LENGTH = -1;     // parameter_options.name_bg/name_en (TEXT = unlimited)
    private static final int PRODUCT_MODEL_MAX_LENGTH = 500;   // products.model
    private static final int CATEGORY_ID_MAX_LENGTH = 500;     // categories.asbis_id
    private static final int MANUFACTURER_NAME_MAX_LENGTH = 255; // manufacturers.name

    // ===========================================
    // RESULT CLASSES
    // ===========================================

    private static class BatchResult {
        long processed;
        long created;
        long updated;
        long errors;
        long skippedNoCategory;

        BatchResult(long processed, long created, long updated, long errors, long skippedNoCategory) {
            this.processed = processed;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
            this.skippedNoCategory = skippedNoCategory;
        }
    }

    private static class ParameterOptionResult {
        long created;
        long updated;

        ParameterOptionResult(long created, long updated) {
            this.created = created;
            this.updated = updated;
        }
    }
}