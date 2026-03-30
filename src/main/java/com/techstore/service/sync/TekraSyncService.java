package com.techstore.service.sync;

import com.techstore.entity.Category;
import com.techstore.entity.Manufacturer;
import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import com.techstore.entity.Product;
import com.techstore.entity.ProductParameter;
import com.techstore.entity.SyncLog;
import com.techstore.enums.Platform;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ParameterOptionRepository;
import com.techstore.repository.ParameterRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.service.TekraApiService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class TekraSyncService {

    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;
    private final ParameterRepository parameterRepository;
    private final ParameterOptionRepository parameterOptionRepository;
    private final EntityManager entityManager;
    private final TekraApiService tekraApiService;
    private final LogHelper logHelper;
    private final SyncHelper syncHelper;

    @Transactional
    public void syncTekraCategories() {
        SyncLog syncLog = logHelper.createSyncLogSimple("TEKRA_CATEGORIES");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Tekra categories synchronization with parent validation");

            List<Map<String, Object>> externalCategories = tekraApiService.getCategoriesRaw();

            if (externalCategories.isEmpty()) {
                log.warn("No categories returned from Tekra API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No categories found", startTime);
                return;
            }

            Map<String, Object> mainCategory = externalCategories.stream()
                    .filter(extCategory -> "videonablyudenie".equals(getString(extCategory, "slug")))
                    .findFirst()
                    .orElse(null);

            if (mainCategory == null) {
                log.warn("Category 'videonablyudenie' not found in Tekra API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "Main category not found", startTime);
                return;
            }

            Map<String, Category> existingCategories = categoryRepository.findAll()
                    .stream()
                    .filter(cat -> cat.getSlug() != null)
                    .collect(Collectors.toMap(
                            Category::getSlug,
                            cat -> cat,
                            (existing, duplicate) -> existing
                    ));

            long created = 0, updated = 0, skipped = 0;

            log.info("=== STEP 1: Creating main category ===");
            Category mainCat = createOrUpdateTekraCategory(mainCategory, existingCategories, null);
            if (mainCat != null) {
                if (existingCategories.containsKey(mainCat.getSlug())) {
                    updated++;
                } else {
                    created++;
                    existingCategories.put(mainCat.getSlug(), mainCat);
                }
            }

            log.info("=== STEP 2: Processing level-2 categories ===");
            Object subCategoriesObj = mainCategory.get("sub_categories");
            if (!(subCategoriesObj instanceof List)) {
                log.warn("No sub_categories found in main category");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 1, created, updated, 0, "No subcategories", startTime);
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subCategories = (List<Map<String, Object>>) subCategoriesObj;

            Map<String, Category> level2Categories = new HashMap<>();

            for (int i = 0; i < subCategories.size(); i++) {
                Map<String, Object> subCat = subCategories.get(i);

                try {
                    String subCatSlug = getString(subCat, "slug");
                    String subCatName = getString(subCat, "name");

                    if (subCatSlug == null || subCatName == null) {
                        skipped++;
                        continue;
                    }

                    Category level2Cat = createOrUpdateTekraCategory(subCat, existingCategories, mainCat);
                    if (level2Cat != null) {
                        String level2Key = level2Cat.getSlug();

                        if (existingCategories.containsKey(level2Key)) {
                            updated++;
                        } else {
                            created++;
                            existingCategories.put(level2Key, level2Cat);
                        }

                        level2Categories.put(subCatSlug, level2Cat);
                    }

                } catch (Exception e) {
                    log.error("ERROR processing level-2 category [{}]: {}", i + 1, e.getMessage(), e);
                    skipped++;
                }
            }

            log.info("=== STEP 3: Processing level-3 categories ===");

            int totalLevel3 = 0;
            for (int i = 0; i < subCategories.size(); i++) {
                Map<String, Object> subCat = subCategories.get(i);

                try {
                    String subCatSlug = getString(subCat, "slug");
                    Category parentCategory = level2Categories.get(subCatSlug);

                    if (parentCategory == null) {
                        continue;
                    }

                    Object subSubCatObj = subCat.get("subsubcat");
                    if (!(subSubCatObj instanceof List)) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> subSubCategories = (List<Map<String, Object>>) subSubCatObj;

                    for (int j = 0; j < subSubCategories.size(); j++) {
                        Map<String, Object> subSubCat = subSubCategories.get(j);

                        try {
                            String subSubCatSlug = getString(subSubCat, "slug");
                            String subSubCatName = getString(subSubCat, "name");

                            if (subSubCatSlug == null || subSubCatName == null) {
                                skipped++;
                                continue;
                            }

                            if (parentCategory.getId() == null) {
                                skipped++;
                                continue;
                            }

                            Category level3Cat = createOrUpdateTekraCategory(
                                    subSubCat, existingCategories, parentCategory);

                            if (level3Cat != null) {
                                String level3Key = level3Cat.getSlug();

                                if (existingCategories.containsKey(level3Key)) {
                                    updated++;
                                } else {
                                    created++;
                                    existingCategories.put(level3Key, level3Cat);
                                }

                                totalLevel3++;
                            }

                        } catch (Exception e) {
                            log.error("  ERROR processing level-3 category: {}", e.getMessage(), e);
                            skipped++;
                        }
                    }

                } catch (Exception e) {
                    log.error("ERROR processing subcategories: {}", e.getMessage(), e);
                    skipped++;
                }
            }

            entityManager.flush();
            entityManager.clear();

            long totalCategories = created + updated;
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalCategories, created, updated, skipped,
                    skipped > 0 ? String.format("Skipped %d categories", skipped) : null, startTime);

            log.info("Tekra categories sync completed - Total: {}, Created: {}, Updated: {}, Skipped: {}",
                    totalCategories, created, updated, skipped);

        } catch (Exception e) {
            log.error("=== SYNC FAILED ===", e);
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            throw e;
        }
    }

    @Transactional
    public void ensureAnalogDigitalSubcategories() {
        log.info("=== Ensuring analog/digital subcategories exist ===");

        try {
            Optional<Category> camerasOpt = categoryRepository.findByNameBg("Камери");
            Category camerasParent = camerasOpt.orElse(null);

            if (!categoryRepository.findByNameBg("IP Камери").isPresent()) {
                Category ipCameras = new Category();
                ipCameras.setNameBg("IP Камери");
                ipCameras.setNameEn("IP Cameras");
                ipCameras.setSlug(syncHelper.createSlugFromName("IP Камери"));
                ipCameras.setParent(camerasParent);
                ipCameras.setPlatform(Platform.TEKRA);
                ipCameras.setShow(true);
                ipCameras.setSortOrder(1);
                if (ipCameras.getParent() != null) {
                    ipCameras.setCategoryPath(ipCameras.generateCategoryPath());
                }
                categoryRepository.save(ipCameras);
                log.info("✓ Created subcategory: IP Камери");
            }

            if (!categoryRepository.findByNameBg("Аналогови камери").isPresent()) {
                Category analogCameras = new Category();
                analogCameras.setNameBg("Аналогови камери");
                analogCameras.setNameEn("Analog Cameras");
                analogCameras.setSlug(syncHelper.createSlugFromName("Аналогови камери"));
                analogCameras.setParent(camerasParent);
                analogCameras.setPlatform(Platform.TEKRA);
                analogCameras.setShow(true);
                analogCameras.setSortOrder(2);
                if (analogCameras.getParent() != null) {
                    analogCameras.setCategoryPath(analogCameras.generateCategoryPath());
                }
                categoryRepository.save(analogCameras);
                log.info("✓ Created subcategory: Аналогови камери");
            }

            Optional<Category> recordersOpt = categoryRepository.findByNameBg("Рекордери");
            Category recordersParent = recordersOpt.orElse(null);

            if (!categoryRepository.findByNameBg("NVR Рекордери").isPresent()) {
                Category nvrRecorders = new Category();
                nvrRecorders.setNameBg("NVR Рекордери");
                nvrRecorders.setNameEn("NVR Recorders");
                nvrRecorders.setSlug(syncHelper.createSlugFromName("NVR Рекордери"));
                nvrRecorders.setParent(recordersParent);
                nvrRecorders.setPlatform(Platform.TEKRA);
                nvrRecorders.setShow(true);
                nvrRecorders.setSortOrder(1);
                if (nvrRecorders.getParent() != null) {
                    nvrRecorders.setCategoryPath(nvrRecorders.generateCategoryPath());
                }
                categoryRepository.save(nvrRecorders);
                log.info("✓ Created subcategory: NVR Рекордери");
            }

            if (!categoryRepository.findByNameBg("DVR Рекордери").isPresent()) {
                Category dvrRecorders = new Category();
                dvrRecorders.setNameBg("DVR Рекордери");
                dvrRecorders.setNameEn("DVR Recorders");
                dvrRecorders.setSlug(syncHelper.createSlugFromName("DVR Рекордери"));
                dvrRecorders.setParent(recordersParent);
                dvrRecorders.setPlatform(Platform.TEKRA);
                dvrRecorders.setShow(true);
                dvrRecorders.setSortOrder(2);
                if (dvrRecorders.getParent() != null) {
                    dvrRecorders.setCategoryPath(dvrRecorders.generateCategoryPath());
                }
                categoryRepository.save(dvrRecorders);
                log.info("✓ Created subcategory: DVR Рекордери");
            }

            log.info("✓ All analog/digital subcategories ensured");

        } catch (Exception e) {
            log.error("Error ensuring analog/digital subcategories: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void syncTekraManufacturers() {
        SyncLog syncLog = logHelper.createSyncLogSimple("TEKRA_MANUFACTURERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Tekra manufacturers synchronization");

            List<Category> tekraCategories = categoryRepository.findAll().stream()
                    .filter(cat -> cat.getTekraSlug() != null)
                    .toList();

            if (tekraCategories.isEmpty()) {
                log.warn("No Tekra categories found");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No Tekra categories found", startTime);
                return;
            }

            Set<String> allTekraManufacturers = new HashSet<>();

            for (Category category : tekraCategories) {
                try {
                    Set<String> categoryManufacturers = tekraApiService
                            .extractTekraManufacturersFromProducts(category.getTekraSlug());
                    allTekraManufacturers.addAll(categoryManufacturers);
                } catch (Exception e) {
                    log.error("Error extracting manufacturers: {}", e.getMessage());
                }
            }

            if (allTekraManufacturers.isEmpty()) {
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No manufacturers found", startTime);
                return;
            }

            Map<String, Manufacturer> existingManufacturers = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            long created = 0, updated = 0, errors = 0;

            for (String manufacturerName : allTekraManufacturers) {
                try {
                    String normalized = normalizeManufacturerName(manufacturerName);
                    Manufacturer manufacturer = existingManufacturers.get(normalized);

                    if (manufacturer == null) {
                        manufacturer = createTekraManufacturer(manufacturerName);
                        if (manufacturer != null) {
                            manufacturer = manufacturerRepository.save(manufacturer);
                            existingManufacturers.put(normalized, manufacturer);
                            created++;
                        } else {
                            errors++;
                        }
                    } else {
                        updated++;
                    }
                } catch (Exception e) {
                    errors++;
                    log.error("Error processing manufacturer {}: {}", manufacturerName, e.getMessage());
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, (long) allTekraManufacturers.size(),
                    created, updated, errors,
                    errors > 0 ? String.format("Completed with %d errors", errors) : null, startTime);

            log.info("Manufacturers sync completed - Created: {}, Skipped: {}", created, updated);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            throw e;
        }
    }

    @Transactional
    public void syncTekraParameters() {
        SyncLog syncLog = logHelper.createSyncLogSimple("TEKRA_PARAMETERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== Starting GLOBAL Tekra Parameters Synchronization ===");

            List<Parameter> allExistingParams = parameterRepository.findAll();

            for (Parameter p : allExistingParams) {
                if (p.getCategories() != null) {
                    p.getCategories().size();
                }
            }

            Map<String, Parameter> globalParamsCache = new HashMap<>();
            for (Parameter p : allExistingParams) {
                if (p.getTekraKey() != null) {
                    globalParamsCache.put(p.getTekraKey(), p);
                }
            }

            log.info("Loaded {} existing parameters with tekraKey from database", globalParamsCache.size());

            List<ParameterOption> allExistingOptions = parameterOptionRepository.findAll();

            Map<String, ParameterOption> globalOptionsCache = new HashMap<>();
            for (ParameterOption option : allExistingOptions) {
                if (option.getParameter() != null &&
                        option.getParameter().getNameBg() != null &&
                        option.getNameBg() != null) {

                    String cacheKey = buildOptionCacheKey(
                            option.getParameter().getNameBg(),
                            option.getNameBg()
                    );
                    globalOptionsCache.put(cacheKey, option);
                }
            }

            log.info("Loaded {} existing options from database", globalOptionsCache.size());

            List<Category> tekraCategories = categoryRepository.findAll().stream()
                    .filter(cat -> cat.getTekraSlug() != null)
                    .toList();

            log.info("Processing parameters from {} Tekra categories", tekraCategories.size());

            Map<String, ParameterData> allParametersData = new HashMap<>();
            Set<String> processedSkus = new HashSet<>();

            for (Category category : tekraCategories) {
                try {
                    String categorySlug = category.getTekraSlug();
                    List<Map<String, Object>> products = tekraApiService.getProductsRaw(categorySlug);

                    for (Map<String, Object> product : products) {
                        String sku = getString(product, "sku");
                        if (sku == null || processedSkus.contains(sku)) {
                            continue;
                        }
                        processedSkus.add(sku);

                        Map<String, String> productParams = extractTekraParameters(product);

                        for (Map.Entry<String, String> param : productParams.entrySet()) {
                            String tekraKey = param.getKey();
                            String paramValue = param.getValue();

                            String paramName = convertTekraParameterKeyToName(tekraKey);

                            ParameterData paramData = allParametersData.computeIfAbsent(
                                    tekraKey,
                                    k -> {
                                        ParameterData pd = new ParameterData();
                                        pd.nameBg = paramName;
                                        pd.nameEn = translateParameterName(paramName);
                                        pd.tekraKey = tekraKey;
                                        pd.categories = new HashSet<>();
                                        pd.values = new HashSet<>();
                                        return pd;
                                    }
                            );

                            paramData.categories.add(category);
                            paramData.values.add(paramValue);
                        }
                    }

                } catch (Exception e) {
                    log.error("Error processing category {}: {}", category.getNameBg(), e.getMessage());
                }
            }

            log.info("Collected {} unique parameters across all categories", allParametersData.size());

            long created = 0, reused = 0, optionsCreated = 0;

            for (Map.Entry<String, ParameterData> entry : allParametersData.entrySet()) {
                String tekraKey = entry.getKey();
                ParameterData paramData = entry.getValue();

                try {
                    Parameter parameter = globalParamsCache.get(tekraKey);

                    if (parameter == null) {
                        parameter = new Parameter();
                        parameter.setNameBg(paramData.nameBg);
                        parameter.setNameEn(paramData.nameEn);
                        parameter.setTekraKey(paramData.tekraKey);
                        parameter.setPlatform(Platform.TEKRA);
                        parameter.setOrder(getParameterOrder(paramData.tekraKey));
                        parameter.setCategories(new HashSet<>(paramData.categories));
                        parameter.setCreatedBy("system");

                        parameter = parameterRepository.save(parameter);
                        globalParamsCache.put(paramData.tekraKey, parameter);
                        created++;

                        log.debug("✓ Created parameter: '{}' (tekraKey={}) for {} categories",
                                parameter.getNameBg(), paramData.tekraKey, paramData.categories.size());
                    } else {
                        if (parameter.getTekraKey() == null) {
                            parameter.setTekraKey(paramData.tekraKey);
                        }

                        Set<Category> existingCategories = parameter.getCategories();
                        if (existingCategories == null) {
                            existingCategories = new HashSet<>();
                            parameter.setCategories(existingCategories);
                        }

                        Set<Long> existingCategoryIds = existingCategories.stream()
                                .map(Category::getId)
                                .collect(Collectors.toSet());

                        boolean updated = false;
                        for (Category newCat : paramData.categories) {
                            if (!existingCategoryIds.contains(newCat.getId())) {
                                existingCategories.add(newCat);
                                updated = true;
                            }
                        }

                        if (updated) {
                            parameterRepository.save(parameter);
                        }

                        reused++;
                    }

                    int optionsForThisParam = createOptionsForParameter(
                            parameter,
                            paramData.values,
                            globalOptionsCache
                    );

                    optionsCreated += optionsForThisParam;

                } catch (Exception e) {
                    log.error("Error processing parameter: {}", e.getMessage());
                }
            }

            String message = String.format("Parameters: %d created, %d reused. Options: %d created",
                    created, reused, optionsCreated);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    allParametersData.size(), created, 0, 0, message, startTime);

            log.info("=== Tekra Parameters Sync Completed ===");
            log.info("   Created: {}, Reused: {}, Options: {}", created, reused, optionsCreated);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Tekra parameters sync failed", e);
            throw e;
        }
    }

    private static class ParameterData {
        String nameBg;
        String nameEn;
        String tekraKey;
        Set<Category> categories;
        Set<String> values;
    }

    private int createOptionsForParameter(Parameter parameter,
                                          Set<String> values,
                                          Map<String, ParameterOption> globalOptionsCache) {
        int created = 0;

        for (String value : values) {
            if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
                continue;
            }

            String cacheKey = buildOptionCacheKey(parameter.getNameBg(), value);

            if (globalOptionsCache.containsKey(cacheKey)) {
                continue;
            }

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

    @Transactional
    public void syncTekraProducts() {
        SyncLog syncLog = logHelper.createSyncLogSimple("TEKRA_PRODUCTS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== STARTING Tekra products synchronization - WITH ANALOG/DIGITAL DISCRIMINATION ===");

            fixDuplicateProducts();

            Map<String, Manufacturer> manufacturersMap = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} manufacturers", manufacturersMap.size());

            List<Parameter> allParameters = parameterRepository.findAll().stream()
                    .filter(p -> p.getPlatform() == Platform.TEKRA || p.getTekraKey() != null)
                    .toList();

            Map<String, Parameter> parametersByTekraKey = new HashMap<>();

            for (Parameter p : allParameters) {
                if (p.getTekraKey() != null) {
                    parametersByTekraKey.put(p.getTekraKey(), p);
                }
            }

            log.info("Loaded {} parameters with tekraKey globally", parametersByTekraKey.size());

            Map<Long, Map<String, ParameterOption>> optionsByParameterId = new HashMap<>();

            List<ParameterOption> allOptions = parameterOptionRepository.findAll();
            for (ParameterOption option : allOptions) {
                if (option.getParameter() != null && option.getNameBg() != null) {
                    optionsByParameterId
                            .computeIfAbsent(option.getParameter().getId(), k -> new HashMap<>())
                            .put(normalizeName(option.getNameBg()), option);
                }
            }

            log.info("Loaded {} options globally", allOptions.size());

            List<Category> allCategories = categoryRepository.findAll().stream()
                    .filter(cat -> cat.getTekraSlug() != null && !cat.getTekraSlug().isEmpty())
                    .toList();

            Map<String, Category> categoriesByName = new HashMap<>();
            Map<String, Category> categoriesBySlug = new HashMap<>();
            Map<String, Category> categoriesByTekraSlug = new HashMap<>();

            for (Category cat : allCategories) {
                if (cat.getNameBg() != null) {
                    categoriesByName.put(cat.getNameBg().toLowerCase(), cat);
                }
                if (cat.getSlug() != null) {
                    categoriesBySlug.put(cat.getSlug().toLowerCase(), cat);
                }
                if (cat.getTekraSlug() != null) {
                    categoriesByTekraSlug.put(cat.getTekraSlug().toLowerCase(), cat);
                }
            }

            log.info("Fetching products from all Tekra categories...");
            List<Map<String, Object>> allProducts = new ArrayList<>();
            Set<String> processedSkus = new HashSet<>();

            for (Category category : allCategories) {
                try {
                    String categorySlug = category.getTekraSlug();
                    List<Map<String, Object>> categoryProducts = tekraApiService.getProductsRaw(categorySlug);

                    for (Map<String, Object> product : categoryProducts) {
                        String sku = getString(product, "sku");
                        if (sku != null && !processedSkus.contains(sku)) {
                            allProducts.add(product);
                            processedSkus.add(sku);
                        }
                    }

                } catch (Exception e) {
                    log.error("Error fetching products for category: {}", e.getMessage());
                }
            }

            log.info("Collected {} unique products", allProducts.size());

            if (allProducts.isEmpty()) {
                log.warn("No products found in any category");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No products found", startTime);
                return;
            }

            long totalProcessed = 0, totalCreated = 0, totalUpdated = 0, totalErrors = 0;
            long skippedNoCategory = 0, skippedNoManufacturer = 0;
            long analogCount = 0, digitalCount = 0, undeterminedCount = 0;

            Map<String, Integer> matchTypeStats = new HashMap<>();
            matchTypeStats.put("perfect_path", 0);
            matchTypeStats.put("partial_path", 0);
            matchTypeStats.put("name_match", 0);
            matchTypeStats.put("no_match", 0);

            for (int i = 0; i < allProducts.size(); i++) {
                Map<String, Object> rawProduct = allProducts.get(i);

                try {
                    String sku = getString(rawProduct, "sku");
                    String name = getString(rawProduct, "name");

                    if (sku == null || name == null) {
                        totalErrors++;
                        continue;
                    }

                    Category productCategory = findMostSpecificCategory(rawProduct,
                            categoriesByName, categoriesBySlug, categoriesByTekraSlug, matchTypeStats);

                    if (productCategory == null || !isValidCategory(productCategory)) {
                        log.warn("✗ Skipping product '{}' ({}): NO VALID CATEGORY", name, sku);
                        skippedNoCategory++;
                        matchTypeStats.put("no_match", matchTypeStats.get("no_match") + 1);
                        continue;
                    }

                    // ✅ НОВА ЛОГИКА: Приложи разграничаване аналогов/цифров
                    String productType = determineProductType(rawProduct);
                    if ("IP".equals(productType)) {
                        digitalCount++;
                    } else if ("Analog".equals(productType)) {
                        analogCount++;
                    } else {
                        undeterminedCount++;
                    }

                    productCategory = findCategoryWithTypeDiscrimination(
                            rawProduct,
                            productCategory,
                            categoriesByName
                    );

                    Product product = findOrCreateProduct(sku, rawProduct, productCategory);
                    boolean isNew = (product.getId() == null);

                    boolean success = updateProductFieldsFromTekraXML(product, rawProduct, isNew, manufacturersMap);

                    if (!success) {
                        skippedNoManufacturer++;
                        continue;
                    }

                    product = productRepository.save(product);

                    if (product.getCategory() != null && product.getId() != null) {
                        try {
                            setTekraParametersToProductSimplified(
                                    product,
                                    rawProduct,
                                    parametersByTekraKey,
                                    optionsByParameterId
                            );
                            product = productRepository.save(product);
                        } catch (Exception e) {
                            log.error("ERROR setting parameters for product {}: {}", product.getSku(), e.getMessage());
                        }
                    }

                    if (isNew) {
                        totalCreated++;
                    } else {
                        totalUpdated++;
                    }

                    totalProcessed++;

                    if (totalProcessed % 50 == 0) {
                        log.info("Progress: {}/{} (created: {}, updated: {}, analog: {}, digital: {})",
                                totalProcessed, allProducts.size(), totalCreated, totalUpdated, analogCount, digitalCount);
                    }

                    if (totalProcessed % 100 == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    totalErrors++;
                    log.error("Error processing product {}: {}",
                            getString(rawProduct, "sku"), e.getMessage());

                    if (totalErrors <= 3) {
                        log.error("Full exception for debugging:", e);
                    }
                }
            }

            log.info("=== CATEGORY MATCHING STATISTICS ===");
            matchTypeStats.forEach((type, count) ->
                    log.info("{}: {}", type, count)
            );

            log.info("=== PRODUCT TYPE STATISTICS ===");
            log.info("Digital (IP): {}", digitalCount);
            log.info("Analog: {}", analogCount);
            log.info("Undetermined: {}", undeterminedCount);

            String message = String.format(
                    "Total: %d, Created: %d, Updated: %d, Digital: %d, Analog: %d, Skipped: %d, Errors: %d",
                    totalProcessed, totalCreated, totalUpdated, digitalCount, analogCount,
                    skippedNoCategory + skippedNoManufacturer, totalErrors
            );

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, totalCreated,
                    totalUpdated, totalErrors, message, startTime);

            log.info("=== COMPLETE: Products sync finished ===");

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("=== FAILED: Products synchronization error ===", e);
            throw e;
        }
    }

    private String determineProductType(Map<String, Object> rawProduct) {
        try {
            // ✅ ПЪРВО: Провери името на продукта (НАЙ-НАДЕЖДНО!)
            String productName = getString(rawProduct, "name");
            if (productName != null) {
                String lowerName = productName.toLowerCase();

                // IP детекция в името
                if (lowerName.contains("ip камера") ||
                        lowerName.contains("ip булет") ||
                        lowerName.contains("ip куполна") ||
                        lowerName.contains("ip купол") ||
                        lowerName.contains("nvr")) {
                    return "IP";
                }

                // Analog детекция в името
                if (lowerName.contains("аналогов") ||
                        lowerName.contains("ahd") ||
                        lowerName.contains("tvi") ||
                        lowerName.contains("cvi") ||
                        lowerName.contains("dvr")) {
                    return "Analog";
                }
            }

            // ✅ ВТОРО: Провери category_2
            String category2 = getString(rawProduct, "category_2");
            if (category2 != null) {
                String lower = category2.toLowerCase();
                if (lower.contains("ip систем")) {
                    return "IP";
                }
                if (lower.contains("аналогов")) {
                    return "Analog";
                }
            }

            // ✅ ТРЕТО: Провери параметри
            Map<String, String> params = extractTekraParameters(rawProduct);

            if (params.containsKey("ip_kanali")) {
                String value = params.get("ip_kanali");
                if (value != null && !value.trim().isEmpty() &&
                        !value.equals("0") && !value.equals("-")) {
                    return "IP";
                }
            }

            if (params.containsKey("analogovi_kanali")) {
                String value = params.get("analogovi_kanali");
                if (value != null && !value.trim().isEmpty() &&
                        !value.equals("0") && !value.equals("-")) {
                    return "Analog";
                }
            }

            if (params.containsKey("tehnologiya")) {
                String technology = params.get("tehnologiya").toLowerCase();

                if (technology.contains("ip") ||
                        technology.contains("nvr") ||
                        technology.contains("poe")) {
                    return "IP";
                }

                if (technology.contains("ahd") ||
                        technology.contains("tvi") ||
                        technology.contains("cvi") ||
                        technology.contains("cvbs") ||
                        technology.contains("dvr")) {
                    return "Analog";
                }
            }

            if (params.containsKey("protokol")) {
                String protocol = params.get("protokol").toLowerCase();

                if (protocol.contains("ip") ||
                        protocol.contains("onvif") ||
                        protocol.contains("rtsp")) {
                    return "IP";
                }
            }

            // Допълнителна проверка в името (ако не е хванато по-горе)
            if (productName != null) {
                String lowerName = productName.toLowerCase();

                if (lowerName.contains(" ip ")) {
                    return "IP";
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Error determining product type: {}", e.getMessage());
            return null;
        }
    }

    private Category findCategoryWithTypeDiscrimination(
            Map<String, Object> rawProduct,
            Category baseCategory,
            Map<String, Category> categoriesByName) {

        try {
            String productType = determineProductType(rawProduct);

            if (productType == null) {
                return baseCategory;
            }

            String baseCategoryName = baseCategory.getNameBg();
            String targetCategoryName = null;

            if ("IP".equals(productType)) {
                if (baseCategoryName.contains("Камер")) {
                    targetCategoryName = "IP Камери";
                } else if (baseCategoryName.contains("Рекордер")) {
                    targetCategoryName = "NVR Рекордери";
                }
            } else if ("Analog".equals(productType)) {
                if (baseCategoryName.contains("Камер")) {
                    targetCategoryName = "Аналогови камери";
                } else if (baseCategoryName.contains("Рекордер")) {
                    targetCategoryName = "DVR Рекордери";
                }
            }

            if (targetCategoryName == null) {
                return baseCategory;
            }

            Category targetCategory = categoriesByName.get(targetCategoryName.toLowerCase());

            if (targetCategory != null) {
                log.debug("Product {} remapped from '{}' to '{}' (type: {})",
                        getString(rawProduct, "sku"),
                        baseCategoryName,
                        targetCategoryName,
                        productType);
                return targetCategory;
            } else {
                log.warn("Target category '{}' not found, using base category '{}'",
                        targetCategoryName, baseCategoryName);
                return baseCategory;
            }

        } catch (Exception e) {
            log.error("Error in findCategoryWithTypeDiscrimination: {}", e.getMessage());
            return baseCategory;
        }
    }

    private void setTekraParametersToProductSimplified(
            Product product,
            Map<String, Object> rawProduct,
            Map<String, Parameter> parametersByTekraKey,
            Map<Long, Map<String, ParameterOption>> optionsByParameterId) {

        try {
            if (product.getCategory() == null) {
                return;
            }

            Set<ProductParameter> existingProductParams = product.getProductParameters();
            if (existingProductParams == null) {
                existingProductParams = new HashSet<>();
            }

            Set<ProductParameter> manualParameters = existingProductParams.stream()
                    .filter(pp -> pp.getParameter() != null)
                    .filter(pp -> isManualParameter(pp))
                    .collect(Collectors.toSet());

            Set<ProductParameter> autoParameters = new HashSet<>();
            int mappedCount = 0;
            int notFoundCount = 0;

            Map<String, String> parameterMappings = extractTekraParameters(rawProduct);

            for (Map.Entry<String, String> paramEntry : parameterMappings.entrySet()) {
                try {
                    String parameterKey = paramEntry.getKey();
                    String parameterValue = paramEntry.getValue();

                    if (parameterValue == null || parameterValue.trim().isEmpty() || "-".equals(parameterValue.trim())) {
                        continue;
                    }

                    Parameter parameter = parametersByTekraKey.get(parameterKey);

                    if (parameter == null) {
                        notFoundCount++;
                        log.debug("Parameter with tekraKey='{}' not found for product {}",
                                parameterKey, product.getSku());
                        continue;
                    }

                    Map<String, ParameterOption> parameterOptions = optionsByParameterId.get(parameter.getId());
                    if (parameterOptions == null) {
                        notFoundCount++;
                        continue;
                    }

                    String normalizedValue = normalizeName(parameterValue);
                    ParameterOption option = parameterOptions.get(normalizedValue);

                    if (option == null) {
                        notFoundCount++;
                        continue;
                    }

                    ProductParameter productParam = new ProductParameter();
                    productParam.setProduct(product);
                    productParam.setParameter(parameter);
                    productParam.setParameterOption(option);
                    autoParameters.add(productParam);

                    mappedCount++;

                } catch (Exception e) {
                    notFoundCount++;
                }
            }

            Set<ProductParameter> mergedParameters = new HashSet<>();
            mergedParameters.addAll(manualParameters);
            mergedParameters.addAll(autoParameters);

            product.setProductParameters(mergedParameters);

        } catch (Exception e) {
            log.error("ERROR setting Tekra parameters for product {}: {}",
                    product.getSku(), e.getMessage());
        }
    }

    private boolean isManualParameter(ProductParameter productParameter) {
        Parameter parameter = productParameter.getParameter();
        if (parameter == null) return false;

        boolean isDifferentPlatform = (parameter.getPlatform() == null ||
                parameter.getPlatform() != Platform.TEKRA);

        boolean isCreatedByAdmin = isAdminUser(parameter.getCreatedBy());
        boolean isModifiedByAdmin = isAdminUser(parameter.getLastModifiedBy());

        return isDifferentPlatform || isCreatedByAdmin || isModifiedByAdmin;
    }

    private boolean isAdminUser(String username) {
        if (username == null || username.isEmpty()) return false;
        return "ADMIN".equalsIgnoreCase(username.trim()) || "admin".equalsIgnoreCase(username.trim());
    }

    @Transactional
    private void fixDuplicateProducts() {
        List<Object[]> duplicates = productRepository.findDuplicateProductsBySku();

        if (!duplicates.isEmpty()) {
            for (Object[] duplicate : duplicates) {
                String sku = (String) duplicate[0];

                List<Product> products = productRepository.findProductsBySkuCode(sku);
                if (products.size() > 1) {
                    for (int i = 1; i < products.size(); i++) {
                        productRepository.delete(products.get(i));
                    }
                }
            }
        }
    }

    private Product findOrCreateProduct(String sku, Map<String, Object> rawProduct, Category category) {
        try {
            List<Product> existing = productRepository.findProductsBySkuCode(sku);
            Product product;

            if (!existing.isEmpty()) {
                product = existing.get(0);
                if (existing.size() > 1) {
                    for (int i = 1; i < existing.size(); i++) {
                        productRepository.delete(existing.get(i));
                    }
                }
            } else {
                product = new Product();
                product.setSku(sku);
            }

            product.setCategory(category);

            return product;

        } catch (Exception e) {
            log.error("Error in findOrCreateProduct for SKU {}: {}", sku, e.getMessage());
            throw e;
        }
    }

    private boolean updateProductFieldsFromTekraXML(Product product, Map<String, Object> rawData,
                                                    boolean isNew, Map<String, Manufacturer> manufacturersMap) {
        try {
            if (isNew) {
                product.setReferenceNumber(getString(rawData, "sku"));
                product.setPlatform(Platform.TEKRA);

                String name = getString(rawData, "name");
                product.setNameBg(name);
                product.setNameEn(name);

                product.setModel(getString(rawData, "model"));
                product.setCreatedBy("system");

                String description = getString(rawData, "description");
                if (description != null) {
                    product.setDescriptionBg(description);
                    product.setDescriptionEn(description);
                }

                Double weight = getDoubleValue(rawData, "weight");
                if (weight == null) {
                    weight = getDoubleValue(rawData, "net_weight");
                }
                if (weight != null && weight > 0) {
                    product.setWeight(BigDecimal.valueOf(weight));
                }

                setImagesFromTekraXML(product, rawData);

                String manufacturerName = getString(rawData, "manufacturer");
                if (manufacturerName != null && !manufacturerName.isEmpty()) {
                    String normalizedName = normalizeManufacturerName(manufacturerName);
                    Manufacturer manufacturer = manufacturersMap.get(normalizedName);

                    if (manufacturer != null) {
                        product.setManufacturer(manufacturer);
                    } else {
                        return false;
                    }
                }
            }

            Double price = getDoubleValue(rawData, "price");
            if (price != null) {
                product.setPriceClient(BigDecimal.valueOf(price));
            }

            Double partnerPrice = getDoubleValue(rawData, "partner_price");
            if (partnerPrice != null) {
                product.setPricePartner(BigDecimal.valueOf(partnerPrice));
            }

            Integer quantity = getIntegerValue(rawData, "quantity");
            boolean inStock = (quantity != null && quantity > 0);
            product.setShow(inStock);
            product.setStatus(inStock ? ProductStatus.AVAILABLE : ProductStatus.NOT_AVAILABLE);

            product.calculateFinalPrice();

            return true;

        } catch (Exception e) {
            log.error("Error updating product fields from Tekra XML: {}", e.getMessage());
            throw new RuntimeException("Failed to update product fields", e);
        }
    }

    private void setImagesFromTekraXML(Product product, Map<String, Object> rawData) {
        List<String> allImages = new ArrayList<>();

        String primaryImage = getString(rawData, "image");
        if (primaryImage != null && !primaryImage.isEmpty()) {
            allImages.add(primaryImage);
        }

        Object galleryObj = rawData.get("gallery");
        if (galleryObj instanceof List) {
            List<?> galleryList = (List<?>) galleryObj;
            for (Object imageObj : galleryList) {
                if (imageObj instanceof String) {
                    String imageUrl = (String) imageObj;
                    if (!allImages.contains(imageUrl)) {
                        allImages.add(imageUrl);
                    }
                }
            }
        }

        if (!allImages.isEmpty()) {
            product.setPrimaryImageUrl(allImages.get(0));

            if (allImages.size() > 1) {
                List<String> additionalImages = allImages.subList(1, allImages.size());
                if (product.getAdditionalImages() != null) {
                    product.getAdditionalImages().clear();
                    product.getAdditionalImages().addAll(additionalImages);
                } else {
                    product.setAdditionalImages(new ArrayList<>(additionalImages));
                }
            }
        }
    }

    private Integer getParameterOrder(String parameterKey) {
        Map<String, Integer> orderMap = Map.ofEntries(
                Map.entry("model", 1),
                Map.entry("rezolyutsiya", 2),
                Map.entry("obektiv", 3),
                Map.entry("korpus", 4),
                Map.entry("cvjat", 5),
                Map.entry("razmer", 6),
                Map.entry("stepen_na_zashtita", 7),
                Map.entry("ir_podsvetka", 8),
                Map.entry("zvuk", 9),
                Map.entry("wdr", 10),
                Map.entry("kompresiya", 11),
                Map.entry("poe_portove", 12),
                Map.entry("moshtnost", 13),
                Map.entry("raboten_tok", 14),
                Map.entry("broy_izhodi", 15),
                Map.entry("seriya_eco", 16),
                Map.entry("merna", 99)
        );

        return orderMap.getOrDefault(parameterKey, 50);
    }

    private String translateParameterName(String bulgarianName) {
        Map<String, String> translations = Map.ofEntries(
                Map.entry("Аудио входове/изходи", "Audio In/Out"),
                Map.entry("Брой HDD", "HDD Count"),
                Map.entry("Цвят", "Color"),
                Map.entry("IP канали", "IP Channels"),
                Map.entry("IR подсветка", "IR Illumination"),
                Map.entry("Компресия", "Compression"),
                Map.entry("Корпус", "Body Type"),
                Map.entry("LED подсветка", "LED Illumination"),
                Map.entry("Мощност", "Power"),
                Map.entry("Обектив", "Lens"),
                Map.entry("Оптично увеличение", "Optical Zoom"),
                Map.entry("PoE портове", "PoE Ports"),
                Map.entry("Работен ток", "Operating Current"),
                Map.entry("Размери", "Dimensions"),
                Map.entry("Резолюция", "Resolution"),
                Map.entry("Резолюция на запис", "Recording Resolution"),
                Map.entry("Степен на защита", "Protection Rating"),
                Map.entry("WDR", "WDR"),
                Map.entry("Звук", "Audio"),
                Map.entry("Мерна единица", "Unit"),
                Map.entry("Модел", "Model"),
                Map.entry("Eco серия", "Eco Series"),
                Map.entry("Брой канали", "Channels Count"),
                Map.entry("Брой изходи", "Outputs Count"),
                Map.entry("Технология", "Technology"),
                Map.entry("Капацитет", "Capacity"),
                Map.entry("Подсветка", "Backlight"),
                Map.entry("Протокол", "Protocol"),
                Map.entry("Аналогови канали", "Analog Channels"),
                Map.entry("Алармени входове/изходи", "Alarm In/Out"),
                Map.entry("Серийни портове", "Serial Ports"),
                Map.entry("Термална резолюция", "Thermal Resolution"),
                Map.entry("Тип управление", "Type Of Control")
        );

        return translations.getOrDefault(bulgarianName, bulgarianName);
    }

    private String convertTekraParameterKeyToName(String parameterKey) {
        Map<String, String> parameterTranslations = Map.ofEntries(
                Map.entry("audio_vhodove_izhodi", "Аудио входове/изходи"),
                Map.entry("broy_hdd", "Брой HDD"),
                Map.entry("cvjat", "Цвят"),
                Map.entry("ip_kanali", "IP канали"),
                Map.entry("ir_podsvetka", "IR подсветка"),
                Map.entry("kompresiya", "Компресия"),
                Map.entry("korpus", "Корпус"),
                Map.entry("led_podsvetka", "LED подсветка"),
                Map.entry("moshtnost", "Мощност"),
                Map.entry("obektiv", "Обектив"),
                Map.entry("optichno_uvelichenie", "Оптично увеличение"),
                Map.entry("poe_portove", "PoE портове"),
                Map.entry("raboten_tok", "Работен ток"),
                Map.entry("razmer", "Размери"),
                Map.entry("rezolyutsiya", "Резолюция"),
                Map.entry("rezolyutsiya_na_zapis", "Резолюция на запис"),
                Map.entry("stepen_na_zashtita", "Степен на защита"),
                Map.entry("wdr", "WDR"),
                Map.entry("zvuk", "Звук"),
                Map.entry("tehnologiya", "Технология"),
                Map.entry("kapatsitet", "Капацитет"),
                Map.entry("podsvetka", "Подсветка"),
                Map.entry("protokol", "Протокол"),
                Map.entry("analogovi_kanali", "Аналогови канали"),
                Map.entry("alarmeni_vhodove_izhodi", "Алармени входове/изходи"),
                Map.entry("seriyni_portove", "Серийни портове"),
                Map.entry("termalna_rezolyutsiya", "Термална резолюция"),
                Map.entry("tip_upravlenie", "Тип управление"),
                Map.entry("merna", "Мерна единица"),
                Map.entry("model", "Модел"),
                Map.entry("seriya_eco", "Eco серия"),
                Map.entry("broy_kanali", "Брой канали"),
                Map.entry("broy_izhodi", "Брой изходи"),
                Map.entry("max_ports", "Максимални портове"),
                Map.entry("ports_count", "Брой портове")
        );

        return parameterTranslations.getOrDefault(parameterKey,
                Arrays.stream(parameterKey.split("_"))
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                        .collect(Collectors.joining(" "))
        );
    }

    private Map<String, String> extractTekraParameters(Map<String, Object> rawProduct) {
        Map<String, String> parameters = new HashMap<>();

        Set<String> systemFields = Set.of(
                "id", "sku", "name", "model", "manufacturer",
                "price", "partner_price", "quantity",
                "description", "short_description",
                "weight", "net_weight", "volume",
                "image", "gallery", "files",
                "category_1", "category_2", "category_3",
                "url", "link", "slug",
                "partner_product", "in_stock", "availability",
                "created_at", "updated_at", "status",
                "barcode", "ean", "isbn", "warranty",
                "reference_number", "merna", "outlet", "promo"
        );

        Set<String> knownTekraParameters = Set.of(
                "audio_vhodove_izhodi", "broy_hdd", "cvjat", "ip_kanali",
                "ir_podsvetka", "kompresiya", "korpus", "led_podsvetka",
                "moshtnost", "obektiv", "optichno_uvelichenie", "poe_portove",
                "raboten_tok", "razmer", "rezolyutsiya", "rezolyutsiya_na_zapis",
                "stepen_na_zashtita", "wdr", "zvuk", "seriya_eco",
                "broy_kanali", "broy_izhodi", "max_ports", "ports_count",
                "tehnologiya", "kapatsitet", "podsvetka", "protokol",
                "analogovi_kanali", "alarmeni_vhodove_izhodi", "seriyni_portove",
                "termalna_rezolyutsiya", "tip_upravlenie"
        );

        for (Map.Entry<String, Object> entry : rawProduct.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null || key == null || key.trim().isEmpty()) {
                continue;
            }

            if (systemFields.contains(key)) {
                continue;
            }

            if (value instanceof List || value instanceof Map) {
                continue;
            }

            String stringValue = value.toString().trim();

            if (stringValue.isEmpty() || "null".equalsIgnoreCase(stringValue)) {
                continue;
            }

            if (stringValue.startsWith("http://") || stringValue.startsWith("https://")) {
                continue;
            }

            if (stringValue.length() > 200) {
                continue;
            }

            if (knownTekraParameters.contains(key)) {
                String paramKey = key.startsWith("prop_") ? key.substring(5) : key;
                parameters.put(paramKey, stringValue);
                continue;
            }

            if (key.startsWith("prop_")) {
                String paramKey = key.substring(5);
                parameters.put(paramKey, stringValue);
                continue;
            }

            if (looksLikeParameter(key, stringValue)) {
                parameters.put(key, stringValue);
            }
        }

        return parameters;
    }

    private boolean looksLikeParameter(String key, String value) {
        if (key.contains("http") || value.contains("http")) {
            return false;
        }

        if (key.contains("/") || value.contains("/uploads/")) {
            return false;
        }

        if (key.length() > 50 || key.contains(" ")) {
            return false;
        }

        if (value.matches("^\\d+(\\.\\d+)?$")) {
            return true;
        }

        if (value.length() < 100 && !value.contains("\n")) {
            return true;
        }

        return false;
    }

    private boolean isValidCategory(Category category) {
        return category != null && category.getId() != null &&
                category.getNameBg() != null && !category.getNameBg().trim().isEmpty();
    }

    private Category findMostSpecificCategory(Map<String, Object> product,
                                              Map<String, Category> categoriesByName,
                                              Map<String, Category> categoriesBySlug,
                                              Map<String, Category> categoriesByTekraSlug,
                                              Map<String, Integer> matchTypeStats) {

        final String category3Raw = getString(product, "category_3");
        final String category2Raw = getString(product, "category_2");
        final String category1Raw = getString(product, "category_1");

        final String category3 = "null".equalsIgnoreCase(category3Raw) ? null : category3Raw;
        final String category2 = "null".equalsIgnoreCase(category2Raw) ? null : category2Raw;
        final String category1 = "null".equalsIgnoreCase(category1Raw) ? null : category1Raw;

        String expectedPath = syncHelper.buildCategoryPath(category1, category2, category3);

        if (expectedPath == null) {
            matchTypeStats.put("no_match", matchTypeStats.get("no_match") + 1);
            return null;
        }

        Optional<Category> exactMatch = categoryRepository.findAll().stream()
                .filter(cat -> cat.getCategoryPath() != null)
                .filter(cat -> expectedPath.equalsIgnoreCase(cat.getCategoryPath()))
                .filter(this::isValidCategory)
                .findFirst();

        if (exactMatch.isPresent()) {
            matchTypeStats.put("perfect_path", matchTypeStats.get("perfect_path") + 1);
            return exactMatch.get();
        }

        if (category2 != null) {
            String partialPath = syncHelper.buildCategoryPath(category1, category2, null);

            Optional<Category> partialMatch = categoryRepository.findAll().stream()
                    .filter(cat -> cat.getCategoryPath() != null)
                    .filter(cat -> partialPath.equalsIgnoreCase(cat.getCategoryPath()))
                    .filter(this::isValidCategory)
                    .findFirst();

            if (partialMatch.isPresent()) {
                matchTypeStats.put("partial_path", matchTypeStats.get("partial_path") + 1);
                return partialMatch.get();
            }
        }

        if (category3 != null) {
            String normalizedCat3 = syncHelper.normalizeCategoryForPath(category3);
            Optional<Category> match = categoryRepository.findAll().stream()
                    .filter(cat -> cat.getTekraSlug() != null)
                    .filter(cat -> normalizedCat3.equalsIgnoreCase(cat.getTekraSlug()))
                    .filter(this::isValidCategory)
                    .findFirst();

            if (match.isPresent()) {
                matchTypeStats.put("name_match", matchTypeStats.get("name_match") + 1);
                return match.get();
            }
        }

        matchTypeStats.put("no_match", matchTypeStats.get("no_match") + 1);
        return null;
    }

    private Category createOrUpdateTekraCategory(Map<String, Object> rawData,
                                                 Map<String, Category> existingCategories,
                                                 Category parentCategory) {
        try {
            String tekraId = getString(rawData, "id");
            String tekraSlug = getString(rawData, "slug");
            String name = getString(rawData, "name");

            if (tekraId == null || tekraSlug == null || name == null) {
                return null;
            }

            Optional<Category> existingCategoryOpt = findExistingCategoryByTekraData(
                    tekraId, tekraSlug, name);

            Category category;
            boolean isNew = false;

            if (existingCategoryOpt.isPresent()) {
                category = existingCategoryOpt.get();
            } else {
                category = new Category();
                isNew = true;
            }

            category.setTekraId(tekraId);
            category.setTekraSlug(tekraSlug);

            if (category.getNameBg() == null || isNew) {
                category.setNameBg(name);
            }
            if (category.getNameEn() == null || isNew) {
                category.setNameEn(name);
            }

            if (category.getParent() == null) {
                category.setParent(parentCategory);
            }

            String countStr = getString(rawData, "count");
            if (countStr != null && !countStr.isEmpty()) {
                try {
                    Integer count = Integer.parseInt(countStr);
                    category.setSortOrder(count);
                } catch (NumberFormatException e) {
                    category.setSortOrder(0);
                }
            } else {
                category.setSortOrder(0);
            }

            String uniqueSlug;
            if (category.getSlug() != null && !isNew) {
                uniqueSlug = category.getSlug();
            } else {
                uniqueSlug = generateUniqueSlug(tekraSlug, name, parentCategory, existingCategories);
            }
            category.setSlug(uniqueSlug);

            category.setCategoryPath(category.generateCategoryPath());
            category.setPlatform(Platform.TEKRA);

            category = categoryRepository.save(category);
            categoryRepository.flush();

            return category;

        } catch (Exception e) {
            log.error("Error creating/updating category: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create/update category", e);
        }
    }

    private Manufacturer createTekraManufacturer(String manufacturerName) {
        try {
            Manufacturer manufacturer = new Manufacturer();
            manufacturer.setName(manufacturerName);
            manufacturer.setInformationName(manufacturerName);
            manufacturer.setPlatform(Platform.TEKRA);
            return manufacturer;
        } catch (Exception e) {
            log.error("Error creating manufacturer: {}", e.getMessage());
            return null;
        }
    }

    private Optional<Category> findExistingCategoryByTekraData(String tekraId,
                                                               String tekraSlug,
                                                               String categoryName) {
        if (tekraId != null) {
            List<Category> byTekraId = categoryRepository.findAll().stream()
                    .filter(cat -> tekraId.equals(cat.getTekraId()))
                    .toList();

            if (!byTekraId.isEmpty()) {
                return Optional.of(byTekraId.get(0));
            }
        }

        if (tekraSlug != null) {
            List<Category> byTekraSlug = categoryRepository.findAll().stream()
                    .filter(cat -> tekraSlug.equals(cat.getTekraSlug()))
                    .toList();

            if (!byTekraSlug.isEmpty()) {
                return Optional.of(byTekraSlug.get(0));
            }
        }

        if (categoryName != null && !categoryName.trim().isEmpty()) {
            String normalizedName = normalizeCategoryName(categoryName);

            List<Category> allCategories = categoryRepository.findAll();

            for (Category cat : allCategories) {
                String catNameBg = normalizeCategoryName(cat.getNameBg());
                String catNameEn = normalizeCategoryName(cat.getNameEn());

                if (normalizedName.equals(catNameBg) || normalizedName.equals(catNameEn)) {
                    return Optional.of(cat);
                }
            }
        }

        return Optional.empty();
    }

    private String generateUniqueSlug(String tekraSlug, String categoryName,
                                      Category parentCategory,
                                      Map<String, Category> existingCategories) {
        if (tekraSlug == null || tekraSlug.isEmpty()) {
            tekraSlug = syncHelper.createSlugFromName(categoryName);
        }

        String baseSlug = tekraSlug;

        if (parentCategory == null) {
            if (!syncHelper.slugExistsInMap(baseSlug, existingCategories) &&
                    !syncHelper.slugExistsInDatabase(baseSlug, null)) {
                return baseSlug;
            }
            return baseSlug + "-root";
        }

        String parentSlug = parentCategory.getSlug();
        if (parentSlug == null || parentSlug.isEmpty()) {
            parentSlug = parentCategory.getTekraSlug();
            if (parentSlug == null) {
                parentSlug = "cat-" + parentCategory.getId();
            }
        }

        String hierarchicalSlug = parentSlug + "-" + baseSlug;

        if (!syncHelper.slugExistsInMap(hierarchicalSlug, existingCategories) &&
                !syncHelper.slugExistsInDatabase(hierarchicalSlug, parentCategory)) {
            return hierarchicalSlug;
        }

        int counter = 2;
        String numberedSlug;
        do {
            numberedSlug = hierarchicalSlug + "-" + counter;
            counter++;
        } while ((syncHelper.slugExistsInMap(numberedSlug, existingCategories) ||
                syncHelper.slugExistsInDatabase(numberedSlug, parentCategory)) && counter < 100);

        return numberedSlug;
    }

    private String normalizeCategoryName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("[^a-zа-я0-9]+", "").replaceAll("\\s+", "");
    }

    private String normalizeManufacturerName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ").replaceAll("[^a-zа-я0-9\\s]+", "");
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String buildOptionCacheKey(String parameterName, String optionName) {
        return normalizeName(parameterName) + ":::" + normalizeName(optionName);
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}