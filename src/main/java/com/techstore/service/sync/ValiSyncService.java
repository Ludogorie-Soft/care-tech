//package com.techstore.service.sync;
//
//import com.techstore.dto.external.ImageDto;
//import com.techstore.dto.request.CategoryRequestFromExternalDto;
//import com.techstore.dto.request.ManufacturerRequestDto;
//import com.techstore.dto.request.ParameterOptionRequestDto;
//import com.techstore.dto.request.ParameterRequestDto;
//import com.techstore.dto.request.ParameterValueRequestDto;
//import com.techstore.dto.request.ProductRequestDto;
//import com.techstore.entity.Category;
//import com.techstore.entity.Manufacturer;
//import com.techstore.entity.Parameter;
//import com.techstore.entity.ParameterOption;
//import com.techstore.entity.Product;
//import com.techstore.entity.ProductParameter;
//import com.techstore.entity.SyncLog;
//import com.techstore.enums.Platform;
//import com.techstore.enums.ProductStatus;
//import com.techstore.repository.CategoryRepository;
//import com.techstore.repository.ManufacturerRepository;
//import com.techstore.repository.ParameterOptionRepository;
//import com.techstore.repository.ParameterRepository;
//import com.techstore.repository.ProductRepository;
//import com.techstore.service.ValiApiService;
//import com.techstore.util.LogHelper;
//import com.techstore.util.SyncHelper;
//import jakarta.persistence.EntityManager;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//import static com.techstore.util.LogHelper.LOG_STATUS_FAILED;
//import static com.techstore.util.LogHelper.LOG_STATUS_SUCCESS;
//
///**
// * ValiSyncService - FINAL PRODUCTION VERSION
// *
// * Версия: 2.1 - Адаптирана към конкретните Repository методи
// * Дата: 24.01.2025
// *
// * Структура на базата:
// * - Parameters <--ManyToMany--> Categories (category_parameters таблица)
// * - Parameter --OneToMany--> ParameterOptions
// * - Product --ManyToMany--> Parameters (през ProductParameter)
// *
// * Основни функции:
// * - CREATE ONLY режим за manufacturers, categories, parameters
// * - ГЛОБАЛНО дедуплициране на параметри (един "RAM" за всички категории)
// * - ГЛОБАЛНО дедуплициране на опции (една "16GB" за всички "RAM")
// * - MINIMAL UPDATE за products (само цени и статус)
// * - Запазване на ръчно добавени параметри
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ValiSyncService {
//
//    private final ValiApiService valiApiService;
//    private final CategoryRepository categoryRepository;
//    private final ManufacturerRepository manufacturerRepository;
//    private final ProductRepository productRepository;
//    private final ParameterRepository parameterRepository;
//    private final ParameterOptionRepository parameterOptionRepository;
//    private final EntityManager entityManager;
//    private final SyncHelper syncHelper;
//    private final LogHelper logHelper;
//
//    @Value("#{'${excluded.categories.external-ids}'.split(',')}")
//    private Set<Long> excludedCategories;
//
//    @Value("${app.sync.batch-size:30}")
//    private int batchSize;
//
//    // ===========================================
//    // MANUFACTURERS SYNC - CREATE ONLY
//    // ===========================================
//
//    @Transactional
//    public void syncManufacturers() {
//        String syncType = "VALI_MANUFACTURERS";
//        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
//        long startTime = System.currentTimeMillis();
//
//        try {
//            log.info("Starting manufacturers synchronization - CREATE ONLY mode");
//
//            List<ManufacturerRequestDto> externalManufacturers = valiApiService.getManufacturers();
//            log.info("Fetched {} manufacturers from Vali API", externalManufacturers.size());
//
//            Map<String, Manufacturer> existingManufacturers = manufacturerRepository.findAll()
//                    .stream()
//                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
//                    .collect(Collectors.toMap(
//                            m -> normalizeManufacturerName(m.getName()),
//                            m -> m,
//                            (existing, duplicate) -> {
//                                log.warn("Duplicate manufacturer: {}, IDs: {} and {}, keeping first",
//                                        existing.getName(), existing.getId(), duplicate.getId());
//                                return existing;
//                            }
//                    ));
//
//            log.info("Found {} existing manufacturers in database", existingManufacturers.size());
//
//            long created = 0, skipped = 0;
//
//            for (ManufacturerRequestDto extManufacturer : externalManufacturers) {
//                String normalizedName = normalizeManufacturerName(extManufacturer.getName());
//                Manufacturer manufacturer = existingManufacturers.get(normalizedName);
//
//                if (manufacturer == null) {
//                    manufacturer = createManufacturerFromExternal(extManufacturer);
//                    manufacturer = manufacturerRepository.save(manufacturer);
//                    existingManufacturers.put(normalizedName, manufacturer);
//                    created++;
//                    log.debug("Created manufacturer: {} (externalId: {})",
//                            extManufacturer.getName(), extManufacturer.getId());
//                } else {
//                    skipped++;
//                    log.trace("Manufacturer already exists, skipping: {}", extManufacturer.getName());
//                }
//            }
//
//            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
//                    (long) externalManufacturers.size(), created, 0, 0,
//                    String.format("Skipped %d existing", skipped), startTime);
//            log.info("Manufacturers sync completed - Created: {}, Skipped: {}", created, skipped);
//
//        } catch (Exception e) {
//            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
//            log.error("Error during manufacturers synchronization", e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    private String normalizeManufacturerName(String name) {
//        if (name == null) return "";
//        return name.toLowerCase().trim().replaceAll("\\s+", " ");
//    }
//
//    // ===========================================
//    // CATEGORIES SYNC - CREATE ONLY
//    // ===========================================
//
//    @Transactional
//    public void syncCategories() {
//        String syncType = "VALI_CATEGORIES";
//        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
//        long startTime = System.currentTimeMillis();
//
//        try {
//            log.info("Starting categories synchronization - CREATE ONLY mode");
//
//            List<CategoryRequestFromExternalDto> externalCategories = valiApiService.getCategories();
//            log.info("Fetched {} categories from Vali API", externalCategories.size());
//
//            entityManager.clear();
//
//            Map<Long, Category> existingCategories = new HashMap<>();
//            long created = 0, skipped = 0;
//
//            int chunkSize = 100;
//            for (int i = 0; i < externalCategories.size(); i += chunkSize) {
//                int end = Math.min(i + chunkSize, externalCategories.size());
//                List<CategoryRequestFromExternalDto> chunk = externalCategories.subList(i, end);
//
//                log.info("Processing category chunk {}-{} of {}", i, end, externalCategories.size());
//
//                Set<Long> chunkExternalIds = chunk.stream()
//                        .map(CategoryRequestFromExternalDto::getId)
//                        .collect(Collectors.toSet());
//
//                List<Category> existingInChunk = categoryRepository.findByExternalIdIn(chunkExternalIds);
//                for (Category cat : existingInChunk) {
//                    existingCategories.put(cat.getExternalId(), cat);
//                }
//
//                List<Category> categoriesToSave = new ArrayList<>();
//
//                for (CategoryRequestFromExternalDto extCategory : chunk) {
//                    if (excludedCategories.contains(extCategory.getId())) {
//                        skipped++;
//                        continue;
//                    }
//
//                    Category category = existingCategories.get(extCategory.getId());
//
//                    if (category == null) {
//                        category = createCategoryFromExternal(extCategory);
//                        categoriesToSave.add(category);
//                        existingCategories.put(category.getExternalId(), category);
//                        created++;
//                        log.debug("Creating new category: {} (externalId: {})",
//                                category.getNameBg(), extCategory.getId());
//                    } else {
//                        skipped++;
//                        log.trace("Category already exists, skipping: {}", category.getNameBg());
//                    }
//                }
//
//                if (!categoriesToSave.isEmpty()) {
//                    categoryRepository.saveAll(categoriesToSave);
//                    categoryRepository.flush();
//                }
//
//                entityManager.clear();
//
//                if (i % 200 == 0) {
//                    System.gc();
//                }
//            }
//
//            log.info("Updating parent relationships...");
//            updateCategoryParentsOptimized(externalCategories, existingCategories);
//
//            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, externalCategories.size(),
//                    created, 0, 0,
//                    String.format("Skipped %d existing categories", skipped), startTime);
//
//            log.info("Categories sync completed - Created: {}, Skipped: {}", created, skipped);
//
//        } catch (Exception e) {
//            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
//            log.error("Error during categories synchronization", e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void updateCategoryParentsOptimized(List<CategoryRequestFromExternalDto> externalCategories,
//                                                Map<Long, Category> existingCategories) {
//        int batchSizeLocal = 50;
//        int updateCount = 0;
//
//        List<Category> categoriesToUpdate = new ArrayList<>();
//
//        for (CategoryRequestFromExternalDto extCategory : externalCategories) {
//            if (extCategory.getParent() != null && extCategory.getParent() != 0) {
//                Category category = existingCategories.get(extCategory.getId());
//                Category parent = existingCategories.get(extCategory.getParent());
//
//                if (category != null && parent != null && !parent.equals(category)) {
//                    if (category.getParent() == null) {
//                        category.setParent(parent);
//                        categoriesToUpdate.add(category);
//                        updateCount++;
//
//                        if (categoriesToUpdate.size() >= batchSizeLocal) {
//                            categoryRepository.saveAll(categoriesToUpdate);
//                            categoryRepository.flush();
//                            entityManager.clear();
//                            categoriesToUpdate.clear();
//                        }
//                    }
//                }
//            }
//        }
//
//        if (!categoriesToUpdate.isEmpty()) {
//            categoryRepository.saveAll(categoriesToUpdate);
//            categoryRepository.flush();
//            entityManager.clear();
//        }
//
//        log.info("Updated parent relationships for {} categories", updateCount);
//    }
//
//    // ===========================================
//    // PARAMETERS SYNC - GLOBAL DEDUPLICATION
//    // ===========================================
//
//    @Transactional
//    public void syncParameters() {
//        String syncType = "VALI_PARAMETERS";
//        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
//        long startTime = System.currentTimeMillis();
//
//        try {
//            log.info("Starting Vali parameters synchronization - GLOBAL DEDUPLICATION mode");
//
//            List<Category> categories = categoryRepository.findAll();
//            log.info("Processing parameters for {} categories", categories.size());
//
//            // ✅ ДИАГНОСТИКА: Провери конкретно категория "Памети" (id=6, externalId=389)
//            Category memoryCategory = categories.stream()
//                    .filter(c -> c.getExternalId() != null && c.getExternalId() == 389L)
//                    .findFirst()
//                    .orElse(null);
//
//            if (memoryCategory != null) {
//                log.error("==================== VALI API DIAGNOSTIC ====================");
//                log.error("Testing category: {} (externalId: {})", memoryCategory.getNameBg(), memoryCategory.getExternalId());
//
//                try {
//                    List<ParameterRequestDto> apiParams = valiApiService.getParametersByCategory(389L);
//
//                    log.error("API returned {} parameters for category 'Памети':", apiParams.size());
//
//                    Set<Long> apiParamIds = new HashSet<>();
//                    for (ParameterRequestDto param : apiParams) {
//                        apiParamIds.add(param.getExternalId());
//                        String nameBg = getParameterNameBg(param);
//                        log.error("  - externalId: {}, name: {}", param.getExternalId(), nameBg);
//                    }
//
//                    log.error("---");
//                    log.error("Checking if missing parameters are in API response:");
//                    log.error("  Parameter 61 in API? {}", apiParamIds.contains(61L));
//                    log.error("  Parameter 707 in API? {}", apiParamIds.contains(707L));
//                    log.error("  Parameter 711 in API? {}", apiParamIds.contains(711L));
//
//                    // Проверка на продукт
//                    log.error("---");
//                    log.error("Now checking what product API says:");
//                    try {
//                        List<ProductRequestDto> products = valiApiService.getProductsByCategory(389L);
//
//                        // Намери проблемния продукт
//                        ProductRequestDto testProduct = products.stream()
//                                .filter(p -> p.getReferenceNumber() != null && p.getReferenceNumber().contains("KF556C40BBAK2"))
//                                .findFirst()
//                                .orElse(null);
//
//                        if (testProduct != null) {
//                            log.error("Found test product: {}", testProduct.getReferenceNumber());
//
//                            if (testProduct.getParameters() != null) {
//                                log.error("Product has {} parameters:", testProduct.getParameters().size());
//
//                                Set<Long> productParamIds = testProduct.getParameters().stream()
//                                        .map(ParameterValueRequestDto::getParameterId)
//                                        .collect(Collectors.toSet());
//
//                                for (Long paramId : productParamIds) {
//                                    boolean inCategoryParams = apiParamIds.contains(paramId);
//                                    log.error("  - parameterId: {} | In category params? {}",
//                                            paramId, inCategoryParams ? "YES" : "NO ❌");
//                                }
//                            }
//                        } else {
//                            log.error("Test product not found in category products");
//                        }
//                    } catch (Exception e) {
//                        log.error("Error fetching products: {}", e.getMessage());
//                    }
//
//                } catch (Exception e) {
//                    log.error("Error during diagnostic: {}", e.getMessage());
//                }
//
//                log.error("============================================================");
//            }
//
//            long totalProcessed = 0, created = 0, reused = 0, errors = 0;
//
//            // ✅ STEP 1: Load ALL parameters globally
//            List<Parameter> allParameters = parameterRepository.findAll();
//
//            // Force initialization of categories collection
//            for (Parameter p : allParameters) {
//                if (p.getCategories() != null) {
//                    p.getCategories().size();
//                }
//            }
//
//            // ✅ STEP 2: Build GLOBAL cache by normalized name
//            Map<String, Parameter> globalParametersCache = allParameters.stream()
//                    .filter(p -> p.getNameBg() != null)
//                    .collect(Collectors.toMap(
//                            p -> normalizeParameterName(p.getNameBg()),
//                            p -> p,
//                            (existing, duplicate) -> {
//                                log.warn("Duplicate parameter name '{}', keeping first (id: {})",
//                                        existing.getNameBg(), existing.getId());
//                                return existing;
//                            }
//                    ));
//
//            log.info("Loaded {} existing parameters from database", globalParametersCache.size());
//
//            // ✅ STEP 3: Load ALL parameter options globally
//            List<ParameterOption> allOptions = parameterOptionRepository.findAll();
//
//            // Structure: parameterNormalizedName -> optionNormalizedName -> ParameterOption
//            Map<String, Map<String, ParameterOption>> globalOptionsCache = new HashMap<>();
//
//            for (ParameterOption option : allOptions) {
//                if (option.getParameter() != null && option.getParameter().getNameBg() != null && option.getNameBg() != null) {
//                    String paramKey = normalizeParameterName(option.getParameter().getNameBg());
//                    String optionKey = normalizeParameterValue(option.getNameBg());
//
//                    globalOptionsCache
//                            .computeIfAbsent(paramKey, k -> new HashMap<>())
//                            .putIfAbsent(optionKey, option);
//                }
//            }
//
//            log.info("Loaded {} existing parameter options from database", allOptions.size());
//
//            // ✅ STEP 4: Process each category
//            for (Category category : categories) {
//                if (category.getExternalId() == null) {
//                    log.debug("Skipping category {} - no externalId", category.getNameBg());
//                    continue;
//                }
//
//                try {
//                    List<ParameterRequestDto> externalParameters = valiApiService
//                            .getParametersByCategory(category.getExternalId());
//
//                    if (externalParameters == null || externalParameters.isEmpty()) {
//                        log.debug("No parameters found for category: {}", category.getNameBg());
//                        continue;
//                    }
//
//                    log.debug("Processing {} parameters for category: {}",
//                            externalParameters.size(), category.getNameBg());
//
//                    for (ParameterRequestDto extParam : externalParameters) {
//                        try {
//                            String nameBg = getParameterNameBg(extParam);
//                            if (nameBg == null || nameBg.isEmpty()) {
//                                log.warn("Parameter missing nameBg, skipping");
//                                continue;
//                            }
//
//                            String normalizedName = normalizeParameterName(nameBg);
//                            Parameter parameter = globalParametersCache.get(normalizedName);
//
//                            if (parameter == null) {
//                                // ✅ CREATE NEW GLOBAL PARAMETER
//                                parameter = createParameterFromExternal(extParam);
//                                parameter.setCategories(new HashSet<>());
//                                parameter.getCategories().add(category);
//
//                                parameter = parameterRepository.save(parameter);
//                                globalParametersCache.put(normalizedName, parameter);
//                                created++;
//
//                                log.debug("✓ Created NEW parameter: '{}' (id: {}) for category '{}'",
//                                        nameBg, parameter.getId(), category.getNameBg());
//                            } else {
//                                // ✅ REUSE EXISTING PARAMETER
//                                Set<Category> parameterCategories = parameter.getCategories();
//                                if (parameterCategories == null) {
//                                    parameterCategories = new HashSet<>();
//                                    parameter.setCategories(parameterCategories);
//                                }
//
//                                // Check if category already linked (use ID comparison)
//                                boolean categoryAlreadyLinked = parameterCategories.stream()
//                                        .anyMatch(cat -> cat.getId().equals(category.getId()));
//
//                                if (!categoryAlreadyLinked) {
//                                    parameterCategories.add(category);
//                                    parameterRepository.save(parameter);
//                                    log.debug("♻ Added category '{}' to existing parameter '{}' (id: {})",
//                                            category.getNameBg(), nameBg, parameter.getId());
//                                } else {
//                                    log.trace("♻ Parameter '{}' already linked to category, skipping", nameBg);
//                                }
//                                reused++;
//                            }
//
//                            // ✅ STEP 5: Sync options with GLOBAL DEDUPLICATION
//                            if (extParam.getOptions() != null && !extParam.getOptions().isEmpty()) {
//                                syncParameterOptionsWithGlobalDeduplication(
//                                        parameter,
//                                        extParam.getOptions(),
//                                        globalOptionsCache,
//                                        normalizedName
//                                );
//                            }
//
//                            totalProcessed++;
//
//                        } catch (Exception e) {
//                            log.error("Error syncing parameter '{}': {}",
//                                    extParam != null ? getParameterNameBg(extParam) : "unknown",
//                                    e.getMessage());
//                            errors++;
//                        }
//                    }
//
//                    // Flush after each category
//                    entityManager.flush();
//
//                } catch (Exception e) {
//                    log.error("Error syncing parameters for category {}: {}",
//                            category.getExternalId(), e.getMessage());
//                    errors++;
//
//                    try {
//                        entityManager.clear();
//                    } catch (Exception clearEx) {
//                        log.error("Error clearing entity manager: {}", clearEx.getMessage());
//                    }
//                }
//            }
//
//            String message = String.format("Created: %d, Reused: %d, Total: %d",
//                    created, reused, totalProcessed);
//
//            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, created,
//                    0, errors, message, startTime);
//
//            log.info("✓ Vali parameters sync completed");
//            log.info("   Created: {}, Reused: {}, Total: {}, Errors: {}",
//                    created, reused, totalProcessed, errors);
//            log.info("   Deduplication ratio: {}%",
//                    totalProcessed > 0 ? (reused * 100 / totalProcessed) : 0);
//
//        } catch (Exception e) {
//            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
//            log.error("Error during Vali parameters synchronization", e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    /**
//     * Синхронизира опциите на параметър с глобално дедуплициране
//     */
//    private void syncParameterOptionsWithGlobalDeduplication(
//            Parameter parameter,
//            List<ParameterOptionRequestDto> externalOptions,
//            Map<String, Map<String, ParameterOption>> globalOptionsCache,
//            String parameterNormalizedName) {
//
//        if (externalOptions == null || externalOptions.isEmpty()) {
//            return;
//        }
//
//        int createdOptions = 0;
//        int reusedOptions = 0;
//        List<ParameterOption> optionsToSave = new ArrayList<>();
//
//        Map<String, ParameterOption> parameterOptions = globalOptionsCache
//                .computeIfAbsent(parameterNormalizedName, k -> new HashMap<>());
//
//        for (ParameterOptionRequestDto extOption : externalOptions) {
//            try {
//                String nameBg = null;
//                String nameEn = null;
//
//                if (extOption.getName() != null) {
//                    for (var name : extOption.getName()) {
//                        if ("bg".equals(name.getLanguageCode())) {
//                            nameBg = name.getText();
//                        } else if ("en".equals(name.getLanguageCode())) {
//                            nameEn = name.getText();
//                        }
//                    }
//                }
//
//                if (nameBg == null || nameBg.isEmpty()) {
//                    log.trace("Option missing nameBg, skipping");
//                    continue;
//                }
//
//                String normalizedValue = normalizeParameterValue(nameBg);
//                ParameterOption existingOption = parameterOptions.get(normalizedValue);
//
//                if (existingOption == null) {
//                    // ✅ CREATE NEW OPTION
//                    ParameterOption option = new ParameterOption();
//                    option.setNameBg(nameBg);
//                    option.setNameEn(nameEn);
//                    option.setParameter(parameter);
//                    option.setExternalId(extOption.getExternalId());
//                    option.setOrder(extOption.getOrder() != null ? extOption.getOrder() : parameterOptions.size());
//
//                    optionsToSave.add(option);
//                    parameterOptions.put(normalizedValue, option);
//                    createdOptions++;
//
//                    log.trace("   ✓ Created NEW option: '{}' for parameter '{}'", nameBg, parameter.getNameBg());
//                } else {
//                    // ✅ REUSE EXISTING OPTION
//                    reusedOptions++;
//                    log.trace("   ♻ Reusing option: '{}' (id: {}) for parameter '{}'",
//                            nameBg, existingOption.getId(), parameter.getNameBg());
//                }
//
//            } catch (Exception e) {
//                log.error("Error processing option for parameter {}: {}", parameter.getNameBg(), e.getMessage());
//            }
//        }
//
//        if (!optionsToSave.isEmpty()) {
//            parameterOptionRepository.saveAll(optionsToSave);
//        }
//
//        if (createdOptions > 0 || reusedOptions > 0) {
//            log.debug("   Options for '{}': Created: {}, Reused: {}",
//                    parameter.getNameBg(), createdOptions, reusedOptions);
//        }
//    }
//
//    private String getParameterNameBg(ParameterRequestDto extParam) {
//        if (extParam.getName() == null) return null;
//
//        return extParam.getName().stream()
//                .filter(name -> "bg".equals(name.getLanguageCode()))
//                .map(name -> name.getText())
//                .findFirst()
//                .orElse(null);
//    }
//
//    private String normalizeParameterName(String name) {
//        if (name == null) return "";
//        return name.toLowerCase().trim().replaceAll("\\s+", " ");
//    }
//
//    private String normalizeParameterValue(String value) {
//        if (value == null) return "";
//        return value.toLowerCase().trim().replaceAll("\\s+", " ");
//    }
//
//    private Parameter createParameterFromExternal(ParameterRequestDto extParameter) {
//        Parameter parameter = new Parameter();
//        parameter.setExternalId(extParameter.getExternalId());
//        parameter.setOrder(extParameter.getOrder());
//        parameter.setPlatform(Platform.VALI);
//        parameter.setCategories(new HashSet<>());
//        parameter.setCreatedBy("system");
//
//        if (extParameter.getName() != null) {
//            extParameter.getName().forEach(name -> {
//                if ("bg".equals(name.getLanguageCode())) {
//                    parameter.setNameBg(name.getText());
//                } else if ("en".equals(name.getLanguageCode())) {
//                    parameter.setNameEn(name.getText());
//                }
//            });
//        }
//
//        return parameter;
//    }
//
//    // ===========================================
//    // PRODUCTS SYNC - MINIMAL UPDATE
//    // ===========================================
//
//    @Transactional
//    public void syncProducts() {
//        String syncType = "VALI_PRODUCTS";
//        log.info("Starting products synchronization - MINIMAL UPDATE mode");
//        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
//        long startTime = System.currentTimeMillis();
//
//        long totalProcessed = 0, created = 0, updated = 0, errors = 0;
//
//        try {
//            Map<Long, Manufacturer> manufacturersMap = manufacturerRepository.findAll()
//                    .stream()
//                    .filter(m -> m.getExternalId() != null)
//                    .collect(Collectors.toMap(
//                            Manufacturer::getExternalId,
//                            m -> m,
//                            (existing, duplicate) -> existing
//                    ));
//
//            log.info("Loaded {} manufacturers with externalId", manufacturersMap.size());
//
//            if (manufacturersMap.isEmpty()) {
//                log.error("⚠️⚠️⚠️ NO MANUFACTURERS FOUND! Products sync will fail! ⚠️⚠️⚠️");
//                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0,
//                        "No manufacturers found", startTime);
//                return;
//            }
//
//            List<Category> categories = categoryRepository.findAll();
//            log.info("Found {} categories to process for products", categories.size());
//
//            int categoryCounter = 0;
//            int processedCategories = 0;
//
//            for (Category category : categories) {
//                categoryCounter++;
//
//                if (category.getExternalId() == null) {
//                    log.debug("Skipping category {} - no externalId", category.getNameBg());
//                    continue;
//                }
//
//                try {
//                    log.info("Processing category {}/{}: {} (externalId: {})",
//                            categoryCounter, categories.size(), category.getNameBg(), category.getExternalId());
//
//                    CategorySyncResult result = syncProductsByCategory(category, manufacturersMap);
//                    totalProcessed += result.processed;
//                    created += result.created;
//                    updated += result.updated;
//                    errors += result.errors;
//                    processedCategories++;
//
//                    log.info("Category {} completed - Processed: {}, Created: {}, Updated: {}, Errors: {}",
//                            category.getNameBg(), result.processed, result.created, result.updated, result.errors);
//
//                } catch (Exception e) {
//                    log.error("Error processing products for category {}: {}",
//                            category.getExternalId(), e.getMessage(), e);
//                    errors++;
//                }
//            }
//
//            log.info("Processed {} categories out of {} total", processedCategories, categories.size());
//
//            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, created, updated, errors,
//                    errors > 0 ? String.format("Completed with %d errors", errors) : null, startTime);
//            log.info("Products sync completed - Total: {}, Created: {}, Updated: {}, Errors: {}",
//                    totalProcessed, created, updated, errors);
//
//        } catch (Exception e) {
//            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, totalProcessed, created, updated, errors,
//                    e.getMessage(), startTime);
//            log.error("Error during products synchronization", e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    private CategorySyncResult syncProductsByCategory(Category category,
//                                                      Map<Long, Manufacturer> manufacturersMap) {
//        long totalProcessed = 0, created = 0, updated = 0, errors = 0;
//
//        try {
//            List<ProductRequestDto> allProducts = valiApiService.getProductsByCategory(category.getExternalId());
//
//            if (allProducts.isEmpty()) {
//                log.debug("No products found for category: {}", category.getNameBg());
//                return new CategorySyncResult(0, 0, 0, 0);
//            }
//
//            log.info("Fetched {} products for category: {}", allProducts.size(), category.getNameBg());
//
//            List<List<ProductRequestDto>> chunks = partitionList(allProducts, batchSize);
//            log.info("Split into {} chunks of size {}", chunks.size(), batchSize);
//
//            for (int i = 0; i < chunks.size(); i++) {
//                List<ProductRequestDto> chunk = chunks.get(i);
//
//                try {
//                    log.debug("Processing chunk {}/{} ({} products)", i + 1, chunks.size(), chunk.size());
//
//                    ChunkResult result = processProductsChunk(chunk, manufacturersMap, category);
//                    totalProcessed += result.processed;
//                    created += result.created;
//                    updated += result.updated;
//                    errors += result.errors;
//
//                    if (i < chunks.size() - 1) {
//                        entityManager.flush();
//                        entityManager.clear();
//                    }
//
//                } catch (Exception e) {
//                    log.error("Error processing product chunk {}/{} for category {}: {}",
//                            i + 1, chunks.size(), category.getExternalId(), e.getMessage(), e);
//                    errors += chunk.size();
//                }
//            }
//
//            log.info("Category {} sync result - Processed: {}, Created: {}, Updated: {}, Errors: {}",
//                    category.getNameBg(), totalProcessed, created, updated, errors);
//
//        } catch (Exception e) {
//            log.error("Error getting products for category {}: {}", category.getExternalId(), e.getMessage(), e);
//            errors++;
//        }
//
//        return new CategorySyncResult(totalProcessed, created, updated, errors);
//    }
//
//    // ============================================
//    // ФИНАЛНА ВЕРСИЯ - processProductsChunk
//    // Замени ЦЕЛИЯ метод в ValiSyncService.java
//    // ============================================
//
//    private ChunkResult processProductsChunk(List<ProductRequestDto> products,
//                                             Map<Long, Manufacturer> manufacturersMap,
//                                             Category category) {
//        long processed = 0, created = 0, updated = 0, errors = 0;
//
//        Set<Long> externalProductIdsInChunk = products.stream()
//                .map(ProductRequestDto::getId)
//                .collect(Collectors.toSet());
//
//        Map<Long, Product> existingProductsMap = productRepository.findByExternalIdIn(externalProductIdsInChunk)
//                .stream()
//                .collect(Collectors.toMap(Product::getExternalId, p -> p));
//
//        log.debug("Found {} existing products out of {} in chunk", existingProductsMap.size(), products.size());
//
//        // ✅ STEP 1: Collect ALL parameter/option external IDs from chunk
//        Set<Long> allParameterExternalIds = new HashSet<>();
//        Set<Long> allOptionExternalIds = new HashSet<>();
//
//        for (ProductRequestDto extProduct : products) {
//            if (extProduct.getParameters() != null) {
//                allParameterExternalIds.addAll(extProduct.getParameters().stream()
//                        .map(ParameterValueRequestDto::getParameterId)
//                        .collect(Collectors.toSet()));
//                allOptionExternalIds.addAll(extProduct.getParameters().stream()
//                        .map(ParameterValueRequestDto::getOptionId)
//                        .filter(id -> id != null && id != 0)  // ✅ FIX: Filter out invalid option IDs
//                        .collect(Collectors.toSet()));
//            }
//        }
//
//        log.debug("Chunk needs {} unique parameters and {} unique options",
//                allParameterExternalIds.size(), allOptionExternalIds.size());
//
//        // ✅ STEP 2: Load existing parameters from DB
//        List<Parameter> existingParameters = parameterRepository.findAll().stream()
//                .filter(p -> p.getExternalId() != null && allParameterExternalIds.contains(p.getExternalId()))
//                .toList();
//
//        Map<Long, Parameter> chunkParametersCache = existingParameters.stream()
//                .collect(Collectors.toMap(
//                        Parameter::getExternalId,
//                        p -> p,
//                        (existing, duplicate) -> existing
//                ));
//
//        // ✅ STEP 3: Find MISSING parameters and sync them on-the-fly
//        Set<Long> missingParameterIds = new HashSet<>(allParameterExternalIds);
//        missingParameterIds.removeAll(chunkParametersCache.keySet());
//
//        if (!missingParameterIds.isEmpty()) {
//            log.warn("⚠️ MISSING {} parameters in database: {}", missingParameterIds.size(), missingParameterIds);
//            log.warn("⚠️ These parameters were NOT synced by syncParameters()!");
//            log.warn("⚠️ Products using them will have 'not found' parameters.");
//            log.warn("⚠️ Solution: Run syncParameters() for category '{}' (externalId: {})",
//                    category.getNameBg(), category.getExternalId());
//
//            try {
//                syncMissingParametersForCategory(category, missingParameterIds, chunkParametersCache);
//            } catch (Exception e) {
//                log.error("Failed to sync missing parameters on-the-fly: {}", e.getMessage());
//            }
//        }
//
//        // ✅ STEP 4: Load existing options from DB
//        List<ParameterOption> existingOptions = parameterOptionRepository.findAll().stream()
//                .filter(o -> o.getExternalId() != null &&
//                        o.getExternalId() != 0 &&  // ✅ FIX: Filter invalid IDs
//                        allOptionExternalIds.contains(o.getExternalId()))
//                .toList();
//
//        Map<Long, ParameterOption> chunkOptionsCache = existingOptions.stream()
//                .collect(Collectors.toMap(
//                        ParameterOption::getExternalId,
//                        o -> o,
//                        (existing, duplicate) -> existing
//                ));
//
//        // ✅ STEP 5: Find MISSING options and sync them on-the-fly
//        Set<Long> missingOptionIds = new HashSet<>(allOptionExternalIds);
//        missingOptionIds.removeAll(chunkOptionsCache.keySet());
//
//        if (!missingOptionIds.isEmpty()) {
//            log.warn("⚠️ MISSING {} options in database: {}", missingOptionIds.size(), missingOptionIds);
//
//            try {
//                syncMissingOptionsForCategory(category, missingOptionIds, chunkParametersCache, chunkOptionsCache);
//            } catch (Exception e) {
//                log.error("Failed to sync missing options on-the-fly: {}", e.getMessage());
//            }
//        }
//
//        log.debug("Pre-loaded {} parameters and {} options for chunk",
//                chunkParametersCache.size(), chunkOptionsCache.size());
//
//        // ✅ STEP 6: Process products
//        List<Product> productsToSave = new ArrayList<>();
//
//        for (ProductRequestDto extProduct : products) {
//            try {
//                Manufacturer manufacturer = manufacturersMap.get(extProduct.getManufacturerId());
//                if (manufacturer == null) {
//                    log.warn("Manufacturer with externalId {} not found for product {} ({}), skipping",
//                            extProduct.getManufacturerId(), extProduct.getReferenceNumber(), extProduct.getId());
//                    errors++;
//                    continue;
//                }
//
//                Product product;
//
//                if (existingProductsMap.containsKey(extProduct.getId())) {
//                    product = existingProductsMap.get(extProduct.getId());
//                    log.trace("Updating existing product: {} (externalId: {})",
//                            product.getSku(), product.getExternalId());
//                    updateProductFieldsFromExternal(product, extProduct, manufacturer, category,
//                            chunkParametersCache, chunkOptionsCache);
//                    updated++;
//                } else {
//                    product = new Product();
//                    product.setId(null);
//                    log.trace("Creating new product: {} (externalId: {})",
//                            extProduct.getReferenceNumber(), extProduct.getId());
//                    updateProductFieldsFromExternal(product, extProduct, manufacturer, category,
//                            chunkParametersCache, chunkOptionsCache);
//                    created++;
//                }
//
//                productsToSave.add(product);
//                processed++;
//
//            } catch (Exception e) {
//                errors++;
//                log.error("Error processing product externalId={}, sku={}: {}",
//                        extProduct.getId(),
//                        extProduct.getReferenceNumber(),
//                        e.getMessage());
//
//                if (errors <= 3) {
//                    log.error("Full exception for debugging:", e);
//                }
//            }
//        }
//
//        if (!productsToSave.isEmpty()) {
//            try {
//                log.debug("Saving {} products from chunk", productsToSave.size());
//                productRepository.saveAll(productsToSave);
//                log.debug("Successfully saved {} products", productsToSave.size());
//            } catch (Exception e) {
//                log.error("Error saving products batch: {}", e.getMessage(), e);
//                errors += productsToSave.size();
//            }
//        }
//
//        entityManager.flush();
//        entityManager.clear();
//
//        log.debug("Chunk processed - Processed: {}, Created: {}, Updated: {}, Errors: {}",
//                processed, created, updated, errors);
//
//        return new ChunkResult(processed, created, updated, errors);
//    }
//
//    // ============================================
//    // НОВ МЕТОД - syncMissingOptionsForCategory
//    // Добави този метод СЛЕД syncMissingParametersForCategory
//    // ============================================
//
//    /**
//     * Синхронизира липсващи опции on-the-fly
//     */
//    private void syncMissingOptionsForCategory(Category category,
//                                               Set<Long> missingOptionIds,
//                                               Map<Long, Parameter> chunkParametersCache,
//                                               Map<Long, ParameterOption> chunkOptionsCache) {
//        log.info("Attempting to fetch {} missing options from Vali API for category '{}'",
//                missingOptionIds.size(), category.getNameBg());
//
//        try {
//            // Fetch ALL parameters for this category from API
//            List<ParameterRequestDto> apiParameters = valiApiService.getParametersByCategory(category.getExternalId());
//
//            int syncedCount = 0;
//
//            for (ParameterRequestDto extParam : apiParameters) {
//                if (extParam.getOptions() == null || extParam.getOptions().isEmpty()) {
//                    continue;
//                }
//
//                // Find the parameter in cache
//                Parameter parameter = chunkParametersCache.get(extParam.getExternalId());
//                if (parameter == null) {
//                    log.trace("Parameter {} not in cache, skipping options", extParam.getExternalId());
//                    continue;
//                }
//
//                // Check each option
//                for (ParameterOptionRequestDto extOption : extParam.getOptions()) {
//                    Long optionExternalId = extOption.getExternalId();
//
//                    // Skip invalid IDs
//                    if (optionExternalId == null || optionExternalId == 0) {
//                        log.trace("Skipping invalid option externalId: {}", optionExternalId);
//                        continue;
//                    }
//
//                    // Check if this is a missing option
//                    if (missingOptionIds.contains(optionExternalId)) {
//                        // Extract names
//                        String nameBg = null;
//                        String nameEn = null;
//
//                        if (extOption.getName() != null) {
//                            for (var name : extOption.getName()) {
//                                if ("bg".equals(name.getLanguageCode())) {
//                                    nameBg = name.getText();
//                                } else if ("en".equals(name.getLanguageCode())) {
//                                    nameEn = name.getText();
//                                }
//                            }
//                        }
//
//                        if (nameBg == null || nameBg.isEmpty()) {
//                            log.warn("Option {} has no nameBg, skipping", optionExternalId);
//                            continue;
//                        }
//
//                        // Create option
//                        ParameterOption option = new ParameterOption();
//                        option.setNameBg(nameBg);
//                        option.setNameEn(nameEn);
//                        option.setParameter(parameter);
//                        option.setExternalId(optionExternalId);
//                        option.setOrder(extOption.getOrder() != null ? extOption.getOrder() : 0);
//
//                        option = parameterOptionRepository.save(option);
//                        chunkOptionsCache.put(option.getExternalId(), option);
//
//                        log.info("✓ Created missing option: '{}' (external_id: {}) for parameter '{}'",
//                                nameBg, optionExternalId, parameter.getNameBg());
//
//                        syncedCount++;
//                    }
//                }
//            }
//
//            log.info("✓ Successfully synced {} missing options on-the-fly", syncedCount);
//
//            if (syncedCount < missingOptionIds.size()) {
//                log.warn("⚠️ Could not find {} options in API response - they may not exist for this category",
//                        missingOptionIds.size() - syncedCount);
//                log.warn("⚠️ Missing option IDs that were not found: {}",
//                        missingOptionIds.stream()
//                                .filter(id -> !chunkOptionsCache.containsKey(id))
//                                .collect(Collectors.toSet()));
//            }
//
//        } catch (Exception e) {
//            log.error("Error fetching missing options from API: {}", e.getMessage());
//        }
//    }
//
//    private void syncMissingParametersForCategory(Category category,
//                                                  Set<Long> missingParameterIds,
//                                                  Map<Long, Parameter> chunkParametersCache) {
//        log.info("Attempting to fetch {} missing parameters from Vali API for category '{}'",
//                missingParameterIds.size(), category.getNameBg());
//
//        try {
//            // Fetch ALL parameters for this category from API
//            List<ParameterRequestDto> apiParameters = valiApiService.getParametersByCategory(category.getExternalId());
//
//            int syncedCount = 0;
//
//            for (ParameterRequestDto extParam : apiParameters) {
//                if (missingParameterIds.contains(extParam.getExternalId())) {
//                    // Create parameter
//                    Parameter parameter = createParameterFromExternal(extParam);
//                    parameter.setCategories(new HashSet<>());
//                    parameter.getCategories().add(category);
//
//                    parameter = parameterRepository.save(parameter);
//                    chunkParametersCache.put(parameter.getExternalId(), parameter);
//
//                    log.info("✓ Created missing parameter: '{}' (external_id: {}) for category '{}'",
//                            parameter.getNameBg(), parameter.getExternalId(), category.getNameBg());
//
//                    // Sync options for this parameter
//                    if (extParam.getOptions() != null && !extParam.getOptions().isEmpty()) {
//                        Map<String, Map<String, ParameterOption>> tempOptionsCache = new HashMap<>();
//                        syncParameterOptionsWithGlobalDeduplication(
//                                parameter,
//                                extParam.getOptions(),
//                                tempOptionsCache,
//                                normalizeParameterName(parameter.getNameBg())
//                        );
//                    }
//
//                    syncedCount++;
//                }
//            }
//
//            log.info("✓ Successfully synced {} missing parameters on-the-fly", syncedCount);
//
//            if (syncedCount < missingParameterIds.size()) {
//                log.warn("⚠️ Could not find {} parameters in API response - they may not exist for this category",
//                        missingParameterIds.size() - syncedCount);
//            }
//
//        } catch (Exception e) {
//            log.error("Error fetching missing parameters from API: {}", e.getMessage());
//        }
//    }
//
//    private void updateProductFieldsFromExternal(Product product, ProductRequestDto extProduct,
//                                                 Manufacturer manufacturer, Category category,
//                                                 Map<Long, Parameter> chunkParametersCache,
//                                                 Map<Long, ParameterOption> chunkOptionsCache) {
//        boolean isNew = (product.getId() == null);
//
//        try {
//            if (isNew) {
//                product.setExternalId(extProduct.getId());
//                product.setWorkflowId(extProduct.getIdWF());
//                product.setReferenceNumber(extProduct.getReferenceNumber());
//                product.setModel(extProduct.getModel());
//                product.setBarcode(extProduct.getBarcode());
//
//                if (manufacturer != null) {
//                    product.setManufacturer(manufacturer);
//                } else {
//                    log.warn("Product {} has no manufacturer (manufacturerId: {})",
//                            extProduct.getReferenceNumber(), extProduct.getManufacturerId());
//                }
//
//                product.setCategory(category);
//                product.setWarranty(extProduct.getWarranty());
//                product.setWeight(extProduct.getWeight());
//                product.setPlatform(Platform.VALI);
//                product.setShow(extProduct.getShow());
//
//                setImagesToProduct(product, extProduct);
//                setNamesToProduct(product, extProduct);
//                setDescriptionToProduct(product, extProduct);
//
//                try {
//                    setParametersToProduct(product, extProduct, chunkParametersCache, chunkOptionsCache);
//                } catch (Exception e) {
//                    log.error("Error setting parameters for product {}: {}",
//                            extProduct.getReferenceNumber(), e.getMessage());
//                    product.setProductParameters(new HashSet<>());
//                }
//            }
//
//            // ALWAYS UPDATE
//            product.setStatus(ProductStatus.fromCode(extProduct.getStatus()));
//            product.setPriceClient(extProduct.getPriceClient());
//            product.setPricePartner(extProduct.getPricePartner());
//            product.setPricePromo(extProduct.getPricePromo());
//            product.setPriceClientPromo(extProduct.getPriceClientPromo());
//
//            product.calculateFinalPrice();
//            product.setCreatedBy("system");
//
//            if (!isNew) {
//                log.trace("Updated product {} - status: {}, priceClient: {}",
//                        product.getSku(), product.getStatus(), product.getPriceClient());
//            }
//
//        } catch (Exception e) {
//            log.error("Critical error in updateProductFieldsFromExternal for product {}: {}",
//                    extProduct.getReferenceNumber(), e.getMessage(), e);
//            throw e;
//        }
//    }
//
//// ============================================
//    // ОБНОВЕН МЕТОД - setParametersToProduct
//    // Замени ЦЕЛИЯ метод в ValiSyncService.java
//    // ============================================
//
//    private void setParametersToProduct(Product product, ProductRequestDto extProduct,
//                                        Map<Long, Parameter> chunkParametersCache,
//                                        Map<Long, ParameterOption> chunkOptionsCache) {
//        if (extProduct.getParameters() == null || product.getCategory() == null) {
//            if (product.getProductParameters() == null) {
//                product.setProductParameters(new HashSet<>());
//            }
//            return;
//        }
//
//        Set<ProductParameter> existingProductParams = product.getProductParameters();
//        if (existingProductParams == null) {
//            existingProductParams = new HashSet<>();
//        }
//
//        Set<ProductParameter> manualParameters = existingProductParams.stream()
//                .filter(pp -> pp.getParameter() != null)
//                .filter(this::isManualParameterForVali)
//                .collect(Collectors.toSet());
//
//        Set<ProductParameter> autoParameters = new HashSet<>();
//        int mappedCount = 0;
//        int notFoundCount = 0;
//        int skippedInvalidCount = 0;
//
//        for (ParameterValueRequestDto paramValue : extProduct.getParameters()) {
//            try {
//                // ✅ FIX: Skip invalid option IDs (0 or null)
//                if (paramValue.getOptionId() == null || paramValue.getOptionId() == 0) {
//                    log.trace("Skipping parameter with invalid optionId: {} for product {}",
//                            paramValue.getOptionId(), extProduct.getReferenceNumber());
//                    skippedInvalidCount++;
//                    continue;
//                }
//
//                // ✅ Use pre-loaded chunk cache (NO database queries here!)
//                Parameter parameter = chunkParametersCache.get(paramValue.getParameterId());
//                if (parameter == null) {
//                    log.trace("Parameter with external ID {} not found in cache for product {}",
//                            paramValue.getParameterId(), extProduct.getReferenceNumber());
//                    notFoundCount++;
//                    continue;
//                }
//
//                ParameterOption option = chunkOptionsCache.get(paramValue.getOptionId());
//                if (option == null) {
//                    log.trace("Parameter option with external ID {} not found in cache for product {}",
//                            paramValue.getOptionId(), extProduct.getReferenceNumber());
//                    notFoundCount++;
//                    continue;
//                }
//
//                if (!option.getParameter().getId().equals(parameter.getId())) {
//                    log.trace("Parameter option {} does not belong to parameter {} for product {}",
//                            paramValue.getOptionId(), paramValue.getParameterId(), extProduct.getReferenceNumber());
//                    notFoundCount++;
//                    continue;
//                }
//
//                // ✅ AUTO-LINK: If parameter not linked to category, link it now
//                if (parameter.getCategories() != null &&
//                        parameter.getCategories().stream().noneMatch(cat -> cat.getId().equals(product.getCategory().getId()))) {
//
//                    parameter.getCategories().add(product.getCategory());
//                    parameterRepository.save(parameter);
//
//                    log.debug("Auto-linked parameter '{}' (id:{}) to category '{}' via product '{}'",
//                            parameter.getNameBg(), parameter.getId(), product.getCategory().getNameBg(), extProduct.getReferenceNumber());
//                }
//
//                ProductParameter pp = new ProductParameter();
//                pp.setProduct(product);
//                pp.setParameter(parameter);
//                pp.setParameterOption(option);
//                autoParameters.add(pp);
//
//                mappedCount++;
//
//            } catch (Exception e) {
//                log.error("Error mapping parameter for product {}: {}", extProduct.getReferenceNumber(), e.getMessage());
//                notFoundCount++;
//            }
//        }
//
//        Set<ProductParameter> mergedParameters = new HashSet<>();
//        mergedParameters.addAll(manualParameters);
//        mergedParameters.addAll(autoParameters);
//
//        product.setProductParameters(mergedParameters);
//
//        if (mappedCount > 0 || notFoundCount > 0 || !manualParameters.isEmpty() || skippedInvalidCount > 0) {
//            log.info("Product {} parameter mapping: {} from Vali API, {} manual (preserved), {} not found{}",
//                    extProduct.getReferenceNumber(),
//                    mappedCount,
//                    manualParameters.size(),
//                    notFoundCount,
//                    skippedInvalidCount > 0 ? ", " + skippedInvalidCount + " invalid (skipped)" : "");
//        }
//    }
//
//    private boolean isManualParameterForVali(ProductParameter productParameter) {
//        Parameter parameter = productParameter.getParameter();
//
//        if (parameter == null) {
//            return false;
//        }
//
//        boolean isDifferentPlatform = (parameter.getPlatform() == null ||
//                parameter.getPlatform() != Platform.VALI);
//
//        boolean isCreatedByAdmin = isAdminUser(parameter.getCreatedBy());
//        boolean isModifiedByAdmin = isAdminUser(parameter.getLastModifiedBy());
//
//        boolean isManual = isDifferentPlatform || isCreatedByAdmin || isModifiedByAdmin;
//
//        if (isManual) {
//            log.trace("Parameter '{}' identified as manual: platform={}, createdBy={}, lastModifiedBy={}",
//                    parameter.getNameBg(), parameter.getPlatform(),
//                    parameter.getCreatedBy(), parameter.getLastModifiedBy());
//        }
//
//        return isManual;
//    }
//
//    private boolean isAdminUser(String username) {
//        if (username == null || username.isEmpty()) {
//            return false;
//        }
//
//        return "ADMIN".equalsIgnoreCase(username.trim()) ||
//                "admin".equalsIgnoreCase(username.trim());
//    }
//
//    // ===========================================
//    // HELPER METHODS
//    // ===========================================
//
//    private Category createCategoryFromExternal(CategoryRequestFromExternalDto extCategory) {
//        Category category = new Category();
//        category.setExternalId(extCategory.getId());
//        category.setShow(extCategory.getShow());
//        category.setSortOrder(extCategory.getOrder());
//        category.setPlatform(Platform.VALI);
//
//        if (extCategory.getName() != null) {
//            extCategory.getName().forEach(name -> {
//                if ("bg".equals(name.getLanguageCode())) {
//                    category.setNameBg(name.getText());
//                } else if ("en".equals(name.getLanguageCode())) {
//                    category.setNameEn(name.getText());
//                }
//            });
//        }
//
//        String baseName = category.getNameEn() != null ? category.getNameEn() : category.getNameBg();
//        category.setSlug(generateUniqueSlugForVali(baseName, category));
//
//        return category;
//    }
//
//    private String generateUniqueSlugForVali(String categoryName, Category category) {
//        if (categoryName == null || categoryName.trim().isEmpty()) {
//            return "category-" + System.currentTimeMillis();
//        }
//
//        String baseSlug = syncHelper.createSlugFromName(categoryName);
//
//        if (!categoryRepository.existsBySlugAndIdNot(baseSlug, category.getId())) {
//            return baseSlug;
//        }
//
//        String discriminator = syncHelper.extractDiscriminator(categoryName);
//        if (discriminator != null && !discriminator.isEmpty()) {
//            String discriminatedSlug = baseSlug + "-" + discriminator;
//            if (!categoryRepository.existsBySlugAndIdNot(discriminatedSlug, category.getId())) {
//                return discriminatedSlug;
//            }
//        }
//
//        int counter = 1;
//        String numberedSlug;
//        do {
//            numberedSlug = baseSlug + "-" + counter;
//            counter++;
//        } while (categoryRepository.existsBySlugAndIdNot(numberedSlug, category.getId()));
//
//        return numberedSlug;
//    }
//
//    private Manufacturer createManufacturerFromExternal(ManufacturerRequestDto extManufacturer) {
//        Manufacturer manufacturer = new Manufacturer();
//        manufacturer.setExternalId(extManufacturer.getId());
//        manufacturer.setName(extManufacturer.getName());
//        manufacturer.setPlatform(Platform.VALI);
//
//        if (extManufacturer.getInformation() != null) {
//            manufacturer.setInformationName(extManufacturer.getInformation().getName());
//            manufacturer.setInformationEmail(extManufacturer.getInformation().getEmail());
//            manufacturer.setInformationAddress(extManufacturer.getInformation().getAddress());
//        }
//
//        if (extManufacturer.getEuRepresentative() != null) {
//            manufacturer.setEuRepresentativeName(extManufacturer.getEuRepresentative().getName());
//            manufacturer.setEuRepresentativeEmail(extManufacturer.getEuRepresentative().getEmail());
//            manufacturer.setEuRepresentativeAddress(extManufacturer.getEuRepresentative().getAddress());
//        }
//
//        return manufacturer;
//    }
//
//    private static void setImagesToProduct(Product product, ProductRequestDto extProduct) {
//        if (extProduct.getImages() != null && !extProduct.getImages().isEmpty()) {
//            product.setPrimaryImageUrl(extProduct.getImages().get(0).getHref());
//
//            List<String> newAdditionalImages = extProduct.getImages().stream()
//                    .skip(1)
//                    .map(ImageDto::getHref)
//                    .toList();
//
//            if (product.getAdditionalImages() != null) {
//                product.getAdditionalImages().clear();
//                product.getAdditionalImages().addAll(newAdditionalImages);
//            } else {
//                product.setAdditionalImages(new ArrayList<>(newAdditionalImages));
//            }
//        } else {
//            product.setPrimaryImageUrl(null);
//            if (product.getAdditionalImages() != null) {
//                product.getAdditionalImages().clear();
//            } else {
//                product.setAdditionalImages(new ArrayList<>());
//            }
//        }
//    }
//
//    private static void setNamesToProduct(Product product, ProductRequestDto extProduct) {
//        if (extProduct.getName() != null) {
//            extProduct.getName().forEach(name -> {
//                if ("bg".equals(name.getLanguageCode())) {
//                    product.setNameBg(name.getText());
//                } else if ("en".equals(name.getLanguageCode())) {
//                    product.setNameEn(name.getText());
//                }
//            });
//        }
//    }
//
//    private static void setDescriptionToProduct(Product product, ProductRequestDto extProduct) {
//        if (extProduct.getDescription() != null) {
//            extProduct.getDescription().forEach(desc -> {
//                if ("bg".equals(desc.getLanguageCode())) {
//                    product.setDescriptionBg(desc.getText());
//                } else if ("en".equals(desc.getLanguageCode())) {
//                    product.setDescriptionEn(desc.getText());
//                }
//            });
//        }
//    }
//
//    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
//        List<List<T>> partitions = new ArrayList<>();
//        for (int i = 0; i < list.size(); i += partitionSize) {
//            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
//        }
//        return partitions;
//    }
//
//    // ===========================================
//    // RESULT CLASSES
//    // ===========================================
//
//    private static class CategorySyncResult {
//        long processed;
//        long created;
//        long updated;
//        long errors;
//
//        CategorySyncResult(long processed, long created, long updated, long errors) {
//            this.processed = processed;
//            this.created = created;
//            this.updated = updated;
//            this.errors = errors;
//        }
//    }
//
//    private static class ChunkResult {
//        long processed;
//        long created;
//        long updated;
//        long errors;
//
//        ChunkResult(long processed, long created, long updated, long errors) {
//            this.processed = processed;
//            this.created = created;
//            this.updated = updated;
//            this.errors = errors;
//        }
//    }
//}


package com.techstore.service.sync;

import com.techstore.dto.external.ImageDto;
import com.techstore.dto.request.*;
import com.techstore.entity.*;
import com.techstore.enums.Platform;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.*;
import com.techstore.service.ValiApiService;
import com.techstore.util.LogHelper;
import com.techstore.util.SyncHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.techstore.util.LogHelper.LOG_STATUS_FAILED;
import static com.techstore.util.LogHelper.LOG_STATUS_SUCCESS;

/**
 * ValiSyncService - COMPLETELY REWRITTEN VERSION 3.0
 *
 * Дата: 23.01.2025
 *
 * КРИСТАЛНА ЛОГИКА:
 * 1. Manufacturers → CREATE ONLY
 * 2. Categories → CREATE ONLY
 * 3. Parameters → ГЛОБАЛНО дедуплициране по ИМЕ
 * 4. Products → Използва готовите параметри
 *
 * ГАРАНЦИИ:
 * - Един параметър "RAM" за ВСИЧКИ категории
 * - Една опция "16GB" за ВСИЧКИ "RAM"
 * - НИЩО не липсва
 * - БЕЗ дублирания
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValiSyncService {

    private final ValiApiService valiApiService;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;
    private final ParameterRepository parameterRepository;
    private final ParameterOptionRepository parameterOptionRepository;
    private final EntityManager entityManager;
    private final SyncHelper syncHelper;
    private final LogHelper logHelper;

    @Value("#{'${excluded.categories.external-ids}'.split(',')}")
    private Set<Long> excludedCategories;

    @Value("${app.sync.batch-size:30}")
    private int batchSize;

    @Transactional
    public void syncManufacturers() {
        String syncType = "VALI_MANUFACTURERS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting manufacturers synchronization - CREATE ONLY mode");

            List<ManufacturerRequestDto> externalManufacturers = valiApiService.getManufacturers();
            log.info("Fetched {} manufacturers from Vali API", externalManufacturers.size());

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

            for (ManufacturerRequestDto extManufacturer : externalManufacturers) {
                String normalizedName = normalizeManufacturerName(extManufacturer.getName());
                Manufacturer manufacturer = existingManufacturers.get(normalizedName);

                if (manufacturer == null) {
                    manufacturer = createManufacturerFromExternal(extManufacturer);
                    manufacturer = manufacturerRepository.save(manufacturer);
                    existingManufacturers.put(normalizedName, manufacturer);
                    created++;
                } else {
                    skipped++;
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    (long) externalManufacturers.size(), created, 0, 0,
                    String.format("Skipped %d existing", skipped), startTime);
            log.info("Manufacturers sync completed - Created: {}, Skipped: {}", created, skipped);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during manufacturers synchronization", e);
            throw new RuntimeException(e);
        }
    }

    private String normalizeManufacturerName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private Manufacturer createManufacturerFromExternal(ManufacturerRequestDto extManufacturer) {
        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setExternalId(extManufacturer.getId());
        manufacturer.setName(extManufacturer.getName());
        manufacturer.setPlatform(Platform.VALI);

        if (extManufacturer.getInformation() != null) {
            manufacturer.setInformationName(extManufacturer.getInformation().getName());
            manufacturer.setInformationEmail(extManufacturer.getInformation().getEmail());
            manufacturer.setInformationAddress(extManufacturer.getInformation().getAddress());
        }

        if (extManufacturer.getEuRepresentative() != null) {
            manufacturer.setEuRepresentativeName(extManufacturer.getEuRepresentative().getName());
            manufacturer.setEuRepresentativeEmail(extManufacturer.getEuRepresentative().getEmail());
            manufacturer.setEuRepresentativeAddress(extManufacturer.getEuRepresentative().getAddress());
        }

        return manufacturer;
    }

    @Transactional
    public void syncCategories() {
        String syncType = "VALI_CATEGORIES";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting categories synchronization - CREATE ONLY mode");

            List<CategoryRequestFromExternalDto> externalCategories = valiApiService.getCategories();
            log.info("Fetched {} categories from Vali API", externalCategories.size());

            entityManager.clear();

            Map<Long, Category> existingCategories = new HashMap<>();
            long created = 0, skipped = 0;

            int chunkSize = 100;
            for (int i = 0; i < externalCategories.size(); i += chunkSize) {
                int end = Math.min(i + chunkSize, externalCategories.size());
                List<CategoryRequestFromExternalDto> chunk = externalCategories.subList(i, end);

                Set<Long> chunkExternalIds = chunk.stream()
                        .map(CategoryRequestFromExternalDto::getId)
                        .collect(Collectors.toSet());

                List<Category> existingInChunk = categoryRepository.findByExternalIdIn(chunkExternalIds);
                for (Category cat : existingInChunk) {
                    existingCategories.put(cat.getExternalId(), cat);
                }

                List<Category> categoriesToSave = new ArrayList<>();

                for (CategoryRequestFromExternalDto extCategory : chunk) {
                    if (excludedCategories.contains(extCategory.getId())) {
                        skipped++;
                        continue;
                    }

                    Category category = existingCategories.get(extCategory.getId());

                    if (category == null) {
                        category = createCategoryFromExternal(extCategory);
                        categoriesToSave.add(category);
                        existingCategories.put(category.getExternalId(), category);
                        created++;
                    } else {
                        skipped++;
                    }
                }

                if (!categoriesToSave.isEmpty()) {
                    categoryRepository.saveAll(categoriesToSave);
                    categoryRepository.flush();
                }

                entityManager.clear();

                if (i % 200 == 0) {
                    System.gc();
                }
            }

            updateCategoryParentsOptimized(externalCategories, existingCategories);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, externalCategories.size(),
                    created, 0, 0, String.format("Skipped %d existing", skipped), startTime);

            log.info("Categories sync completed - Created: {}, Skipped: {}", created, skipped);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during categories synchronization", e);
            throw new RuntimeException(e);
        }
    }

    private void updateCategoryParentsOptimized(List<CategoryRequestFromExternalDto> externalCategories,
                                                Map<Long, Category> existingCategories) {
        int batchSizeLocal = 50;
        List<Category> categoriesToUpdate = new ArrayList<>();

        for (CategoryRequestFromExternalDto extCategory : externalCategories) {
            if (extCategory.getParent() != null && extCategory.getParent() != 0) {
                Category category = existingCategories.get(extCategory.getId());
                Category parent = existingCategories.get(extCategory.getParent());

                if (category != null && parent != null && !parent.equals(category)) {
                    if (category.getParent() == null) {
                        category.setParent(parent);
                        categoriesToUpdate.add(category);

                        if (categoriesToUpdate.size() >= batchSizeLocal) {
                            categoryRepository.saveAll(categoriesToUpdate);
                            categoryRepository.flush();
                            entityManager.clear();
                            categoriesToUpdate.clear();
                        }
                    }
                }
            }
        }

        if (!categoriesToUpdate.isEmpty()) {
            categoryRepository.saveAll(categoriesToUpdate);
            categoryRepository.flush();
            entityManager.clear();
        }
    }

    private Category createCategoryFromExternal(CategoryRequestFromExternalDto extCategory) {
        Category category = new Category();
        category.setExternalId(extCategory.getId());
        category.setShow(extCategory.getShow());
        category.setSortOrder(extCategory.getOrder());
        category.setPlatform(Platform.VALI);

        if (extCategory.getName() != null) {
            extCategory.getName().forEach(name -> {
                if ("bg".equals(name.getLanguageCode())) {
                    category.setNameBg(name.getText());
                } else if ("en".equals(name.getLanguageCode())) {
                    category.setNameEn(name.getText());
                }
            });
        }

        String baseName = category.getNameEn() != null ? category.getNameEn() : category.getNameBg();
        category.setSlug(generateUniqueSlugForVali(baseName, category));

        return category;
    }

    private String generateUniqueSlugForVali(String categoryName, Category category) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return "category-" + System.currentTimeMillis();
        }

        String baseSlug = syncHelper.createSlugFromName(categoryName);

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

    @Transactional
    public void syncParameters() {
        String syncType = "VALI_PARAMETERS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== Starting GLOBAL Parameters Synchronization ===");
            log.info("Strategy: Deduplicate by NORMALIZED NAME");

            List<Parameter> allExistingParams = parameterRepository.findAll();

            for (Parameter p : allExistingParams) {
                if (p.getCategories() != null) {
                    p.getCategories().size();
                }
            }

            Map<String, Parameter> globalParamsCache = allExistingParams.stream()
                    .filter(p -> p.getNameBg() != null)
                    .collect(Collectors.toMap(
                            p -> normalizeName(p.getNameBg()),
                            p -> p,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} existing parameters from database", globalParamsCache.size());

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

            List<Category> categories = categoryRepository.findAll();
            log.info("Processing parameters from {} categories", categories.size());

            Map<String, ParameterData> allParametersData = new HashMap<>();

            int categoryCounter = 0;
            for (Category category : categories) {
                if (category.getExternalId() == null) {
                    continue;
                }

                categoryCounter++;

                try {
                    List<ParameterRequestDto> apiParams = valiApiService
                            .getParametersByCategory(category.getExternalId());

                    if (apiParams == null || apiParams.isEmpty()) {
                        continue;
                    }

                    log.debug("[{}/{}] Category '{}': {} parameters from API",
                            categoryCounter, categories.size(), category.getNameBg(), apiParams.size());

                    for (ParameterRequestDto apiParam : apiParams) {
                        String nameBg = extractNameBg(apiParam);
                        if (nameBg == null || nameBg.isEmpty()) {
                            continue;
                        }

                        String normalizedName = normalizeName(nameBg);

                        ParameterData paramData = allParametersData.computeIfAbsent(
                                normalizedName,
                                k -> new ParameterData(apiParam, new HashSet<>())
                        );

                        paramData.categories.add(category);
                    }

                } catch (Exception e) {
                    log.error("Error fetching parameters for category {}: {}",
                            category.getExternalId(), e.getMessage());
                }
            }

            log.info("Collected {} unique parameters across all categories", allParametersData.size());

            long created = 0, reused = 0, optionsCreated = 0;

            for (Map.Entry<String, ParameterData> entry : allParametersData.entrySet()) {
                String normalizedName = entry.getKey();
                ParameterData paramData = entry.getValue();

                try {
                    Parameter parameter = globalParamsCache.get(normalizedName);

                    if (parameter == null) {
                        parameter = createParameterFromExternal(paramData.apiParam);
                        parameter.setCategories(new HashSet<>(paramData.categories));
                        parameter = parameterRepository.save(parameter);
                        globalParamsCache.put(normalizedName, parameter);
                        created++;

                        log.debug("✓ Created parameter: '{}' for {} categories",
                                parameter.getNameBg(), paramData.categories.size());
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

                    if (paramData.apiParam.getOptions() != null &&
                            !paramData.apiParam.getOptions().isEmpty()) {

                        int optionsForThisParam = createOptionsForParameter(
                                parameter,
                                paramData.apiParam.getOptions(),
                                globalOptionsCache
                        );

                        optionsCreated += optionsForThisParam;
                    }

                } catch (Exception e) {
                    log.error("Error processing parameter '{}': {}", normalizedName, e.getMessage());
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    allParametersData.size(), created, 0, 0,
                    String.format("Created: %d, Reused: %d, Options: %d", created, reused, optionsCreated),
                    startTime);

            log.info("=== Parameters Sync Completed ===");
            log.info("   Unique parameters: {}", allParametersData.size());
            log.info("   Created: {}, Reused: {}", created, reused);
            log.info("   Options created: {}", optionsCreated);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during parameters synchronization", e);
            throw new RuntimeException(e);
        }
    }

    private int createOptionsForParameter(Parameter parameter,
                                          List<ParameterOptionRequestDto> apiOptions,
                                          Map<String, ParameterOption> globalOptionsCache) {
        int created = 0;

        for (ParameterOptionRequestDto apiOption : apiOptions) {
            try {
                Long externalId = apiOption.getExternalId();

                if (externalId == null || externalId == 0) {
                    continue;
                }

                String nameBg = extractOptionNameBg(apiOption);
                if (nameBg == null || nameBg.isEmpty()) {
                    continue;
                }

                String cacheKey = buildOptionCacheKey(parameter.getNameBg(), nameBg);

                if (globalOptionsCache.containsKey(cacheKey)) {
                    continue;
                }

                ParameterOption option = new ParameterOption();
                option.setParameter(parameter);
                option.setExternalId(externalId);
                option.setNameBg(nameBg);
                option.setNameEn(extractOptionNameEn(apiOption));
                option.setOrder(apiOption.getOrder() != null ? apiOption.getOrder() : 0);

                option = parameterOptionRepository.save(option);
                globalOptionsCache.put(cacheKey, option);
                created++;

            } catch (Exception e) {
                log.error("Error creating option for parameter {}: {}", parameter.getNameBg(), e.getMessage());
            }
        }

        return created;
    }

    private static class ParameterData {
        ParameterRequestDto apiParam;
        Set<Category> categories;

        ParameterData(ParameterRequestDto apiParam, Set<Category> categories) {
            this.apiParam = apiParam;
            this.categories = categories;
        }
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String buildOptionCacheKey(String parameterName, String optionName) {
        return normalizeName(parameterName) + ":::" + normalizeName(optionName);
    }

    private String extractNameBg(ParameterRequestDto param) {
        if (param.getName() == null) return null;
        return param.getName().stream()
                .filter(n -> "bg".equals(n.getLanguageCode()))
                .map(n -> n.getText())
                .findFirst()
                .orElse(null);
    }

    private String extractOptionNameBg(ParameterOptionRequestDto option) {
        if (option.getName() == null) return null;
        return option.getName().stream()
                .filter(n -> "bg".equals(n.getLanguageCode()))
                .map(n -> n.getText())
                .findFirst()
                .orElse(null);
    }

    private String extractOptionNameEn(ParameterOptionRequestDto option) {
        if (option.getName() == null) return null;
        return option.getName().stream()
                .filter(n -> "en".equals(n.getLanguageCode()))
                .map(n -> n.getText())
                .findFirst()
                .orElse(null);
    }

    private Parameter createParameterFromExternal(ParameterRequestDto extParameter) {
        Parameter parameter = new Parameter();
        parameter.setExternalId(extParameter.getExternalId());
        parameter.setOrder(extParameter.getOrder());
        parameter.setPlatform(Platform.VALI);
        parameter.setCategories(new HashSet<>());
        parameter.setCreatedBy("system");

        if (extParameter.getName() != null) {
            extParameter.getName().forEach(name -> {
                if ("bg".equals(name.getLanguageCode())) {
                    parameter.setNameBg(name.getText());
                } else if ("en".equals(name.getLanguageCode())) {
                    parameter.setNameEn(name.getText());
                }
            });
        }

        return parameter;
    }

    @Transactional
    public void syncProducts() {
        String syncType = "VALI_PRODUCTS";
        log.info("Starting products synchronization - SIMPLIFIED mode");
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        long totalProcessed = 0, created = 0, updated = 0, errors = 0;

        try {
            Map<Long, Manufacturer> manufacturersMap = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getExternalId() != null)
                    .collect(Collectors.toMap(
                            Manufacturer::getExternalId,
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} manufacturers", manufacturersMap.size());

            if (manufacturersMap.isEmpty()) {
                log.error("⚠️ NO MANUFACTURERS FOUND! Run syncManufacturers() first!");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0,
                        "No manufacturers found", startTime);
                return;
            }

            Map<Long, Parameter> globalParametersCache = parameterRepository.findAll()
                    .stream()
                    .filter(p -> p.getExternalId() != null)
                    .collect(Collectors.toMap(
                            Parameter::getExternalId,
                            p -> p,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} parameters globally", globalParametersCache.size());

            Map<Long, ParameterOption> globalOptionsCache = parameterOptionRepository.findAll()
                    .stream()
                    .filter(o -> o.getExternalId() != null && o.getExternalId() != 0)
                    .collect(Collectors.toMap(
                            ParameterOption::getExternalId,
                            o -> o,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} options globally", globalOptionsCache.size());

            List<Category> categories = categoryRepository.findAll();
            log.info("Found {} categories to process", categories.size());

            int categoryCounter = 0;

            for (Category category : categories) {
                categoryCounter++;

                if (category.getExternalId() == null) {
                    continue;
                }

                try {
                    log.info("[{}/{}] Processing category: {} (externalId: {})",
                            categoryCounter, categories.size(), category.getNameBg(), category.getExternalId());

                    CategorySyncResult result = syncProductsByCategory(
                            category,
                            manufacturersMap,
                            globalParametersCache,
                            globalOptionsCache
                    );

                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;

                    log.info("Category {} completed - Processed: {}, Created: {}, Updated: {}, Errors: {}",
                            category.getNameBg(), result.processed, result.created, result.updated, result.errors);

                } catch (Exception e) {
                    log.error("Error processing products for category {}: {}",
                            category.getExternalId(), e.getMessage(), e);
                    errors++;
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, created, updated, errors,
                    errors > 0 ? String.format("Completed with %d errors", errors) : null, startTime);

            log.info("=== Products Sync Completed ===");
            log.info("   Total: {}, Created: {}, Updated: {}, Errors: {}",
                    totalProcessed, created, updated, errors);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, totalProcessed, created, updated, errors,
                    e.getMessage(), startTime);
            log.error("Error during products synchronization", e);
            throw new RuntimeException(e);
        }
    }

    private CategorySyncResult syncProductsByCategory(Category category,
                                                      Map<Long, Manufacturer> manufacturersMap,
                                                      Map<Long, Parameter> globalParametersCache,
                                                      Map<Long, ParameterOption> globalOptionsCache) {
        long totalProcessed = 0, created = 0, updated = 0, errors = 0;

        try {
            List<ProductRequestDto> allProducts = valiApiService.getProductsByCategory(category.getExternalId());

            if (allProducts.isEmpty()) {
                return new CategorySyncResult(0, 0, 0, 0);
            }

            log.info("Fetched {} products for category: {}", allProducts.size(), category.getNameBg());

            List<List<ProductRequestDto>> chunks = partitionList(allProducts, batchSize);

            for (int i = 0; i < chunks.size(); i++) {
                List<ProductRequestDto> chunk = chunks.get(i);

                try {
                    ChunkResult result = processProductsChunk(
                            chunk,
                            manufacturersMap,
                            category,
                            globalParametersCache,
                            globalOptionsCache
                    );

                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;

                    if (i < chunks.size() - 1) {
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    log.error("Error processing product chunk {}/{} for category {}: {}",
                            i + 1, chunks.size(), category.getExternalId(), e.getMessage(), e);
                    errors += chunk.size();
                }
            }

        } catch (Exception e) {
            log.error("Error getting products for category {}: {}", category.getExternalId(), e.getMessage(), e);
            errors++;
        }

        return new CategorySyncResult(totalProcessed, created, updated, errors);
    }

    private ChunkResult processProductsChunk(List<ProductRequestDto> products,
                                             Map<Long, Manufacturer> manufacturersMap,
                                             Category category,
                                             Map<Long, Parameter> globalParametersCache,
                                             Map<Long, ParameterOption> globalOptionsCache) {
        long processed = 0, created = 0, updated = 0, errors = 0;

        Set<Long> externalProductIdsInChunk = products.stream()
                .map(ProductRequestDto::getId)
                .collect(Collectors.toSet());

        Map<Long, Product> existingProductsMap = productRepository.findByExternalIdIn(externalProductIdsInChunk)
                .stream()
                .collect(Collectors.toMap(Product::getExternalId, p -> p));

        List<Product> productsToSave = new ArrayList<>();

        for (ProductRequestDto extProduct : products) {
            try {
                Manufacturer manufacturer = manufacturersMap.get(extProduct.getManufacturerId());
                if (manufacturer == null) {
                    log.warn("Manufacturer {} not found for product {}, skipping",
                            extProduct.getManufacturerId(), extProduct.getReferenceNumber());
                    errors++;
                    continue;
                }

                Product product;

                if (existingProductsMap.containsKey(extProduct.getId())) {
                    product = existingProductsMap.get(extProduct.getId());
                    updateProductFieldsFromExternal(product, extProduct, manufacturer, category,
                            globalParametersCache, globalOptionsCache);
                    updated++;
                } else {
                    product = new Product();
                    product.setId(null);
                    updateProductFieldsFromExternal(product, extProduct, manufacturer, category,
                            globalParametersCache, globalOptionsCache);
                    created++;
                }

                productsToSave.add(product);
                processed++;

            } catch (Exception e) {
                errors++;
                log.error("Error processing product {}: {}", extProduct.getReferenceNumber(), e.getMessage());
            }
        }

        if (!productsToSave.isEmpty()) {
            try {
                productRepository.saveAll(productsToSave);
            } catch (Exception e) {
                log.error("Error saving products batch: {}", e.getMessage(), e);
                errors += productsToSave.size();
            }
        }

        entityManager.flush();
        entityManager.clear();

        return new ChunkResult(processed, created, updated, errors);
    }

    private void updateProductFieldsFromExternal(Product product, ProductRequestDto extProduct,
                                                 Manufacturer manufacturer, Category category,
                                                 Map<Long, Parameter> globalParametersCache,
                                                 Map<Long, ParameterOption> globalOptionsCache) {
        boolean isNew = (product.getId() == null);

        if (isNew) {
            product.setExternalId(extProduct.getId());
            product.setWorkflowId(extProduct.getIdWF());
            product.setReferenceNumber(extProduct.getReferenceNumber());
            product.setModel(extProduct.getModel());
            product.setBarcode(extProduct.getBarcode());
            product.setManufacturer(manufacturer);
            product.setCategory(category);
            product.setWarranty(extProduct.getWarranty());
            product.setWeight(extProduct.getWeight());
            product.setPlatform(Platform.VALI);
            product.setShow(extProduct.getShow());

            setImagesToProduct(product, extProduct);
            setNamesToProduct(product, extProduct);
            setDescriptionToProduct(product, extProduct);

            try {
                setParametersToProduct(product, extProduct, globalParametersCache, globalOptionsCache);
            } catch (Exception e) {
                log.error("Error setting parameters for product {}: {}", extProduct.getReferenceNumber(), e.getMessage());
                product.setProductParameters(new HashSet<>());
            }
        }

        product.setStatus(ProductStatus.fromCode(extProduct.getStatus()));
        product.setPriceClient(extProduct.getPriceClient());
        product.setPricePartner(extProduct.getPricePartner());
        product.setPricePromo(extProduct.getPricePromo());
        product.setPriceClientPromo(extProduct.getPriceClientPromo());
        product.calculateFinalPrice();
        product.setCreatedBy("system");
    }

    private void setParametersToProduct(Product product, ProductRequestDto extProduct,
                                        Map<Long, Parameter> globalParametersCache,
                                        Map<Long, ParameterOption> globalOptionsCache) {
        if (extProduct.getParameters() == null || product.getCategory() == null) {
            product.setProductParameters(new HashSet<>());
            return;
        }

        Set<ProductParameter> existingProductParams = product.getProductParameters();
        if (existingProductParams == null) {
            existingProductParams = new HashSet<>();
        }

        Set<ProductParameter> manualParameters = existingProductParams.stream()
                .filter(pp -> pp.getParameter() != null)
                .filter(this::isManualParameterForVali)
                .collect(Collectors.toSet());

        Set<ProductParameter> autoParameters = new HashSet<>();
        int mappedCount = 0;
        int notFoundCount = 0;
        int skippedInvalidCount = 0;

        for (ParameterValueRequestDto paramValue : extProduct.getParameters()) {
            try {
                if (paramValue.getOptionId() == null || paramValue.getOptionId() == 0) {
                    skippedInvalidCount++;
                    continue;
                }

                Parameter parameter = globalParametersCache.get(paramValue.getParameterId());
                if (parameter == null) {
                    notFoundCount++;
                    continue;
                }

                ParameterOption option = globalOptionsCache.get(paramValue.getOptionId());
                if (option == null) {
                    notFoundCount++;
                    continue;
                }

                if (!option.getParameter().getId().equals(parameter.getId())) {
                    notFoundCount++;
                    continue;
                }

                ProductParameter pp = new ProductParameter();
                pp.setProduct(product);
                pp.setParameter(parameter);
                pp.setParameterOption(option);
                autoParameters.add(pp);

                mappedCount++;

            } catch (Exception e) {
                log.error("Error mapping parameter for product {}: {}", extProduct.getReferenceNumber(), e.getMessage());
                notFoundCount++;
            }
        }

        Set<ProductParameter> mergedParameters = new HashSet<>();
        mergedParameters.addAll(manualParameters);
        mergedParameters.addAll(autoParameters);

        product.setProductParameters(mergedParameters);

        if (notFoundCount > 0 || skippedInvalidCount > 0) {
            log.warn("Product {}: {} mapped, {} not found, {} invalid",
                    extProduct.getReferenceNumber(), mappedCount, notFoundCount, skippedInvalidCount);
        }
    }

    private boolean isManualParameterForVali(ProductParameter productParameter) {
        Parameter parameter = productParameter.getParameter();
        if (parameter == null) return false;

        boolean isDifferentPlatform = (parameter.getPlatform() == null ||
                parameter.getPlatform() != Platform.VALI);

        boolean isCreatedByAdmin = isAdminUser(parameter.getCreatedBy());
        boolean isModifiedByAdmin = isAdminUser(parameter.getLastModifiedBy());

        return isDifferentPlatform || isCreatedByAdmin || isModifiedByAdmin;
    }

    private boolean isAdminUser(String username) {
        if (username == null || username.isEmpty()) return false;
        return "ADMIN".equalsIgnoreCase(username.trim()) || "admin".equalsIgnoreCase(username.trim());
    }

    private static void setImagesToProduct(Product product, ProductRequestDto extProduct) {
        if (extProduct.getImages() != null && !extProduct.getImages().isEmpty()) {
            product.setPrimaryImageUrl(extProduct.getImages().get(0).getHref());

            List<String> additionalImages = extProduct.getImages().stream()
                    .skip(1)
                    .map(ImageDto::getHref)
                    .toList();

            if (product.getAdditionalImages() != null) {
                product.getAdditionalImages().clear();
                product.getAdditionalImages().addAll(additionalImages);
            } else {
                product.setAdditionalImages(new ArrayList<>(additionalImages));
            }
        } else {
            product.setPrimaryImageUrl(null);
            if (product.getAdditionalImages() != null) {
                product.getAdditionalImages().clear();
            } else {
                product.setAdditionalImages(new ArrayList<>());
            }
        }
    }

    private static void setNamesToProduct(Product product, ProductRequestDto extProduct) {
        if (extProduct.getName() != null) {
            extProduct.getName().forEach(name -> {
                if ("bg".equals(name.getLanguageCode())) {
                    product.setNameBg(name.getText());
                } else if ("en".equals(name.getLanguageCode())) {
                    product.setNameEn(name.getText());
                }
            });
        }
    }

    private static void setDescriptionToProduct(Product product, ProductRequestDto extProduct) {
        if (extProduct.getDescription() != null) {
            extProduct.getDescription().forEach(desc -> {
                if ("bg".equals(desc.getLanguageCode())) {
                    product.setDescriptionBg(desc.getText());
                } else if ("en".equals(desc.getLanguageCode())) {
                    product.setDescriptionEn(desc.getText());
                }
            });
        }
    }

    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }

    private static class CategorySyncResult {
        long processed, created, updated, errors;
        CategorySyncResult(long processed, long created, long updated, long errors) {
            this.processed = processed;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }
    }

    private static class ChunkResult {
        long processed, created, updated, errors;
        ChunkResult(long processed, long created, long updated, long errors) {
            this.processed = processed;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }
    }
}