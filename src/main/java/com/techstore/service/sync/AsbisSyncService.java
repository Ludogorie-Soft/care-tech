package com.techstore.service.sync;

import com.techstore.entity.*;
import com.techstore.enums.Platform;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.*;
import com.techstore.service.AsbisApiService;
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

/**
 * AsbisSyncService
 *
 * Стратегия за дедупликация:
 * - Manufacturers : по normalized name (cross-platform reuse)
 * - Categories    : по nameBg — reuse existing (от Vali/Tekra) или CREATE нова
 * - Parameters    : по asbisKey → fallback по nameBg → CREATE (cross-platform reuse)
 * - Options       : по "paramNameBg:::optionValueBg"
 * - Products      : по asbisCode (productcode от XML)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsbisSyncService {

    private final AsbisApiService asbisApiService;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;
    private final ParameterRepository parameterRepository;
    private final ParameterOptionRepository parameterOptionRepository;
    private final EntityManager entityManager;
    private final SyncHelper syncHelper;
    private final LogHelper logHelper;

    // =========================================================
    // MANUFACTURERS
    // =========================================================

    @Transactional
    public void syncAsbisManufacturers() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_MANUFACTURERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Asbis manufacturers synchronization - CREATE ONLY mode");

            List<Map<String, Object>> allProducts = asbisApiService.getAllProducts();
            log.info("Fetched {} products from Asbis API", allProducts.size());

            Set<String> vendorNames = allProducts.stream()
                    .map(p -> getString(p, "vendor"))
                    .filter(Objects::nonNull)
                    .filter(n -> !n.trim().isEmpty())
                    .collect(Collectors.toSet());

            log.info("Extracted {} unique manufacturers from Asbis products", vendorNames.size());

            Map<String, Manufacturer> existingManufacturers = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            long created = 0, skipped = 0;

            for (String name : vendorNames) {
                String normalized = normalizeManufacturerName(name);
                if (existingManufacturers.containsKey(normalized)) {
                    skipped++;
                    continue;
                }
                Manufacturer manufacturer = new Manufacturer();
                manufacturer.setName(name);
                manufacturer.setPlatform(Platform.ASBIS);
                manufacturer = manufacturerRepository.save(manufacturer);
                existingManufacturers.put(normalized, manufacturer);
                created++;
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    (long) vendorNames.size(), created, 0, 0,
                    String.format("Skipped %d existing", skipped), startTime);
            log.info("Asbis manufacturers sync completed - Created: {}, Skipped: {}", created, skipped);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Asbis manufacturers synchronization", e);
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    // CATEGORIES
    // =========================================================

    @Transactional
    public void syncAsbisCategories() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_CATEGORIES");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Asbis categories synchronization");

            List<Map<String, Object>> allProducts = asbisApiService.getAllProducts();

            // Collect unique level-1 (productcategory) and level-2 (producttype) names
            Map<String, Set<String>> level1ToLevel2 = new LinkedHashMap<>();
            for (Map<String, Object> product : allProducts) {
                String cat1 = getString(product, "productcategory");
                String cat2 = getString(product, "producttype");
                if (cat1 == null || cat1.isBlank()) continue;
                level1ToLevel2.computeIfAbsent(cat1.trim(), k -> new LinkedHashSet<>());
                if (cat2 != null && !cat2.isBlank()) {
                    level1ToLevel2.get(cat1.trim()).add(cat2.trim());
                }
            }

            log.info("Extracted {} level-1 categories from Asbis products", level1ToLevel2.size());

            // Load all existing categories — build parent-aware lookup structures
            List<Category> allExistingCats = categoryRepository.findAll();

            // Root categories (parent == null) → by normalizedNameBg
            Map<String, Category> rootByName = new HashMap<>();
            // Child categories → by "parentId:::normalizedNameBg"
            Map<String, Category> childByParentAndName = new HashMap<>();

            for (Category c : allExistingCats) {
                if (c.getNameBg() == null) continue;
                String normalizedName = c.getNameBg().toLowerCase().trim();
                if (c.getParent() == null) {
                    rootByName.put(normalizedName, c);
                } else {
                    childByParentAndName.put(c.getParent().getId() + ":::" + normalizedName, c);
                }
            }

            long created = 0, reused = 0;

            for (Map.Entry<String, Set<String>> entry : level1ToLevel2.entrySet()) {
                String cat1Name = entry.getKey();
                Set<String> cat2Names = entry.getValue();

                // Level 1 — match only among ROOT categories (parent == null)
                String cat1Key = cat1Name.toLowerCase().trim();
                Category parent = rootByName.get(cat1Key);
                if (parent == null) {
                    parent = createCategory(cat1Name, null);
                    rootByName.put(cat1Key, parent);
                    created++;
                    log.debug("✓ Created L1 root category: '{}'", cat1Name);
                } else {
                    reused++;
                    log.debug("↑ Reused L1 root category: '{}'", cat1Name);
                }

                // Level 2 — match only among CHILDREN of this specific parent
                for (String cat2Name : cat2Names) {
                    String cat2Key = cat2Name.toLowerCase().trim();
                    String childLookupKey = parent.getId() + ":::" + cat2Key;
                    Category child = childByParentAndName.get(childLookupKey);
                    if (child == null) {
                        child = createCategory(cat2Name, parent);
                        childByParentAndName.put(parent.getId() + ":::" + cat2Key, child);
                        created++;
                        log.debug("✓ Created L2 category: '{}' under '{}'", cat2Name, cat1Name);
                    } else {
                        reused++;
                        log.debug("↑ Reused L2 category: '{}' under '{}'", cat2Name, cat1Name);
                    }
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    created + reused, created, 0, 0,
                    String.format("Reused %d existing", reused), startTime);
            log.info("Asbis categories sync completed - Created: {}, Reused: {}", created, reused);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Asbis categories synchronization", e);
            throw new RuntimeException(e);
        }
    }

    private Category createCategory(String name, Category parent) {
        Category category = new Category();
        category.setNameBg(name);
        category.setNameEn(name);
        category.setParent(parent);
        category.setPlatform(Platform.ASBIS);
        category.setShow(true);
        category.setSortOrder(0);
        String slug = syncHelper.createSlugFromName(name);
        // Ensure unique slug
        if (categoryRepository.existsBySlugAndIdNot(slug, category.getId() != null ? category.getId() : -1L)) {
            slug = slug + "-asbis";
        }
        category.setSlug(slug);
        category.setCategoryPath(category.generateCategoryPath());
        return categoryRepository.save(category);
    }

    // =========================================================
    // PARAMETERS
    // =========================================================

    @Transactional
    public void syncAsbisParameters() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_PARAMETERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== Starting Asbis Parameters Synchronization ===");

            List<Parameter> allExistingParams = parameterRepository.findAll();
            for (Parameter p : allExistingParams) {
                if (p.getCategories() != null) p.getCategories().size();
            }

            // Primary lookup: by asbisKey
            Map<String, Parameter> globalParamsCache = new HashMap<>();
            for (Parameter p : allExistingParams) {
                if (p.getAsbisKey() != null) {
                    globalParamsCache.put(p.getAsbisKey(), p);
                }
            }

            // Fallback: ALL params without asbisKey, keyed by normalized nameBg
            Map<String, Parameter> existingByNormalizedNameBg = new HashMap<>();
            for (Parameter p : allExistingParams) {
                if (p.getNameBg() != null && p.getAsbisKey() == null) {
                    existingByNormalizedNameBg.put(p.getNameBg().toLowerCase().trim(), p);
                }
            }

            log.info("Loaded {} params with asbisKey, {} fallback by nameBg",
                    globalParamsCache.size(), existingByNormalizedNameBg.size());

            // Guard: categoryId → (normalizedNameBg → parameterId)
            // Prevents two different parameter entities with the same nameBg in the same category
            Map<Long, Map<String, Long>> paramNamesByCategory = new HashMap<>();
            for (Parameter p : allExistingParams) {
                if (p.getNameBg() == null || p.getCategories() == null) continue;
                String norm = p.getNameBg().toLowerCase().trim();
                for (Category cat : p.getCategories()) {
                    if (cat.getId() != null) {
                        paramNamesByCategory.computeIfAbsent(cat.getId(), k -> new HashMap<>())
                                .put(norm, p.getId());
                    }
                }
            }

            // Options cache: "parameterId:::normalizedValue" — tied to specific parameter ID
            List<ParameterOption> allExistingOptions = parameterOptionRepository.findAll();
            Map<String, ParameterOption> globalOptionsCache = new HashMap<>();
            for (ParameterOption opt : allExistingOptions) {
                if (opt.getParameter() != null && opt.getParameter().getId() != null && opt.getNameBg() != null) {
                    globalOptionsCache.put(buildOptionCacheKey(opt.getParameter().getId(), opt.getNameBg()), opt);
                }
            }

            // Load all categories for parameter mapping.
            // When multiple categories share the same nameBg (allowed for subcategories under different parents),
            // prefer the one whose parent nameBg best matches the Asbis productcategory context.
            // Simple strategy: build a multi-map by nameBg and resolve per-product.
            Map<String, List<Category>> categoriesByNameBgMulti = new HashMap<>();
            for (Category c : categoryRepository.findAll()) {
                if (c.getNameBg() != null) {
                    categoriesByNameBgMulti
                            .computeIfAbsent(c.getNameBg().toLowerCase().trim(), k -> new ArrayList<>())
                            .add(c);
                }
            }
            // Convenience: single-value map — prefer the one with the most specific (deepest) parent chain
            Map<String, Category> categoriesByNameBg = new HashMap<>();
            for (Map.Entry<String, List<Category>> e : categoriesByNameBgMulti.entrySet()) {
                // Sort: categories with a parent come first (subcategories preferred for parameter linking)
                e.getValue().sort((a, b) -> {
                    int aDepth = (a.getParent() != null) ? 1 : 0;
                    int bDepth = (b.getParent() != null) ? 1 : 0;
                    return Integer.compare(bDepth, aDepth);
                });
                categoriesByNameBg.put(e.getKey(), e.getValue().get(0));
            }

            // Collect all parameters from all Asbis products
            List<Map<String, Object>> allProducts = asbisApiService.getAllProducts();

            // Map: asbisKey → ParameterData
            Map<String, ParameterData> allParametersData = new LinkedHashMap<>();

            for (Map<String, Object> product : allProducts) {
                String cat1 = getString(product, "productcategory");
                String cat2 = getString(product, "producttype");

                Category category = null;
                if (cat2 != null && !cat2.isBlank()) {
                    category = categoriesByNameBg.get(cat2.toLowerCase().trim());
                }
                if (category == null && cat1 != null && !cat1.isBlank()) {
                    category = categoriesByNameBg.get(cat1.toLowerCase().trim());
                }

                @SuppressWarnings("unchecked")
                Map<String, String> attrList = (Map<String, String>) product.getOrDefault("attrlist", Collections.emptyMap());

                for (Map.Entry<String, String> attr : attrList.entrySet()) {
                    String attrName = attr.getKey();
                    String attrValue = attr.getValue();
                    if (attrName == null || attrName.isBlank() || attrValue == null || attrValue.isBlank()) continue;
                    if (attrValue.length() > 200) continue;

                    String asbisKey = generateAsbisKey(attrName);
                    final Category finalCategory = category;

                    ParameterData paramData = allParametersData.computeIfAbsent(asbisKey, k -> {
                        ParameterData pd = new ParameterData();
                        pd.nameBg = attrName;
                        pd.asbisKey = asbisKey;
                        pd.categories = new HashSet<>();
                        pd.values = new HashSet<>();
                        return pd;
                    });

                    if (finalCategory != null) paramData.categories.add(finalCategory);
                    paramData.values.add(attrValue.trim());
                }
            }

            log.info("Collected {} unique parameters from Asbis products", allParametersData.size());

            long created = 0, reused = 0, optionsCreated = 0;

            for (Map.Entry<String, ParameterData> entry : allParametersData.entrySet()) {
                String asbisKey = entry.getKey();
                ParameterData paramData = entry.getValue();

                try {
                    Parameter parameter = globalParamsCache.get(asbisKey);

                    if (parameter == null) {
                        // Cross-platform fallback by nameBg
                        String normalizedName = paramData.nameBg.toLowerCase().trim();
                        Parameter byNameBg = existingByNormalizedNameBg.remove(normalizedName);

                        if (byNameBg != null) {
                            byNameBg.setAsbisKey(asbisKey);
                            Set<Category> existCats = byNameBg.getCategories();
                            if (existCats == null) { existCats = new HashSet<>(); byNameBg.setCategories(existCats); }
                            Set<Long> existCatIds = existCats.stream().map(Category::getId).collect(Collectors.toSet());
                            for (Category c : paramData.categories) {
                                if (!existCatIds.contains(c.getId()) &&
                                        canAddCategoryToParam(byNameBg.getId(), byNameBg.getNameBg(), c, paramNamesByCategory)) {
                                    existCats.add(c);
                                    registerParamInCategory(byNameBg.getId(), byNameBg.getNameBg(), c, paramNamesByCategory);
                                }
                            }
                            parameter = parameterRepository.save(byNameBg);
                            globalParamsCache.put(asbisKey, parameter);
                            reused++;
                            log.debug("↑ Adopted parameter '{}' by nameBg (asbisKey={})", parameter.getNameBg(), asbisKey);
                        } else {
                            parameter = new Parameter();
                            parameter.setNameBg(paramData.nameBg);
                            parameter.setNameEn(paramData.nameBg);
                            parameter.setAsbisKey(asbisKey);
                            parameter.setPlatform(Platform.ASBIS);
                            parameter.setOrder(50);
                            // Filter categories through uniqueness guard before first save
                            String newNorm = paramData.nameBg.toLowerCase().trim();
                            Set<Category> validCats = new HashSet<>();
                            for (Category c : paramData.categories) {
                                if (canAddCategoryToParam(null, paramData.nameBg, c, paramNamesByCategory)) {
                                    validCats.add(c);
                                }
                            }
                            parameter.setCategories(validCats);
                            parameter.setCreatedBy("system");
                            parameter = parameterRepository.save(parameter);
                            for (Category c : parameter.getCategories()) {
                                registerParamInCategory(parameter.getId(), parameter.getNameBg(), c, paramNamesByCategory);
                            }
                            globalParamsCache.put(asbisKey, parameter);
                            created++;
                            log.debug("✓ Created parameter: '{}' (asbisKey={})", paramData.nameBg, asbisKey);
                        }
                    } else {
                        // Update categories for already-cached parameter
                        Set<Category> existCats = parameter.getCategories();
                        if (existCats == null) { existCats = new HashSet<>(); parameter.setCategories(existCats); }
                        Set<Long> existCatIds = existCats.stream().map(Category::getId).collect(Collectors.toSet());
                        boolean updated = false;
                        for (Category c : paramData.categories) {
                            if (!existCatIds.contains(c.getId()) &&
                                    canAddCategoryToParam(parameter.getId(), parameter.getNameBg(), c, paramNamesByCategory)) {
                                existCats.add(c);
                                registerParamInCategory(parameter.getId(), parameter.getNameBg(), c, paramNamesByCategory);
                                updated = true;
                            }
                        }
                        if (updated) parameterRepository.save(parameter);
                        reused++;
                    }

                    optionsCreated += createOptionsForParameter(parameter, paramData.values, globalOptionsCache);

                } catch (Exception e) {
                    log.error("Error processing Asbis parameter '{}': {}", asbisKey, e.getMessage());
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    allParametersData.size(), created, 0, 0,
                    String.format("Created: %d, Reused: %d, Options: %d", created, reused, optionsCreated),
                    startTime);

            log.info("=== Asbis Parameters Sync Completed: created={}, reused={}, options={} ===",
                    created, reused, optionsCreated);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Asbis parameters sync failed", e);
            throw new RuntimeException(e);
        }
    }

    private int createOptionsForParameter(Parameter parameter, Set<String> values,
                                          Map<String, ParameterOption> globalOptionsCache) {
        int created = 0;
        for (String value : values) {
            if (value == null || value.isBlank() || "-".equals(value.trim())) continue;
            String cacheKey = buildOptionCacheKey(parameter.getId(), value);
            if (globalOptionsCache.containsKey(cacheKey)) continue;
            ParameterOption option = new ParameterOption();
            option.setParameter(parameter);
            option.setNameBg(value);
            option.setNameEn(value);
            option.setOrder(globalOptionsCache.size());
            option = parameterOptionRepository.save(option);
            globalOptionsCache.put(cacheKey, option);
            created++;
        }
        return created;
    }

    // =========================================================
    // PRODUCTS
    // =========================================================

    @Transactional
    public void syncAsbisProducts() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_PRODUCTS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== Starting Asbis products synchronization ===");

            // Load manufacturers by normalized name
            Map<String, Manufacturer> manufacturersMap = manufacturerRepository.findAll().stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (e, d) -> e
                    ));
            log.info("Loaded {} manufacturers", manufacturersMap.size());

            // Load parameters by asbisKey
            Map<String, Parameter> paramsByAsbisKey = parameterRepository.findAll().stream()
                    .filter(p -> p.getAsbisKey() != null)
                    .collect(Collectors.toMap(Parameter::getAsbisKey, p -> p, (e, d) -> e));
            log.info("Loaded {} parameters with asbisKey", paramsByAsbisKey.size());

            // Load options by parameterId → normalized value
            Map<Long, Map<String, ParameterOption>> optionsByParameterId = new HashMap<>();
            parameterOptionRepository.findAll().forEach(opt -> {
                if (opt.getParameter() != null && opt.getNameBg() != null) {
                    optionsByParameterId
                            .computeIfAbsent(opt.getParameter().getId(), k -> new HashMap<>())
                            .put(normalizeName(opt.getNameBg()), opt);
                }
            });

            // Load categories — parent-aware to correctly resolve same-name subcategories under different parents
            List<Category> allCategoriesForProducts = categoryRepository.findAll();
            Map<String, Category> rootCategoriesByName = new HashMap<>();
            Map<String, Category> childCategoriesByParentAndName = new HashMap<>();
            for (Category c : allCategoriesForProducts) {
                if (c.getNameBg() == null) continue;
                String normalizedName = c.getNameBg().toLowerCase().trim();
                if (c.getParent() == null) {
                    rootCategoriesByName.put(normalizedName, c);
                } else {
                    childCategoriesByParentAndName.put(c.getParent().getId() + ":::" + normalizedName, c);
                }
            }

            List<Map<String, Object>> allProducts = asbisApiService.getAllProducts();
            log.info("Fetched {} products from Asbis API", allProducts.size());

            if (allProducts.isEmpty()) {
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No products found", startTime);
                return;
            }

            // Pre-load existing Asbis products by asbisCode for bulk deduplication
            Set<String> productCodes = allProducts.stream()
                    .map(p -> getString(p, "productcode"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<String, Product> existingByAsbisCode = new HashMap<>();
            // Process in batches to avoid huge IN clauses
            List<String> codeList = new ArrayList<>(productCodes);
            int batchSize = 200;
            for (int i = 0; i < codeList.size(); i += batchSize) {
                List<String> batch = codeList.subList(i, Math.min(i + batchSize, codeList.size()));
                productRepository.findByAsbisCodeIn(batch)
                        .forEach(p -> existingByAsbisCode.put(p.getAsbisCode(), p));
            }

            long totalProcessed = 0, totalCreated = 0, totalUpdated = 0, totalErrors = 0;
            long skippedNoCategory = 0, skippedNoManufacturer = 0;

            for (Map<String, Object> asbisProduct : allProducts) {
                try {
                    String productCode = getString(asbisProduct, "productcode");
                    if (productCode == null || productCode.isBlank()) { totalErrors++; continue; }

                    // Resolve category: prefer producttype (L2 child under L1 parent), fallback productcategory (L1 root)
                    String cat2 = getString(asbisProduct, "producttype");
                    String cat1 = getString(asbisProduct, "productcategory");
                    Category category = null;
                    if (cat2 != null && !cat2.isBlank() && cat1 != null && !cat1.isBlank()) {
                        // Find root parent first, then look for child under it (exact parent-child match)
                        Category parent = rootCategoriesByName.get(cat1.toLowerCase().trim());
                        if (parent != null) {
                            category = childCategoriesByParentAndName.get(parent.getId() + ":::" + cat2.toLowerCase().trim());
                        }
                    }
                    if (category == null && cat2 != null && !cat2.isBlank()) {
                        // Fallback: try cat2 as a root category
                        category = rootCategoriesByName.get(cat2.toLowerCase().trim());
                    }
                    if (category == null && cat1 != null && !cat1.isBlank()) {
                        // Fallback: use L1 root category
                        category = rootCategoriesByName.get(cat1.toLowerCase().trim());
                    }
                    if (category == null) {
                        skippedNoCategory++;
                        continue;
                    }

                    // Resolve manufacturer
                    String vendorName = getString(asbisProduct, "vendor");
                    Manufacturer manufacturer = null;
                    if (vendorName != null && !vendorName.isBlank()) {
                        manufacturer = manufacturersMap.get(normalizeManufacturerName(vendorName));
                    }
                    if (manufacturer == null) {
                        skippedNoManufacturer++;
                        continue;
                    }

                    Product product = existingByAsbisCode.get(productCode);
                    boolean isNew = (product == null);

                    if (isNew) {
                        product = new Product();
                        product.setAsbisCode(productCode);
                        product.setSku(productCode);
                        product.setPlatform(Platform.ASBIS);
                        product.setCreatedBy("system");
                        product.setManufacturer(manufacturer);
                        product.setCategory(category);
                        setAsbisProductFields(product, asbisProduct, true);
                    } else {
                        product.setCategory(category);
                        setAsbisProductFields(product, asbisProduct, false);
                    }

                    product = productRepository.save(product);

                    if (isNew) {
                        existingByAsbisCode.put(productCode, product);
                    }

                    // Set parameters
                    try {
                        setAsbisParametersToProduct(product, asbisProduct, paramsByAsbisKey, optionsByParameterId);
                        productRepository.save(product);
                    } catch (Exception e) {
                        log.error("Error setting parameters for Asbis product {}: {}", productCode, e.getMessage());
                    }

                    if (isNew) totalCreated++; else totalUpdated++;
                    totalProcessed++;

                    if (totalProcessed % 100 == 0) {
                        log.info("Progress: {}/{} (created={}, updated={})",
                                totalProcessed, allProducts.size(), totalCreated, totalUpdated);
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    totalErrors++;
                    log.error("Error processing Asbis product: {}", e.getMessage());
                    if (totalErrors <= 3) log.error("Full exception:", e);
                }
            }

            String message = String.format(
                    "Total: %d, Created: %d, Updated: %d, SkippedNoCat: %d, SkippedNoMfg: %d, Errors: %d",
                    totalProcessed, totalCreated, totalUpdated, skippedNoCategory, skippedNoManufacturer, totalErrors);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, totalCreated,
                    totalUpdated, totalErrors, message, startTime);
            log.info("=== Asbis Products Sync Completed: {} ===", message);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Asbis products sync failed", e);
            throw new RuntimeException(e);
        }
    }

    private void setAsbisProductFields(Product product, Map<String, Object> asbisProduct, boolean isNew) {
        if (isNew) {
            String description = getString(asbisProduct, "productdescription");
            if (description != null && !description.isBlank()) {
                product.setDescriptionBg(description);
                product.setDescriptionEn(description);
            }

            // Primary image
            String image = getString(asbisProduct, "image");
            if (image != null && !image.isBlank()) {
                product.setPrimaryImageUrl(image);
            }

            // Additional images
            @SuppressWarnings("unchecked")
            List<String> images = (List<String>) asbisProduct.get("images");
            if (images != null && !images.isEmpty()) {
                if (product.getPrimaryImageUrl() == null) {
                    product.setPrimaryImageUrl(images.get(0));
                    if (images.size() > 1) {
                        product.setAdditionalImages(new ArrayList<>(images.subList(1, images.size())));
                    }
                } else {
                    product.setAdditionalImages(new ArrayList<>(images));
                }
            }
        }

        // Price and status — always update
        String productCode = getString(asbisProduct, "productcode");
        // Name from productdescription (Asbis uses this as product name)
        String description = getString(asbisProduct, "productdescription");
        if (description != null && !description.isBlank()) {
            product.setNameBg(description);
            product.setNameEn(description);
        } else if (product.getNameBg() == null) {
            product.setNameBg(productCode);
            product.setNameEn(productCode);
        }

        // New Asbis products start hidden — they become visible only after
        // syncAsbisPriceAvail() confirms a positive price AND stock availability.
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.NOT_AVAILABLE);
            product.setShow(false);
        }

        product.calculateFinalPrice();
    }

    @SuppressWarnings("unchecked")
    private void setAsbisParametersToProduct(Product product, Map<String, Object> asbisProduct,
                                             Map<String, Parameter> paramsByAsbisKey,
                                             Map<Long, Map<String, ParameterOption>> optionsByParameterId) {
        Set<ProductParameter> existingParams = product.getProductParameters();
        if (existingParams == null) {
            existingParams = new HashSet<>();
            product.setProductParameters(existingParams);
        }

        // Build target set from Asbis attrlist: key = "parameterId-optionId"
        Map<String, Parameter> targetParams = new LinkedHashMap<>();
        Map<String, ParameterOption> targetOptions = new LinkedHashMap<>();

        Map<String, String> attrList = (Map<String, String>) asbisProduct.getOrDefault("attrlist", Collections.emptyMap());

        for (Map.Entry<String, String> attr : attrList.entrySet()) {
            String attrName = attr.getKey();
            String attrValue = attr.getValue();
            if (attrValue == null || attrValue.isBlank()) continue;

            String asbisKey = generateAsbisKey(attrName);
            Parameter parameter = paramsByAsbisKey.get(asbisKey);
            if (parameter == null) continue;

            Map<String, ParameterOption> paramOptions = optionsByParameterId.get(parameter.getId());
            if (paramOptions == null) continue;

            String normalizedValue = normalizeName(attrValue);
            ParameterOption option = paramOptions.get(normalizedValue);
            if (option == null) continue;

            String key = parameter.getId() + "-" + option.getId();
            targetParams.put(key, parameter);
            targetOptions.put(key, option);
        }

        // Remove only entries no longer in target (preserve admin-set parameters).
        // Do NOT use clear()+addAll() — Hibernate flushes INSERTs before DELETEs,
        // which causes unique constraint violations when re-adding the same combinations.
        existingParams.removeIf(pp -> {
            if (pp.getParameter() == null || pp.getParameterOption() == null) return true;
            if (isAdminUser(pp.getParameter().getCreatedBy()) || isAdminUser(pp.getParameter().getLastModifiedBy())) {
                return false;
            }
            String key = pp.getParameter().getId() + "-" + pp.getParameterOption().getId();
            return !targetParams.containsKey(key);
        });

        // Collect keys already present to avoid duplicate inserts
        Set<String> existingKeys = existingParams.stream()
                .filter(pp -> pp.getParameter() != null && pp.getParameterOption() != null)
                .map(pp -> pp.getParameter().getId() + "-" + pp.getParameterOption().getId())
                .collect(Collectors.toSet());

        // Add only entries not already present
        for (String key : targetParams.keySet()) {
            if (existingKeys.contains(key)) continue;
            ProductParameter pp = new ProductParameter();
            pp.setProduct(product);
            pp.setParameter(targetParams.get(key));
            pp.setParameterOption(targetOptions.get(key));
            existingParams.add(pp);
        }
    }

    // =========================================================
    // PRICE & AVAILABILITY
    // =========================================================

    @Transactional
    public void syncAsbisPriceAvail() {
        SyncLog syncLog = logHelper.createSyncLogSimple("ASBIS_PRICE_AVAIL");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== Starting Asbis price/availability synchronization ===");

            List<Map<String, Object>> priceData = asbisApiService.getAllPriceAvailability();

            if (priceData.isEmpty()) {
                log.warn("No price/availability data returned from Asbis API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No data", startTime);
                return;
            }

            log.info("Fetched {} price/availability records", priceData.size());

            // Collect all product codes from price data
            Set<String> productCodes = priceData.stream()
                    .map(p -> getString(p, "productcode"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Bulk pre-load existing products by asbisCode
            Map<String, Product> productsByAsbisCode = new HashMap<>();
            List<String> codeList = new ArrayList<>(productCodes);
            int batchSize = 200;
            for (int i = 0; i < codeList.size(); i += batchSize) {
                List<String> batch = codeList.subList(i, Math.min(i + batchSize, codeList.size()));
                productRepository.findByAsbisCodeIn(batch)
                        .forEach(p -> productsByAsbisCode.put(p.getAsbisCode(), p));
            }

            log.info("Matched {} products in database", productsByAsbisCode.size());

            // Pre-load SKUs that are already visible on higher-priority platforms (Vali, Tekra).
            // If an Asbis product's SKU already exists on one of these platforms, keep it hidden
            // to avoid showing duplicate products to customers.
            Set<String> higherPrioritySkus = new HashSet<>(
                    productRepository.findVisibleSkusByPlatforms(List.of(Platform.VALI, Platform.TEKRA))
            );
            log.info("Found {} SKUs already covered by Vali/Tekra — Asbis duplicates will stay hidden", higherPrioritySkus.size());

            long updated = 0, notFound = 0, errors = 0;

            for (Map<String, Object> priceRecord : priceData) {
                try {
                    String productCode = getString(priceRecord, "productcode");
                    if (productCode == null) { errors++; continue; }

                    Product product = productsByAsbisCode.get(productCode);
                    if (product == null) { notFound++; continue; }

                    // Price
                    Object priceObj = priceRecord.get("price");
                    BigDecimal price = (priceObj instanceof BigDecimal) ? (BigDecimal) priceObj : null;
                    if (price != null) {
                        product.setPriceClient(price);
                    }

                    // Stock / availability
                    Object stockObj = priceRecord.get("stock");
                    int stock = (stockObj instanceof Integer) ? (Integer) stockObj : 0;
                    boolean inStock = stock > 0;
                    boolean hasPrice = price != null && price.compareTo(BigDecimal.ZERO) > 0;
                    boolean isDuplicate = product.getSku() != null && higherPrioritySkus.contains(product.getSku());
                    boolean visible = inStock && hasPrice && !isDuplicate;
                    product.setShow(visible);
                    product.setStatus(visible ? ProductStatus.AVAILABLE : ProductStatus.NOT_AVAILABLE);

                    product.calculateFinalPrice();
                    productRepository.save(product);
                    updated++;

                    if (updated % 200 == 0) {
                        log.info("Price sync progress: {}/{}", updated, productsByAsbisCode.size());
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    errors++;
                    log.error("Error updating price for product: {}", e.getMessage());
                }
            }

            // Hide products not present in PriceAvail.xml — they have no confirmed price/availability.
            // Guard: skip if set is empty — passing empty collection to NOT IN hides everything.
            int hidden = 0;
            if (!productsByAsbisCode.isEmpty()) {
                hidden = productRepository.hideAsbisProductsNotIn(productsByAsbisCode.keySet());
                log.info("Hidden {} Asbis products absent from PriceAvail feed", hidden);
            } else {
                log.warn("productsByAsbisCode is empty — skipping hideAsbisProductsNotIn to prevent mass-hiding. " +
                         "Possible cause: WIC codes in PriceAvail.xml do not match ProductCode in catalog XML.");
            }

            String message = String.format("Updated: %d, Hidden: %d, NotFound: %d, Errors: %d", updated, hidden, notFound, errors);
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, updated, 0, updated, errors, message, startTime);
            log.info("=== Asbis Price/Avail Sync Completed: {} ===", message);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Asbis price/avail sync failed", e);
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private static class ParameterData {
        String nameBg;
        String asbisKey;
        Set<Category> categories;
        Set<String> values;
    }

    /**
     * Returns true if it's safe to link the given category to the given parameter.
     * Blocks the link if another parameter entity with the same nameBg is already in that category.
     */
    private boolean canAddCategoryToParam(Long paramId, String nameBg, Category cat,
                                          Map<Long, Map<String, Long>> paramNamesByCategory) {
        if (cat.getId() == null || nameBg == null) return true;
        Map<String, Long> namesInCat = paramNamesByCategory.get(cat.getId());
        if (namesInCat == null) return true;
        Long existingId = namesInCat.get(nameBg.toLowerCase().trim());
        if (existingId == null) return true;
        if (existingId.equals(paramId)) return true; // Same parameter — OK
        log.warn("Blocked duplicate param '{}' in category '{}' — already owned by param id={}",
                nameBg, cat.getNameBg(), existingId);
        return false;
    }

    private void registerParamInCategory(Long paramId, String nameBg, Category cat,
                                         Map<Long, Map<String, Long>> paramNamesByCategory) {
        if (paramId == null || cat.getId() == null || nameBg == null) return;
        paramNamesByCategory.computeIfAbsent(cat.getId(), k -> new HashMap<>())
                .put(nameBg.toLowerCase().trim(), paramId);
    }

    private String generateAsbisKey(String attrName) {
        if (attrName == null || attrName.isBlank()) return "unknown_attr";
        return attrName.toLowerCase()
                .replaceAll("[^a-z0-9а-яё\\s]+", " ")
                .trim()
                .replaceAll("\\s+", "_");
    }

    private String buildOptionCacheKey(Long parameterId, String optionValue) {
        return parameterId + ":::" + normalizeName(optionValue);
    }

    private String normalizeManufacturerName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ").replaceAll("[^a-zа-я0-9\\s]+", "");
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private boolean isAdminUser(String username) {
        if (username == null || username.isEmpty()) return false;
        return "ADMIN".equalsIgnoreCase(username.trim()) || "admin".equalsIgnoreCase(username.trim());
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
