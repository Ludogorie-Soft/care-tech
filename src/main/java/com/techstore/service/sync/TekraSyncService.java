package com.techstore.service.sync;

import com.techstore.entity.*;
import com.techstore.enums.Platform;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.*;
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

/**
 * TekraSyncService - FINAL VERSION
 *
 * СТРУКТУРА:
 * - Главна категория: "Видеонаблюдение" (TEKRA)
 *   ├── Камери
 *   │   ├── IP Камери
 *   │   └── Аналогови камери
 *   ├── Рекордери
 *   │   ├── NVR Рекордери
 *   │   └── DVR Рекордери
 *   ├── Твърди дискове
 *   ├── Кабели и аксесоари
 *   └── ...
 *
 * - Споделени категории: "Монитори и дисплеи" (от Vali/Most)
 */
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

    // ============================================
    // TEKRA CATEGORY MAPPING
    // ============================================

    private static final Map<String, String> TEKRA_CATEGORY_MAPPING;

    static {
        Map<String, String> map = new HashMap<>();

        // TEKRA категории (под "Видеонаблюдение")
        map.put("Камери", "Камери");
        map.put("IP камери", "Камери");
        map.put("Аналогови камери", "Камери");
        map.put("Куполни камери", "Камери");
        map.put("Булет камери", "Камери");
        map.put("PTZ камери", "Камери");
        map.put("Термални камери", "Камери");

        map.put("Рекордери", "Рекордери");
        map.put("NVR", "Рекордери");
        map.put("DVR", "Рекордери");
        map.put("Хибридни рекордери", "Рекордери");

        map.put("Кабели", "Кабели и аксесоари");
        map.put("Аксесоари", "Кабели и аксесоари");
        map.put("Конектори", "Кабели и аксесоари");
        map.put("Захранвания", "Кабели и аксесоари");

        map.put("PoE суичове", "Мрежово оборудване");
        map.put("Суичове", "Мрежово оборудване");

        map.put("Интеркоми", "Интеркоми и домофони");
        map.put("Домофони", "Интеркоми и домофони");
        map.put("IP домофони", "Интеркоми и домофони");

        map.put("Алармени системи", "Алармени системи");
        map.put("Сензори", "Алармени системи");
        map.put("Детектори", "Алармени системи");

        map.put("Контрол на достъп", "Контрол на достъп");
        map.put("Четци", "Контрол на достъп");
        map.put("Контролери", "Контрол на достъп");

        // СПОДЕЛЕНИ категории (от Vali/Most)
        map.put("Монитори и аксесоари", "Монитори и дисплеи");
        map.put("Монитори", "Монитори и дисплеи");
        map.put("Дисплеи", "Монитори и дисплеи");

        map.put("Твърди дискове", "Твърди дискове");
        map.put("HDD", "Твърди дискове");
        map.put("Дискове за видеонаблюдение", "Твърди дискове");

        TEKRA_CATEGORY_MAPPING = Collections.unmodifiableMap(map);
    }

    // ============================================
    // MINIMAL CATEGORY SYNC - Only main + basic subcategories
    // ============================================

    @Transactional
    public void ensureTekraCategories() {
        SyncLog syncLog = logHelper.createSyncLogSimple("TEKRA_CATEGORIES");
        long startTime = System.currentTimeMillis();

        try {
            log.info("=== Ensuring Tekra category structure ===");

            // 1. Създай главната категория "Видеонаблюдение"
            Category mainCategory = ensureMainCategory();

            // 2. Създай основните подкатегории
            ensureBasicSubcategories(mainCategory);

            // 3. Създай analog/digital субкатегориите
            ensureAnalogDigitalSubcategories();

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    10, 0, 0, 0, "Category structure ensured", startTime);

            log.info("✓ Tekra category structure complete");

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED,
                    0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error ensuring Tekra categories", e);
        }
    }

    private Category ensureMainCategory() {
        Optional<Category> existing = categoryRepository.findByNameBg("Видеонаблюдение");

        if (existing.isPresent()) {
            log.info("✓ Main category 'Видеонаблюдение' already exists");
            return existing.get();
        }

        Category mainCategory = new Category();
        mainCategory.setNameBg("Видеонаблюдение");
        mainCategory.setNameEn("Video Surveillance");
        mainCategory.setSlug("videonablyudenie");
        mainCategory.setPlatform(Platform.TEKRA);
        mainCategory.setShow(true);
        mainCategory.setSortOrder(1);
        mainCategory.setParent(null);

        mainCategory = categoryRepository.save(mainCategory);
        log.info("✓ Created main category: Видеонаблюдение (ID: {})", mainCategory.getId());

        return mainCategory;
    }

    private void ensureBasicSubcategories(Category mainCategory) {
        log.info("=== Ensuring basic subcategories under 'Видеонаблюдение' ===");

        // Камери
        if (!categoryRepository.findByNameBg("Камери").isPresent()) {
            Category cameras = new Category();
            cameras.setNameBg("Камери");
            cameras.setNameEn("Cameras");
            cameras.setSlug(syncHelper.createSlugFromName("Камери"));
            cameras.setParent(mainCategory);
            cameras.setPlatform(Platform.TEKRA);
            cameras.setShow(true);
            cameras.setSortOrder(1);
            cameras.setCategoryPath(cameras.generateCategoryPath());
            categoryRepository.save(cameras);
            log.info("✓ Created: Камери");
        }

        // Рекордери
        if (!categoryRepository.findByNameBg("Рекордери").isPresent()) {
            Category recorders = new Category();
            recorders.setNameBg("Рекордери");
            recorders.setNameEn("Recorders");
            recorders.setSlug(syncHelper.createSlugFromName("Рекордери"));
            recorders.setParent(mainCategory);
            recorders.setPlatform(Platform.TEKRA);
            recorders.setShow(true);
            recorders.setSortOrder(2);
            recorders.setCategoryPath(recorders.generateCategoryPath());
            categoryRepository.save(recorders);
            log.info("✓ Created: Рекордери");
        }

        // Кабели и аксесоари
        if (!categoryRepository.findByNameBg("Кабели и аксесоари").isPresent()) {
            Category cables = new Category();
            cables.setNameBg("Кабели и аксесоари");
            cables.setNameEn("Cables & Accessories");
            cables.setSlug(syncHelper.createSlugFromName("Кабели и аксесоари"));
            cables.setParent(mainCategory);
            cables.setPlatform(Platform.TEKRA);
            cables.setShow(true);
            cables.setSortOrder(3);
            cables.setCategoryPath(cables.generateCategoryPath());
            categoryRepository.save(cables);
            log.info("✓ Created: Кабели и аксесоари");
        }

        // Мрежово оборудване
        if (!categoryRepository.findByNameBg("Мрежово оборудване").isPresent()) {
            Category network = new Category();
            network.setNameBg("Мрежово оборудване");
            network.setNameEn("Network Equipment");
            network.setSlug(syncHelper.createSlugFromName("Мрежово оборудване"));
            network.setParent(mainCategory);
            network.setPlatform(Platform.TEKRA);
            network.setShow(true);
            network.setSortOrder(4);
            network.setCategoryPath(network.generateCategoryPath());
            categoryRepository.save(network);
            log.info("✓ Created: Мрежово оборудване");
        }

        // Интеркоми и домофони
        if (!categoryRepository.findByNameBg("Интеркоми и домофони").isPresent()) {
            Category intercoms = new Category();
            intercoms.setNameBg("Интеркоми и домофони");
            intercoms.setNameEn("Intercoms & Doorbells");
            intercoms.setSlug(syncHelper.createSlugFromName("Интеркоми и домофони"));
            intercoms.setParent(mainCategory);
            intercoms.setPlatform(Platform.TEKRA);
            intercoms.setShow(true);
            intercoms.setSortOrder(5);
            intercoms.setCategoryPath(intercoms.generateCategoryPath());
            categoryRepository.save(intercoms);
            log.info("✓ Created: Интеркоми и домофони");
        }

        // Алармени системи
        if (!categoryRepository.findByNameBg("Алармени системи").isPresent()) {
            Category alarms = new Category();
            alarms.setNameBg("Алармени системи");
            alarms.setNameEn("Alarm Systems");
            alarms.setSlug(syncHelper.createSlugFromName("Алармени системи"));
            alarms.setParent(mainCategory);
            alarms.setPlatform(Platform.TEKRA);
            alarms.setShow(true);
            alarms.setSortOrder(6);
            alarms.setCategoryPath(alarms.generateCategoryPath());
            categoryRepository.save(alarms);
            log.info("✓ Created: Алармени системи");
        }

        // Контрол на достъп
        if (!categoryRepository.findByNameBg("Контрол на достъп").isPresent()) {
            Category accessControl = new Category();
            accessControl.setNameBg("Контрол на достъп");
            accessControl.setNameEn("Access Control");
            accessControl.setSlug(syncHelper.createSlugFromName("Контрол на достъп"));
            accessControl.setParent(mainCategory);
            accessControl.setPlatform(Platform.TEKRA);
            accessControl.setShow(true);
            accessControl.setSortOrder(7);
            accessControl.setCategoryPath(accessControl.generateCategoryPath());
            categoryRepository.save(accessControl);
            log.info("✓ Created: Контрол на достъп");
        }
    }

    private void ensureAnalogDigitalSubcategories() {
        log.info("=== Ensuring analog/digital subcategories ===");

        Optional<Category> camerasOpt = categoryRepository.findByNameBg("Камери");
        if (camerasOpt.isEmpty()) {
            log.warn("⚠️ Parent category 'Камери' not found!");
            return;
        }
        Category camerasParent = camerasOpt.get();

        Optional<Category> recordersOpt = categoryRepository.findByNameBg("Рекордери");
        if (recordersOpt.isEmpty()) {
            log.warn("⚠️ Parent category 'Рекордери' not found!");
            return;
        }
        Category recordersParent = recordersOpt.get();

        // IP Камери
        if (!categoryRepository.findByNameBg("IP Камери").isPresent()) {
            Category ipCameras = new Category();
            ipCameras.setNameBg("IP Камери");
            ipCameras.setNameEn("IP Cameras");
            ipCameras.setSlug(syncHelper.createSlugFromName("IP Камери"));
            ipCameras.setParent(camerasParent);
            ipCameras.setPlatform(Platform.TEKRA);
            ipCameras.setShow(true);
            ipCameras.setSortOrder(1);
            ipCameras.setCategoryPath(ipCameras.generateCategoryPath());
            categoryRepository.save(ipCameras);
            log.info("✓ Created: IP Камери");
        }

        // Аналогови камери
        if (!categoryRepository.findByNameBg("Аналогови камери").isPresent()) {
            Category analogCameras = new Category();
            analogCameras.setNameBg("Аналогови камери");
            analogCameras.setNameEn("Analog Cameras");
            analogCameras.setSlug(syncHelper.createSlugFromName("Аналогови камери"));
            analogCameras.setParent(camerasParent);
            analogCameras.setPlatform(Platform.TEKRA);
            analogCameras.setShow(true);
            analogCameras.setSortOrder(2);
            analogCameras.setCategoryPath(analogCameras.generateCategoryPath());
            categoryRepository.save(analogCameras);
            log.info("✓ Created: Аналогови камери");
        }

        // NVR Рекордери
        if (!categoryRepository.findByNameBg("NVR Рекордери").isPresent()) {
            Category nvrRecorders = new Category();
            nvrRecorders.setNameBg("NVR Рекордери");
            nvrRecorders.setNameEn("NVR Recorders");
            nvrRecorders.setSlug(syncHelper.createSlugFromName("NVR Рекордери"));
            nvrRecorders.setParent(recordersParent);
            nvrRecorders.setPlatform(Platform.TEKRA);
            nvrRecorders.setShow(true);
            nvrRecorders.setSortOrder(1);
            nvrRecorders.setCategoryPath(nvrRecorders.generateCategoryPath());
            categoryRepository.save(nvrRecorders);
            log.info("✓ Created: NVR Рекордери");
        }

        // DVR Рекордери
        if (!categoryRepository.findByNameBg("DVR Рекордери").isPresent()) {
            Category dvrRecorders = new Category();
            dvrRecorders.setNameBg("DVR Рекордери");
            dvrRecorders.setNameEn("DVR Recorders");
            dvrRecorders.setSlug(syncHelper.createSlugFromName("DVR Рекордери"));
            dvrRecorders.setParent(recordersParent);
            dvrRecorders.setPlatform(Platform.TEKRA);
            dvrRecorders.setShow(true);
            dvrRecorders.setSortOrder(2);
            dvrRecorders.setCategoryPath(dvrRecorders.generateCategoryPath());
            categoryRepository.save(dvrRecorders);
            log.info("✓ Created: DVR Рекордери");
        }
    }

    @Transactional
    public void syncTekraManufacturers() {
        SyncLog syncLog = logHelper.createSyncLogSimple("TEKRA_MANUFACTURERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Tekra manufacturers synchronization");

            Set<String> targetCategoryNames = new HashSet<>(TEKRA_CATEGORY_MAPPING.values());

            List<Category> tekraCategories = categoryRepository.findAll().stream()
                    .filter(cat -> targetCategoryNames.contains(cat.getNameBg()))
                    .toList();

            if (tekraCategories.isEmpty()) {
                log.warn("No target categories found");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No target categories", startTime);
                return;
            }

            Set<String> allTekraManufacturers = new HashSet<>();

            List<Map<String, Object>> allCategoriesRaw = tekraApiService.getCategoriesRaw();

            for (Map<String, Object> categoryRaw : allCategoriesRaw) {
                try {
                    String tekraSlug = getString(categoryRaw, "slug");
                    if (tekraSlug == null) continue;

                    Set<String> categoryManufacturers = tekraApiService
                            .extractTekraManufacturersFromProducts(tekraSlug);
                    allTekraManufacturers.addAll(categoryManufacturers);
                } catch (Exception e) {
                    log.error("Error extracting manufacturers: {}", e.getMessage());
                }
            }

            if (allTekraManufacturers.isEmpty()) {
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
                            (existing, duplicate) -> existing
                    ));

            long created = 0, updated = 0;

            for (String manufacturerName : allTekraManufacturers) {
                try {
                    String normalized = normalizeManufacturerName(manufacturerName);
                    Manufacturer manufacturer = existingManufacturers.get(normalized);

                    if (manufacturer == null) {
                        manufacturer = new Manufacturer();
                        manufacturer.setName(manufacturerName);
                        manufacturer.setPlatform(Platform.TEKRA);
                        manufacturer = manufacturerRepository.save(manufacturer);
                        existingManufacturers.put(normalized, manufacturer);
                        created++;
                    } else {
                        updated++;
                    }
                } catch (Exception e) {
                    log.error("Error processing manufacturer: {}", e.getMessage());
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, (long) allTekraManufacturers.size(),
                    created, updated, 0, null, startTime);

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

            Map<String, Parameter> globalParamsCache = allExistingParams.stream()
                    .filter(p -> p.getNameBg() != null)
                    .collect(Collectors.toMap(
                            p -> normalizeName(p.getNameBg()),
                            p -> p,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} existing parameters", globalParamsCache.size());

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

            log.info("Loaded {} existing options", globalOptionsCache.size());

            Set<String> targetCategoryNames = new HashSet<>(TEKRA_CATEGORY_MAPPING.values());
            Map<String, Category> targetCategoriesMap = categoryRepository.findAll().stream()
                    .filter(cat -> targetCategoryNames.contains(cat.getNameBg()))
                    .collect(Collectors.toMap(
                            Category::getNameBg,
                            cat -> cat,
                            (existing, duplicate) -> existing
                    ));

            Map<String, ParameterData> allParametersData = new HashMap<>();
            Set<String> processedSkus = new HashSet<>();

            List<Map<String, Object>> allCategoriesRaw = tekraApiService.getCategoriesRaw();

            for (Map<String, Object> categoryRaw : allCategoriesRaw) {
                try {
                    String tekraSlug = getString(categoryRaw, "slug");
                    String tekraCategoryName = getString(categoryRaw, "name");

                    if (tekraSlug == null || tekraCategoryName == null) continue;

                    String targetCategoryName = TEKRA_CATEGORY_MAPPING.get(tekraCategoryName);
                    if (targetCategoryName == null) {
                        continue;
                    }

                    Category targetCategory = targetCategoriesMap.get(targetCategoryName);
                    if (targetCategory == null) {
                        log.warn("Target category '{}' not found", targetCategoryName);
                        continue;
                    }

                    List<Map<String, Object>> products = tekraApiService.getProductsRaw(tekraSlug);

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
                            String normalizedName = normalizeName(paramName);

                            ParameterData paramData = allParametersData.computeIfAbsent(
                                    normalizedName,
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

                            paramData.categories.add(targetCategory);
                            paramData.values.add(paramValue);
                        }
                    }

                } catch (Exception e) {
                    log.error("Error processing category: {}", e.getMessage());
                }
            }

            log.info("Collected {} unique parameters", allParametersData.size());

            long created = 0, reused = 0, optionsCreated = 0;

            for (Map.Entry<String, ParameterData> entry : allParametersData.entrySet()) {
                String normalizedName = entry.getKey();
                ParameterData paramData = entry.getValue();

                try {
                    Parameter parameter = globalParamsCache.get(normalizedName);

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
                        globalParamsCache.put(normalizedName, parameter);
                        created++;
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
                    log.error("Error processing parameter '{}': {}", normalizedName, e.getMessage());
                }
            }

            String message = String.format("Parameters: %d created, %d reused. Options: %d created",
                    created, reused, optionsCreated);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    allParametersData.size(), created, 0, 0, message, startTime);

            log.info("=== Tekra Parameters Sync Completed ===");

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
            log.info("=== STARTING Tekra products synchronization ===");

            fixDuplicateProducts();

            Map<String, Manufacturer> manufacturersMap = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            List<Parameter> allParameters = parameterRepository.findAll().stream()
                    .filter(p -> p.getPlatform() == Platform.TEKRA || p.getTekraKey() != null)
                    .toList();

            Map<String, Parameter> parametersByTekraKey = new HashMap<>();
            Map<String, Parameter> parametersByNormalizedName = new HashMap<>();

            for (Parameter p : allParameters) {
                if (p.getTekraKey() != null) {
                    parametersByTekraKey.put(p.getTekraKey(), p);
                }
                if (p.getNameBg() != null) {
                    parametersByNormalizedName.put(normalizeName(p.getNameBg()), p);
                }
            }

            Map<Long, Map<String, ParameterOption>> optionsByParameterId = new HashMap<>();

            List<ParameterOption> allOptions = parameterOptionRepository.findAll();
            for (ParameterOption option : allOptions) {
                if (option.getParameter() != null && option.getNameBg() != null) {
                    optionsByParameterId
                            .computeIfAbsent(option.getParameter().getId(), k -> new HashMap<>())
                            .put(normalizeName(option.getNameBg()), option);
                }
            }

            Set<String> targetCategoryNames = new HashSet<>(TEKRA_CATEGORY_MAPPING.values());
            Map<String, Category> categoriesByName = categoryRepository.findAll().stream()
                    .filter(cat -> targetCategoryNames.contains(cat.getNameBg()))
                    .collect(Collectors.toMap(
                            cat -> cat.getNameBg().toLowerCase(),
                            cat -> cat,
                            (existing, duplicate) -> existing
                    ));

            List<Map<String, Object>> allProducts = new ArrayList<>();
            Set<String> processedSkus = new HashSet<>();

            List<Map<String, Object>> allCategoriesRaw = tekraApiService.getCategoriesRaw();

            for (Map<String, Object> categoryRaw : allCategoriesRaw) {
                try {
                    String tekraSlug = getString(categoryRaw, "slug");
                    if (tekraSlug == null) continue;

                    List<Map<String, Object>> categoryProducts = tekraApiService.getProductsRaw(tekraSlug);

                    for (Map<String, Object> product : categoryProducts) {
                        String sku = getString(product, "sku");
                        if (sku != null && !processedSkus.contains(sku)) {
                            allProducts.add(product);
                            processedSkus.add(sku);
                        }
                    }

                } catch (Exception e) {
                    log.error("Error fetching products: {}", e.getMessage());
                }
            }

            log.info("Collected {} unique products", allProducts.size());

            if (allProducts.isEmpty()) {
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0,
                        "No products found", startTime);
                return;
            }

            long totalProcessed = 0, totalCreated = 0, totalUpdated = 0, totalErrors = 0;
            long skippedNoCategory = 0, skippedNoManufacturer = 0;
            long analogCount = 0, digitalCount = 0, undeterminedCount = 0;

            for (int i = 0; i < allProducts.size(); i++) {
                Map<String, Object> rawProduct = allProducts.get(i);

                try {
                    String sku = getString(rawProduct, "sku");
                    String name = getString(rawProduct, "name");

                    if (sku == null || name == null) {
                        totalErrors++;
                        continue;
                    }

                    String tekraCategoryName = extractTekraCategory(rawProduct);
                    if (tekraCategoryName == null) {
                        skippedNoCategory++;
                        continue;
                    }

                    String targetCategoryName = TEKRA_CATEGORY_MAPPING.get(tekraCategoryName);
                    if (targetCategoryName == null) {
                        skippedNoCategory++;
                        continue;
                    }

                    Category productCategory = categoriesByName.get(targetCategoryName.toLowerCase());
                    if (productCategory == null) {
                        skippedNoCategory++;
                        continue;
                    }

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
                                    parametersByNormalizedName,
                                    optionsByParameterId
                            );
                            product = productRepository.save(product);
                        } catch (Exception e) {
                            log.error("ERROR setting parameters: {}", e.getMessage());
                        }
                    }

                    if (isNew) {
                        totalCreated++;
                    } else {
                        totalUpdated++;
                    }

                    totalProcessed++;

                    if (totalProcessed % 50 == 0) {
                        log.info("Progress: {}/{}", totalProcessed, allProducts.size());
                    }

                    if (totalProcessed % 100 == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    totalErrors++;
                    log.error("Error processing product: {}", e.getMessage());
                }
            }

            log.info("=== STATISTICS ===");
            log.info("Digital (IP): {}, Analog: {}, Undetermined: {}",
                    digitalCount, analogCount, undeterminedCount);

            String message = String.format(
                    "Total: %d, Created: %d, Updated: %d, Digital: %d, Analog: %d",
                    totalProcessed, totalCreated, totalUpdated, digitalCount, analogCount
            );

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, totalCreated,
                    totalUpdated, totalErrors, message, startTime);

            log.info("=== COMPLETE ===");

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("=== FAILED ===", e);
            throw e;
        }
    }

    private String extractTekraCategory(Map<String, Object> rawProduct) {
        String cat3 = getString(rawProduct, "category_3");
        if (cat3 != null && !"null".equalsIgnoreCase(cat3)) {
            return cat3;
        }

        String cat2 = getString(rawProduct, "category_2");
        if (cat2 != null && !"null".equalsIgnoreCase(cat2)) {
            return cat2;
        }

        String cat1 = getString(rawProduct, "category_1");
        if (cat1 != null && !"null".equalsIgnoreCase(cat1)) {
            return cat1;
        }

        return null;
    }

    private String determineProductType(Map<String, Object> rawProduct) {
        try {
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

            String productName = getString(rawProduct, "name");
            if (productName != null) {
                String lowerName = productName.toLowerCase();

                if (lowerName.contains(" ip ") ||
                        lowerName.contains("ip камера") ||
                        lowerName.contains("nvr")) {
                    return "IP";
                }

                if (lowerName.contains("ahd") ||
                        lowerName.contains("tvi") ||
                        lowerName.contains("cvi") ||
                        lowerName.contains("dvr")) {
                    return "Analog";
                }
            }

            return null;

        } catch (Exception e) {
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

            return (targetCategory != null) ? targetCategory : baseCategory;

        } catch (Exception e) {
            return baseCategory;
        }
    }

    private void setTekraParametersToProductSimplified(
            Product product,
            Map<String, Object> rawProduct,
            Map<String, Parameter> parametersByTekraKey,
            Map<String, Parameter> parametersByNormalizedName,
            Map<Long, Map<String, ParameterOption>> optionsByParameterId) {

        try {
            if (product.getCategory() == null) return;

            Set<ProductParameter> existingProductParams = product.getProductParameters();
            if (existingProductParams == null) {
                existingProductParams = new HashSet<>();
            }

            Set<ProductParameter> manualParameters = existingProductParams.stream()
                    .filter(pp -> pp.getParameter() != null)
                    .filter(this::isManualParameter)
                    .collect(Collectors.toSet());

            Set<ProductParameter> autoParameters = new HashSet<>();

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
                        String parameterName = convertTekraParameterKeyToName(parameterKey);
                        String normalizedName = normalizeName(parameterName);
                        parameter = parametersByNormalizedName.get(normalizedName);
                    }

                    if (parameter == null) continue;

                    Map<String, ParameterOption> parameterOptions = optionsByParameterId.get(parameter.getId());
                    if (parameterOptions == null) continue;

                    String normalizedValue = normalizeName(parameterValue);
                    ParameterOption option = parameterOptions.get(normalizedValue);

                    if (option == null) continue;

                    ProductParameter productParam = new ProductParameter();
                    productParam.setProduct(product);
                    productParam.setParameter(parameter);
                    productParam.setParameterOption(option);
                    autoParameters.add(productParam);

                } catch (Exception e) {
                    // Silent skip
                }
            }

            Set<ProductParameter> mergedParameters = new HashSet<>();
            mergedParameters.addAll(manualParameters);
            mergedParameters.addAll(autoParameters);

            product.setProductParameters(mergedParameters);

        } catch (Exception e) {
            log.error("ERROR setting parameters: {}", e.getMessage());
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
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
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

            if (value == null || key == null || key.trim().isEmpty()) continue;
            if (systemFields.contains(key)) continue;
            if (value instanceof List || value instanceof Map) continue;

            String stringValue = value.toString().trim();

            if (stringValue.isEmpty() || "null".equalsIgnoreCase(stringValue)) continue;
            if (stringValue.startsWith("http://") || stringValue.startsWith("https://")) continue;
            if (stringValue.length() > 200) continue;

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
        if (key.contains("http") || value.contains("http")) return false;
        if (key.contains("/") || value.contains("/uploads/")) return false;
        if (key.length() > 50 || key.contains(" ")) return false;
        if (value.matches("^\\d+(\\.\\d+)?$")) return true;
        if (value.length() < 100 && !value.contains("\n")) return true;
        return false;
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
        return (value == null) ? null : value.toString();
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
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
        if (value == null) return null;
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