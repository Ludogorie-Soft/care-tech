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

    @Transactional
    public void syncAsbisCategories() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_CATEGORIES");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== STARTING Asbis categories synchronization ===");

            // Measure API call time
            long apiStart = System.currentTimeMillis();
            List<Map<String, Object>> externalCategories = asbisApiService.extractCategories();
            long apiDuration = System.currentTimeMillis() - apiStart;

            log.info("✓ Asbis API call completed in {}ms, returned {} categories",
                    apiDuration, externalCategories.size());

            if (externalCategories.isEmpty()) {
                log.warn("No categories returned from Asbis API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No categories found", startTime);
                return;
            }

            // Load existing categories by Asbis ID
            Map<String, Category> existingCategoriesById = categoryRepository.findAll()
                    .stream()
                    .filter(cat -> cat.getAsbisId() != null && !cat.getAsbisId().isEmpty())
                    .collect(Collectors.toMap(
                            Category::getAsbisId,
                            cat -> cat,
                            (existing, duplicate) -> existing
                    ));

            log.info("Found {} existing Asbis categories in database", existingCategoriesById.size());

            long totalCreated = 0, totalUpdated = 0, totalSkipped = 0;

            // Process categories in correct hierarchical order (Level 1 -> Level 2 -> Level 3)
            for (int level = 1; level <= 3; level++) {
                log.info("=== STEP {}: Processing Level {} categories ===", level, level);

                int finalLevel = level;
                List<Map<String, Object>> levelCategories = externalCategories.stream()
                        .filter(cat -> getInteger(cat, "level") == finalLevel)
                        .toList();

                log.info("Found {} Level {} categories", levelCategories.size(), level);

                for (int i = 0; i < levelCategories.size(); i++) {
                    Map<String, Object> extCategory = levelCategories.get(i);

                    try {
                        String asbisId = getString(extCategory, "id");
                        String name = getString(extCategory, "name");
                        String parentId = getString(extCategory, "parent");

                        if (asbisId == null || name == null) {
                            log.warn("Skipping L{} category with missing id or name", level);
                            totalSkipped++;
                            continue;
                        }

                        // For levels 2 and 3, verify parent exists
                        Category parent = null;
                        if (level > 1 && parentId != null) {
                            parent = existingCategoriesById.get(parentId);
                            if (parent == null) {
                                log.warn("✗ Skipping L{} category '{}': parent '{}' not found",
                                        level, name, parentId);
                                totalSkipped++;
                                continue;
                            }
                        }

                        Category category = existingCategoriesById.get(asbisId);
                        boolean isNew = (category == null);

                        if (isNew) {
                            category = createAsbisCategoryFromExternal(extCategory, existingCategoriesById);
                            totalCreated++;
                        } else {
                            updateAsbisCategoryFromExternal(category, extCategory, existingCategoriesById);
                            totalUpdated++;
                        }

                        category = categoryRepository.save(category);
                        existingCategoriesById.put(asbisId, category);

                        log.info("  ✓ L{} [{}/{}]: '{}' (ID: {}, parent: '{}')",
                                level, i + 1, levelCategories.size(), name, category.getId(),
                                parent != null ? parent.getNameBg() : "none");

                    } catch (Exception e) {
                        log.error("Error processing L{} category: {}", level, e.getMessage(), e);
                        totalSkipped++;
                    }
                }

                // Flush after each level
                entityManager.flush();
                entityManager.clear();
                log.info("=== STEP {} COMPLETE: {} Level {} categories processed ===",
                        level, levelCategories.size(), level);
            }

            long totalCategories = totalCreated + totalUpdated;
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalCategories, totalCreated,
                    totalUpdated, totalSkipped,
                    totalSkipped > 0 ? String.format("Skipped %d categories", totalSkipped) : null,
                    startTime);

            log.info("=== COMPLETE: Asbis categories sync finished in {}ms ===",
                    System.currentTimeMillis() - startTime);
            log.info("Summary: Total={}, Created={}, Updated={}, Skipped={}",
                    totalCategories, totalCreated, totalUpdated, totalSkipped);

        } catch (Exception e) {
            log.error("=== FAILED: Asbis categories synchronization ===", e);
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0,
                    e.getMessage(), startTime);
            throw e;
        }
    }

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

            // Load existing parameters for ALL categories
            Map<String, Parameter> existingParameters = new HashMap<>();
            try {
                List<Parameter> allParameters = parameterRepository.findAll();
                existingParameters = allParameters.stream()
                        .filter(p -> p.getAsbisKey() != null && !p.getAsbisKey().isEmpty())
                        .collect(Collectors.toMap(
                                Parameter::getAsbisKey,
                                p -> p,
                                (existing, duplicate) -> {
                                    log.warn("Duplicate parameter key: {}, keeping first", existing.getAsbisKey());
                                    return existing;
                                }
                        ));
                log.info("Found {} existing parameters in database", existingParameters.size());
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
                        // Create parameter in ALL categories where it might be used
                        List<Parameter> createdParams = createParameterInAllCategories(parameterKey, asbisCategories);
                        if (!createdParams.isEmpty()) {
                            parameter = createdParams.get(0); // Use first one as reference
                            totalCreated += createdParams.size();
                            Map<String, Parameter> finalExistingParameters = existingParameters;
                            createdParams.forEach(p -> finalExistingParameters.put(p.getAsbisKey(), p));
                        } else {
                            log.error("Failed to create parameter in any category for key: {}", parameterKey);
                            totalErrors++;
                            continue;
                        }
                    } else {
                        totalUpdated++;
                    }

                    // Sync parameter options for the main parameter
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
                    log.info("  - Code: {}, Vendor: {}, Category: '{}', Type: '{}', Name: {}",
                            getString(p, "productcode"),
                            getString(p, "vendor"),
                            getString(p, "productcategory"),
                            getString(p, "producttype"),
                            getString(p, "productdescription"))
            );

            // STEP 2: Load lookup maps
            log.info("STEP 2: Loading categories and manufacturers...");
            long lookupStart = System.currentTimeMillis();

            // Categories by Asbis ID and name
            Map<String, Category> categoriesByAsbisId = new HashMap<>();
            Map<String, Category> categoriesByName = new HashMap<>();

            List<Category> allCategories = categoryRepository.findAll();
            for (Category cat : allCategories) {
                if (cat.getAsbisId() != null && !cat.getAsbisId().isEmpty()) {
                    categoriesByAsbisId.put(cat.getAsbisId(), cat);
                }
                if (cat.getNameBg() != null && !cat.getNameBg().isEmpty()) {
                    categoriesByName.put(cat.getNameBg().toLowerCase(), cat);
                }
            }

            Map<String, Manufacturer> manufacturersMap = manufacturerRepository.findAll().stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            Manufacturer::getName,
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            // Load all parameters for quick access
            Map<String, List<Parameter>> parametersByKey = parameterRepository.findAll().stream()
                    .filter(p -> p.getAsbisKey() != null && !p.getAsbisKey().isEmpty())
                    .collect(Collectors.groupingBy(Parameter::getAsbisKey));

            long lookupDuration = System.currentTimeMillis() - lookupStart;
            log.info("✓ Lookup maps loaded in {}ms: {} categories, {} manufacturers, {} parameters",
                    lookupDuration, categoriesByAsbisId.size(), manufacturersMap.size(), parametersByKey.size());

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
                            categoriesByName, manufacturersMap, parametersByKey);

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
    // BATCH PROCESSING HELPER - ПОПРАВЕН
    // ===========================================

    private BatchResult processProductsBatch(List<Map<String, Object>> batch,
                                             Map<String, Category> categoriesByAsbisId,
                                             Map<String, Category> categoriesByName,
                                             Map<String, Manufacturer> manufacturersMap,
                                             Map<String, List<Parameter>> parametersByKey) {
        long processed = 0, created = 0, updated = 0, errors = 0, skippedNoCategory = 0;

        for (Map<String, Object> rawProduct : batch) {
            try {
                String productCode = getString(rawProduct, "productcode");
                String productDescription = getString(rawProduct, "productdescription");

                if (productCode == null || productDescription == null) {
                    log.debug("Skipping product with missing code or description");
                    errors++;
                    continue;
                }

                // Find category using improved method
                Category category = findCategoryForProduct(rawProduct, categoriesByAsbisId, categoriesByName);

                if (category == null) {
                    String productCategory = getString(rawProduct, "productcategory");
                    String productType = getString(rawProduct, "producttype");
                    log.warn("✗ Skipping product '{}' ({}): category not found (category='{}', type='{}')",
                            productDescription, productCode, productCategory, productType);
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
                    } else {
                        log.warn("Manufacturer not found for product {}: {}", productCode, vendorName);
                    }
                }

                boolean isNew = (product.getId() == null);

                // Update product fields
                updateAsbisProductFields(product, rawProduct, category);

                // Set parameters from AttrList - ПОПРАВЕН МЕТОД
                setParametersToProduct(product, rawProduct, parametersByKey);

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
                        getString(rawProduct, "productcode"), e.getMessage(), e);
            }
        }

        return new BatchResult(processed, created, updated, errors, skippedNoCategory);
    }

    // ===========================================
    // ПОПРАВЕНИ HELPER METHODS
    // ===========================================

    private Category findCategoryForProduct(Map<String, Object> rawProduct,
                                            Map<String, Category> categoriesByAsbisId,
                                            Map<String, Category> categoriesByName) {
        String productCategory = getString(rawProduct, "productcategory"); // Level 1
        String productType = getString(rawProduct, "producttype");         // Level 2
        String sku = getString(rawProduct, "productcode");

        // STRATEGY 1: Try to find by Asbis ID (most reliable)
        if (productType != null && !productType.trim().isEmpty()) {
            Category category = categoriesByAsbisId.get(productType);
            if (category != null) {
                log.debug("✓ Product {} matched by Asbis ID: '{}'", sku, productType);
                return category;
            }
        }

        // STRATEGY 2: Try to find by name
        if (productType != null && !productType.trim().isEmpty()) {
            Category category = categoriesByName.get(productType.toLowerCase());
            if (category != null) {
                log.debug("✓ Product {} matched by name: '{}'", sku, productType);
                return category;
            }
        }

        // STRATEGY 3: Fallback to Level 1 category
        if (productCategory != null && !productCategory.trim().isEmpty()) {
            Category category = categoriesByAsbisId.get(productCategory);
            if (category != null) {
                log.debug("✓ Product {} matched Level 1: '{}'", sku, productCategory);
                return category;
            }

            category = categoriesByName.get(productCategory.toLowerCase());
            if (category != null) {
                log.debug("✓ Product {} matched Level 1 by name: '{}'", sku, productCategory);
                return category;
            }
        }

        // No match found
        log.warn("✗ No category match for product {}: productCategory='{}', productType='{}'",
                sku, productCategory, productType);
        return null;
    }

    private void setParametersToProduct(Product product, Map<String, Object> rawProduct,
                                        Map<String, List<Parameter>> parametersByKey) {
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

                // Find parameter in pre-loaded map
                List<Parameter> parameters = parametersByKey.get(parameterKey);
                if (parameters == null || parameters.isEmpty()) {
                    notFoundCount++;
                    continue;
                }

                // Find parameter that belongs to product's category or its parents
                Parameter parameter = findParameterForCategory(parameters, product.getCategory());
                if (parameter == null) {
                    notFoundCount++;
                    continue;
                }

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

        if (mappedCount > 0 || notFoundCount > 0) {
            log.info("Product {} parameter mapping: {} mapped, {} not found",
                    product.getSku(), mappedCount, notFoundCount);
        }
    }

    private Parameter findParameterForCategory(List<Parameter> parameters, Category targetCategory) {
        // First try to find parameter in the exact category
        for (Parameter param : parameters) {
            if (param.getCategory().getId().equals(targetCategory.getId())) {
                return param;
            }
        }

        // If not found, try parent categories
        Category currentCategory = targetCategory.getParent();
        while (currentCategory != null) {
            for (Parameter param : parameters) {
                if (param.getCategory().getId().equals(currentCategory.getId())) {
                    return param;
                }
            }
            currentCategory = currentCategory.getParent();
        }

        // If still not found, return the first parameter (global fallback)
        return parameters.isEmpty() ? null : parameters.get(0);
    }

    private List<Parameter> createParameterInAllCategories(String parameterKey, List<Category> categories) {
        List<Parameter> createdParameters = new ArrayList<>();

        for (Category category : categories) {
            try {
                Parameter parameter = createAsbisParameter(parameterKey, category);
                if (parameter != null) {
                    parameter = parameterRepository.save(parameter);
                    createdParameters.add(parameter);
                    log.debug("Created parameter '{}' in category '{}'", parameterKey, category.getNameBg());
                }
            } catch (Exception e) {
                log.error("Error creating parameter '{}' in category '{}': {}",
                        parameterKey, category.getNameBg(), e.getMessage());
            }
        }

        return createdParameters;
    }

    private Category createAsbisCategoryFromExternal(Map<String, Object> extCategory,
                                                     Map<String, Category> existingCategoriesById) {
        Category category = new Category();

        String asbisId = getString(extCategory, "id");
        String name = getString(extCategory, "name");
        String parentId = getString(extCategory, "parent");
        String fullPath = getString(extCategory, "fullPath");

        category.setAsbisId(asbisId);
        category.setNameBg(name);
        category.setNameEn(name);
        category.setCategoryPath(fullPath);

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
            category.setNameBg(name);
            category.setNameEn(name);
        }

        if (fullPath != null) {
            category.setCategoryPath(fullPath);
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
                            log.error("Error saving parameter option for parameter '{}': {}",
                                    parameter.getNameBg(), e.getMessage());
                        }
                    } else {
                        updated++;
                    }
                } catch (Exception e) {
                    log.error("Error processing parameter option: {}", e.getMessage());
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

        product.setReferenceNumber(productCode);
        product.setNameBg(productDescription);
        product.setNameEn(productDescription);

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

        // For now, set default status
        product.setShow(true);
        product.setStatus(ProductStatus.AVAILABLE);

        product.calculateFinalPrice();
    }

    private ParameterOption findOrCreateAsbisParameterOption(Parameter parameter, String value) {
        try {
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
            log.error("Error finding/creating parameter option for {} = {}: {}",
                    parameter.getNameBg(), value, e.getMessage());
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