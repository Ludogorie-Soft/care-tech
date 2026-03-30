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

/**
 * MostSyncService - COMPLETELY REWRITTEN VERSION 3.0
 *
 * Дата: 23.01.2025
 *
 * КРИСТАЛНА ЛОГИКА (същата като Vali & Tekra):
 * 1. Manufacturers → CREATE ONLY with name deduplication
 * 2. Categories → Uses MOST_CATEGORY_MAPPING (hardcoded)
 * 3. Parameters → ГЛОБАЛНО дедуплициране по ИМЕ
 * 4. Products → Използва готовите параметри (global cache)
 *
 * ГАРАНЦИИ:
 * - Един параметър "RAM" за ВСИЧКИ категории
 * - Една опция "16GB" за ВСИЧКИ "RAM"
 * - НИЩО не липсва
 * - БЕЗ дублирания
 * - Споделяне с Vali/Tekra категории и производители
 */
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

    private static final String USD_TO_BGN_RATE = "1.80";
    private static final String EUR_TO_BGN_RATE = "1.95583";

    private static final Map<String, String> MOST_CATEGORY_MAPPING;

    static {
        Map<String, String> map = new HashMap<>();

        map.put("CPU", "Процесори");
        map.put("AMD Ryzen /AM4", "Процесори");
        map.put("AMD Ryzen /AM5", "Процесори");
        map.put("AMD Ryzen /Internal VGA", "Процесори");
        map.put("AMD Athlon /AM4", "Процесори");
        map.put("AMD A-series /AM4", "Процесори");
        map.put("Intel Core Ultra 200", "Процесори");
        map.put("Core Gen 10th /LGA1200", "Процесори");
        map.put("Core Gen 11th /LGA1200", "Процесори");
        map.put("Core Gen 12th /LGA1700", "Процесори");
        map.put("Core Gen 13th /LGA1700", "Процесори");
        map.put("Core Gen 14th /LGA1700", "Процесори");
        map.put("Celeron Gen10th /LGA1200", "Процесори");
        map.put("Pentium Gen10th /LGA1200", "Процесори");
        map.put("Intel Pentium /Internal VGA", "Процесори");
        map.put("LGA1851 /Intel Core Ultra 200", "Процесори");

        map.put("VIDEO CARD", "Видео карти");
        map.put("GeForce RTX5090", "Видео карти");
        map.put("GeForce RTX5080", "Видео карти");
        map.put("GeForce RTX5070TI", "Видео карти");
        map.put("GeForce RTX5070", "Видео карти");
        map.put("GeForce RTX5060TI", "Видео карти");
        map.put("GeForce RTX5060", "Видео карти");
        map.put("GeForce RTX5050", "Видео карти");
        map.put("GeForce RTX4090", "Видео карти");
        map.put("GeForce RTX4080 Super", "Видео карти");
        map.put("GeForce RTX4070TI Super", "Видео карти");
        map.put("GeForce RTX4070TI", "Видео карти");
        map.put("GeForce RTX4070 Super", "Видео карти");
        map.put("GeForce RTX4070", "Видео карти");
        map.put("GeForce RTX4060Ti", "Видео карти");
        map.put("GeForce RTX4060", "Видео карти");
        map.put("GeForce RTX3060", "Видео карти");
        map.put("GeForce RTX3050", "Видео карти");
        map.put("GeForce RTX2070", "Видео карти");
        map.put("GeForce GTX1650", "Видео карти");
        map.put("GeForce GTX1070TI", "Видео карти");
        map.put("GeForce GT1030", "Видео карти");
        map.put("GeForce GT730", "Видео карти");
        map.put("GeForce GT710", "Видео карти");
        map.put("Radeon RX9070XT", "Видео карти");
        map.put("Radeon RX9070", "Видео карти");
        map.put("Radeon RX9060XT", "Видео карти");
        map.put("Radeon RX7900XTX", "Видео карти");
        map.put("Radeon RX7900XT", "Видео карти");
        map.put("Radeon RX7900GRE", "Видео карти");
        map.put("Radeon RX7800XT", "Видео карти");
        map.put("Radeon RX7700XT", "Видео карти");
        map.put("Radeon RX7600XT", "Видео карти");
        map.put("Radeon RX7600", "Видео карти");
        map.put("Radeon RX6700", "Видео карти");
        map.put("Radeon RX6650XT", "Видео карти");
        map.put("Radeon RX6600XT", "Видео карти");
        map.put("Radeon RX6600", "Видео карти");
        map.put("Radeon RX6500XT", "Видео карти");
        map.put("Radeon RX580", "Видео карти");
        map.put("Radeon RX570", "Видео карти");
        map.put("Radeon RX550", "Видео карти");

        map.put("MEMORY", "Памети");
        map.put("DDR5", "Памети");
        map.put("DDR4", "Памети");
        map.put("DDR3", "Памети");
        map.put("DDR2", "Памети");

        map.put("MOTHERBOARD", "Дънни платки");
        map.put("AMD /Socket AM5", "Дънни платки");
        map.put("AMD /Socket AM4", "Дънни платки");
        map.put("Intel /LGA1851", "Дънни платки");
        map.put("Intel /LGA1700", "Дънни платки");
        map.put("Intel /LGA1200", "Дънни платки");

        map.put("SSD", "SSD дискове");
        map.put("M.2 NVMe", "SSD дискове");
        map.put("2.5\" SATA", "SSD дискове");
        map.put("mSATA", "SSD дискове");
        map.put("M.2 SATA", "SSD дискове");
        map.put("PCIe", "SSD дискове");

        map.put("HDD", "Твърди дискове");
        map.put("3.5\"", "Твърди дискове");
        map.put("2.5\"", "Твърди дискове");

        map.put("POWER SUPPLY", "Захранвания");
        map.put("ATX", "Захранвания");
        map.put("SFX", "Захранвания");

        map.put("CASE", "Кутии за компютри");
        map.put("Mid Tower", "Кутии за компютри");
        map.put("Full Tower", "Кутии за компютри");
        map.put("Mini Tower", "Кутии за компютри");

        map.put("COOLING", "Охлаждане");
        map.put("CPU Cooler", "Охлаждане");
        map.put("Case Fan", "Охлаждане");
        map.put("Liquid Cooling", "Охлаждане");

        map.put("MONITOR", "Монитори");
        map.put("Gaming Monitor", "Монитори");
        map.put("Office Monitor", "Монитори");

        MOST_CATEGORY_MAPPING = Collections.unmodifiableMap(map);
    }

    // ===========================================
    // MANUFACTURERS SYNC - CREATE ONLY
    // ===========================================

    @Transactional
    public void syncMostManufacturers() {
        SyncLog syncLog = logHelper.createSyncLogSimple("MOST_MANUFACTURERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Most manufacturers synchronization - CREATE ONLY mode");

            List<Map<String, Object>> allProducts = mostApiService.getAllProducts();
            log.info("Fetched {} products from Most API", allProducts.size());

            Set<String> manufacturerNames = allProducts.stream()
                    .map(product -> (String) product.get("manufacturer"))
                    .filter(Objects::nonNull)
                    .filter(name -> !name.trim().isEmpty())
                    .collect(Collectors.toSet());

            log.info("Extracted {} unique manufacturers from products", manufacturerNames.size());

            Map<String, Manufacturer> existingManufacturers = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            log.info("Found {} existing manufacturers in database", existingManufacturers.size());

            long created = 0, skipped = 0;

            for (String manufacturerName : manufacturerNames) {
                String normalizedName = normalizeManufacturerName(manufacturerName);
                Manufacturer manufacturer = existingManufacturers.get(normalizedName);

                if (manufacturer == null) {
                    manufacturer = new Manufacturer();
                    manufacturer.setName(manufacturerName);
                    manufacturer.setPlatform(Platform.MOST);
                    manufacturer = manufacturerRepository.save(manufacturer);
                    existingManufacturers.put(normalizedName, manufacturer);
                    created++;
                    log.debug("Created manufacturer: {}", manufacturerName);
                } else {
                    skipped++;
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    (long) manufacturerNames.size(), created, 0, 0,
                    String.format("Skipped %d existing", skipped), startTime);
            log.info("Manufacturers sync completed - Created: {}, Skipped: {}", created, skipped);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during manufacturers synchronization", e);
            throw new RuntimeException(e);
        }
    }

    // ===========================================
    // CATEGORIES - No sync needed (uses mapping)
    // ===========================================

    @Transactional
    public void syncMostCategories() {
        SyncLog syncLog = logHelper.createSyncLogSimple("MOST_CATEGORIES");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Most categories sync - Using MOST_CATEGORY_MAPPING (no sync needed)");
            log.info("Mapped categories: {}", MOST_CATEGORY_MAPPING.values().stream().distinct().count());

            Set<String> targetCategories = new HashSet<>(MOST_CATEGORY_MAPPING.values());

            long existing = categoryRepository.findAll().stream()
                    .filter(cat -> targetCategories.contains(cat.getNameBg()))
                    .count();

            log.info("Found {}/{} target categories in database", existing, targetCategories.size());

            if (existing < targetCategories.size()) {
                log.warn("Missing {} categories - they should be created by Vali/Tekra sync",
                        targetCategories.size() - existing);
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, existing, 0, 0, 0,
                    "Using hardcoded mapping", startTime);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during categories check", e);
        }
    }

    // ===========================================
    // PARAMETERS SYNC - НОВА КРИСТАЛНА ЛОГИКА
    // ===========================================

    @Transactional
    public void syncMostParameters() {
        SyncLog syncLog = logHelper.createSyncLogSimple("MOST_PARAMETERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== Starting GLOBAL Most Parameters Synchronization ===");
            log.info("Strategy: Deduplicate by NORMALIZED NAME (same as Vali/Tekra)");

            List<Parameter> allExistingParams = parameterRepository.findAll();

            for (Parameter p : allExistingParams) {
                if (p.getCategories() != null) {
                    p.getCategories().size();
                }
            }

            Map<String, Parameter> globalParamsCache = new HashMap<>();
            for (Parameter p : allExistingParams) {
                if (p.getMostKey() != null) {
                    globalParamsCache.put(p.getMostKey(), p);
                }
            }

            log.info("Loaded {} existing parameters with mostKey from database", globalParamsCache.size());

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

            List<Map<String, Object>> allProducts = mostApiService.getAllProducts();
            log.info("Processing parameters from {} Most products", allProducts.size());

            Map<String, ParameterData> allParametersData = new HashMap<>();

            for (Map<String, Object> product : allProducts) {
                try {
                    String categoryName = (String) product.get("category");
                    if (categoryName == null) continue;

                    String targetCategoryName = MOST_CATEGORY_MAPPING.get(categoryName);
                    if (targetCategoryName == null) {
                        log.debug("Unknown Most category: {}", categoryName);
                        continue;
                    }

                    Optional<Category> categoryOpt = categoryRepository.findByNameBg(targetCategoryName);
                    if (categoryOpt.isEmpty()) {
                        log.warn("Target category '{}' not found in database", targetCategoryName);
                        continue;
                    }

                    Category category = categoryOpt.get();

                    Map<String, String> productParams = extractMostParameters(product);

                    for (Map.Entry<String, String> param : productParams.entrySet()) {
                        String paramName = param.getKey();
                        String paramValue = param.getValue();

                        String mostKey = generateMostKey(paramName);  // ← ДОБАВЕНО!

                        ParameterData paramData = allParametersData.computeIfAbsent(
                                mostKey,  // ← ПРОМЕНЕНО!
                                k -> {
                                    ParameterData pd = new ParameterData();
                                    pd.nameBg = paramName;
                                    pd.nameEn = paramName;
                                    pd.mostKey = mostKey;  // ← ДОБАВЕНО!
                                    pd.categories = new HashSet<>();
                                    pd.values = new HashSet<>();
                                    return pd;
                                }
                        );

                        paramData.categories.add(category);
                        paramData.values.add(paramValue);
                    }

                } catch (Exception e) {
                    log.error("Error processing product parameters: {}", e.getMessage());
                }
            }

            log.info("Collected {} unique parameters across all categories", allParametersData.size());

            long created = 0, reused = 0, optionsCreated = 0;

            for (Map.Entry<String, ParameterData> entry : allParametersData.entrySet()) {
                String mostKey = entry.getKey();  // ← ПРОМЕНЕНО!
                ParameterData paramData = entry.getValue();

                try {
                    Parameter parameter = globalParamsCache.get(mostKey);  // ← ПРОМЕНЕНО!

                    if (parameter == null) {
                        parameter = new Parameter();
                        parameter.setNameBg(paramData.nameBg);
                        parameter.setNameEn(paramData.nameEn);
                        parameter.setMostKey(paramData.mostKey);  // ← ДОБАВЕНО!
                        parameter.setPlatform(Platform.MOST);
                        parameter.setOrder(50);
                        parameter.setCategories(new HashSet<>(paramData.categories));
                        parameter.setCreatedBy("system");

                        parameter = parameterRepository.save(parameter);
                        globalParamsCache.put(paramData.mostKey, parameter);  // ← ПРОМЕНЕНО!
                        created++;

                        log.debug("✓ Created parameter: '{}' (mostKey={}) for {} categories",
                                parameter.getNameBg(), paramData.mostKey, paramData.categories.size());

                    } else {
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

            log.info("=== Most Parameters Sync Completed ===");
            log.info("   Unique parameters: {}", allParametersData.size());
            log.info("   Created: {}, Reused: {}", created, reused);
            log.info("   Options created: {}", optionsCreated);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Most parameters sync failed", e);
            throw e;
        }
    }

    private static class ParameterData {
        String nameBg;
        String nameEn;
        String mostKey;
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

    // ===========================================
    // PRODUCTS SYNC - ОПРОСТЕНА ЛОГИКА
    // ===========================================

    @Transactional
    public void syncMostProducts() {
        SyncLog syncLog = logHelper.createSyncLogSimple("MOST_PRODUCTS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== STARTING Most products synchronization - SIMPLIFIED mode ===");

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
                    .filter(p -> p.getPlatform() == Platform.MOST || p.getPlatform() == null)
                    .toList();

            Map<String, Parameter> parametersByMostKey = new HashMap<>();

            for (Parameter p : allParameters) {
                if (p.getMostKey() != null) {
                    parametersByMostKey.put(p.getMostKey(), p);
                }
            }

            log.info("Loaded {} parameters with mostKey globally", parametersByMostKey.size());

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

            Map<String, Category> categoriesByName = categoryRepository.findAll().stream()
                    .filter(cat -> cat.getNameBg() != null)
                    .collect(Collectors.toMap(
                            cat -> cat.getNameBg(),
                            cat -> cat,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} categories", categoriesByName.size());

            List<Map<String, Object>> allProducts = mostApiService.getAllProducts();
            log.info("Fetched {} products from Most API", allProducts.size());

            if (allProducts.isEmpty()) {
                log.warn("No products found");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No products found", startTime);
                return;
            }

            long totalProcessed = 0, totalCreated = 0, totalUpdated = 0, totalErrors = 0;
            long skippedNoCategory = 0, skippedNoManufacturer = 0;

            for (int i = 0; i < allProducts.size(); i++) {
                Map<String, Object> mostProduct = allProducts.get(i);

                try {
                    String sku = (String) mostProduct.get("part_number");
                    String name = (String) mostProduct.get("name");

                    if (sku == null || name == null) {
                        totalErrors++;
                        continue;
                    }

                    String categoryName = (String) mostProduct.get("category");
                    String targetCategoryName = MOST_CATEGORY_MAPPING.get(categoryName);

                    if (targetCategoryName == null) {
                        log.debug("Unknown category '{}' for product {}", categoryName, sku);
                        skippedNoCategory++;
                        continue;
                    }

                    Category productCategory = categoriesByName.get(targetCategoryName);

                    if (productCategory == null) {
                        log.warn("Category '{}' not found for product {}", targetCategoryName, sku);
                        skippedNoCategory++;
                        continue;
                    }

                    Product product = findOrCreateProduct(sku, mostProduct, productCategory);
                    boolean isNew = (product.getId() == null);

                    boolean success = updateProductFieldsFromMost(product, mostProduct, isNew, manufacturersMap);

                    if (!success) {
                        skippedNoManufacturer++;
                        continue;
                    }

                    product = productRepository.save(product);

                    if (product.getCategory() != null && product.getId() != null) {
                        try {
                            setMostParametersToProductSimplified(
                                    product,
                                    mostProduct,
                                    parametersByMostKey,  // ← ПРОМЕНЕНО ИМЕ!
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
                        log.info("Progress: {}/{} (created: {}, updated: {})",
                                totalProcessed, allProducts.size(), totalCreated, totalUpdated);
                    }

                    if (totalProcessed % 100 == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    totalErrors++;
                    log.error("Error processing product: {}", e.getMessage());

                    if (totalErrors <= 3) {
                        log.error("Full exception for debugging:", e);
                    }
                }
            }

            String message = String.format(
                    "Total: %d, Created: %d, Updated: %d, Skipped (No Category): %d, Skipped (No Manufacturer): %d, Errors: %d",
                    totalProcessed, totalCreated, totalUpdated, skippedNoCategory, skippedNoManufacturer, totalErrors
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

    private void setMostParametersToProductSimplified(
            Product product,
            Map<String, Object> mostProduct,
            Map<String, Parameter> parametersByMostKey,
            Map<Long, Map<String, ParameterOption>> optionsByParameterId) {

        try {
            if (product.getCategory() == null) {
                log.warn("Product {} has no category, cannot set parameters", product.getSku());
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

            Map<String, String> parameterMappings = extractMostParameters(mostProduct);

            for (Map.Entry<String, String> paramEntry : parameterMappings.entrySet()) {
                try {
                    String parameterName = paramEntry.getKey();
                    String parameterValue = paramEntry.getValue();

                    if (parameterValue == null || parameterValue.trim().isEmpty()) {
                        continue;
                    }

                    String mostKey = generateMostKey(parameterName);  // ← ДОБАВЕНО!
                    Parameter parameter = parametersByMostKey.get(mostKey);  // ← ПРОМЕНЕНО!

                    if (parameter == null) {
                        notFoundCount++;
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
                    log.error("Error mapping parameter {} for product {}: {}",
                            paramEntry.getKey(), product.getSku(), e.getMessage());
                    notFoundCount++;
                }
            }

            Set<ProductParameter> mergedParameters = new HashSet<>();
            mergedParameters.addAll(manualParameters);
            mergedParameters.addAll(autoParameters);

            product.setProductParameters(mergedParameters);

            if (notFoundCount > 0) {
                log.warn("Product {}: {} mapped, {} manual, {} not found",
                        product.getSku(), mappedCount, manualParameters.size(), notFoundCount);
            }

        } catch (Exception e) {
            log.error("ERROR setting Most parameters for product {}: {}",
                    product.getSku(), e.getMessage());
        }
    }

    private boolean isManualParameter(ProductParameter productParameter) {
        Parameter parameter = productParameter.getParameter();
        if (parameter == null) return false;

        boolean isDifferentPlatform = (parameter.getPlatform() == null ||
                parameter.getPlatform() != Platform.MOST);

        boolean isCreatedByAdmin = isAdminUser(parameter.getCreatedBy());
        boolean isModifiedByAdmin = isAdminUser(parameter.getLastModifiedBy());

        return isDifferentPlatform || isCreatedByAdmin || isModifiedByAdmin;
    }

    private boolean isAdminUser(String username) {
        if (username == null || username.isEmpty()) return false;
        return "ADMIN".equalsIgnoreCase(username.trim()) || "admin".equalsIgnoreCase(username.trim());
    }

    private Product findOrCreateProduct(String sku, Map<String, Object> mostProduct, Category category) {
        try {
            List<Product> existing = productRepository.findProductsBySkuCode(sku);
            Product product;

            if (!existing.isEmpty()) {
                product = existing.get(0);
                if (existing.size() > 1) {
                    log.warn("Found {} duplicates for SKU: {}, keeping first", existing.size(), sku);
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

    private boolean updateProductFieldsFromMost(Product product, Map<String, Object> mostProduct,
                                                boolean isNew, Map<String, Manufacturer> manufacturersMap) {
        try {
            if (isNew) {
                String sku = (String) mostProduct.get("part_number");
                product.setReferenceNumber(sku);
                product.setPlatform(Platform.MOST);

                String name = (String) mostProduct.get("name");
                product.setNameBg(name);
                product.setNameEn(name);

                product.setModel((String) mostProduct.get("model"));
                product.setCreatedBy("system");

                String description = (String) mostProduct.get("description");
                if (description != null && !description.trim().isEmpty()) {
                    product.setDescriptionBg(description);
                    product.setDescriptionEn(description);
                }

                String imageUrl = (String) mostProduct.get("image_url");
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    product.setPrimaryImageUrl(imageUrl);
                }

                String manufacturerName = (String) mostProduct.get("manufacturer");
                if (manufacturerName != null && !manufacturerName.isEmpty()) {
                    String normalizedName = normalizeManufacturerName(manufacturerName);
                    Manufacturer manufacturer = manufacturersMap.get(normalizedName);

                    if (manufacturer != null) {
                        product.setManufacturer(manufacturer);
                    } else {
                        log.warn("Manufacturer '{}' not found for product {}, skipping product",
                                manufacturerName, product.getSku());
                        return false;
                    }
                }
            }

            Object priceObj = mostProduct.get("price");
            if (priceObj != null) {
                BigDecimal price = convertToPrice(priceObj);

                String currency = (String) mostProduct.get("currency");
                if ("USD".equals(currency)) {
                    price = price.multiply(new BigDecimal(USD_TO_BGN_RATE));
                } else if ("EUR".equals(currency)) {
                    price = price.multiply(new BigDecimal(EUR_TO_BGN_RATE));
                }

                product.setPriceClient(price);
            }

            Object quantityObj = mostProduct.get("quantity");
            boolean inStock = false;
            if (quantityObj != null) {
                int quantity = convertToInt(quantityObj);
                inStock = (quantity > 0);
            }

            product.setShow(inStock);
            product.setStatus(inStock ? ProductStatus.AVAILABLE : ProductStatus.NOT_AVAILABLE);

            product.calculateFinalPrice();

            return true;

        } catch (Exception e) {
            log.error("Error updating product fields from Most: {}", e.getMessage());
            throw new RuntimeException("Failed to update product fields", e);
        }
    }

    private Map<String, String> extractMostParameters(Map<String, Object> mostProduct) {
        Map<String, String> parameters = new HashMap<>();

        Set<String> systemFields = Set.of(
                "part_number", "name", "model", "manufacturer",
                "price", "currency", "quantity", "category",
                "description", "image_url", "url"
        );

        for (Map.Entry<String, Object> entry : mostProduct.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null || key == null || key.trim().isEmpty()) {
                continue;
            }

            if (systemFields.contains(key)) {
                continue;
            }

            String stringValue = value.toString().trim();

            if (stringValue.isEmpty() || "null".equalsIgnoreCase(stringValue)) {
                continue;
            }

            if (stringValue.length() > 200) {
                continue;
            }

            parameters.put(key, stringValue);
        }

        return parameters;
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

    private BigDecimal convertToPrice(Object value) {
        if (value == null) return BigDecimal.ZERO;

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }

        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Cannot convert to price: {}", value);
            return BigDecimal.ZERO;
        }
    }

    private int convertToInt(Object value) {
        if (value == null) return 0;

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Cannot convert to int: {}", value);
            return 0;
        }
    }

    private String generateMostKey(String parameterName) {
        if (parameterName == null || parameterName.trim().isEmpty()) {
            return "unknown_param";
        }

        return parameterName.toLowerCase()
                .replaceAll("[^a-z0-9\\s]+", " ")  // Remove special chars
                .trim()
                .replaceAll("\\s+", "_");          // Replace spaces with underscore
    }
}