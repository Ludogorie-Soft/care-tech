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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    @Autowired
    @Lazy
    private MostSyncService self;

    private static final String USD_TO_BGN_RATE = "1.80";
    private static final String EUR_TO_BGN_RATE = "1.95583";

    // Products whose MOST category maps to "Лаптопи" but whose name reveals they are accessories.
    // Checked in order — first match wins.
    private static final List<String[]> LAPTOP_CATEGORY_NAME_OVERRIDES;

    static {
        // Each entry: { namePattern (lowercase), targetCategoryNameBg }
        // Order matters — first match wins. More specific patterns come before general ones.
        List<String[]> overrides = new ArrayList<>();

        // ── Tablets → Таблети ───────────────────────────────────────────────
        // Must come first — LENOVO TAB / REALME PAD are NOTEBOOK-mapped but are tablets
        overrides.add(new String[]{"lenovo tab",     "Таблети"});
        overrides.add(new String[]{"realme pad",     "Таблети"});
        overrides.add(new String[]{"samsung tab",    "Таблети"});
        overrides.add(new String[]{"tablet",         "Таблети"});

        // ── Bags / backpacks / sleeves → Чанти за лаптопи ──────────────────
        overrides.add(new String[]{"backpack",       "Чанти за лаптопи"});
        overrides.add(new String[]{"bagpack",        "Чанти за лаптопи"});
        overrides.add(new String[]{"back pack",      "Чанти за лаптопи"});
        overrides.add(new String[]{" bp",            "Чанти за лаптопи"});
        overrides.add(new String[]{"carry case",     "Чанти за лаптопи"});
        overrides.add(new String[]{"carry bag",      "Чанти за лаптопи"});
        overrides.add(new String[]{"laptop bag",     "Чанти за лаптопи"});
        overrides.add(new String[]{"notebook bag",   "Чанти за лаптопи"});
        overrides.add(new String[]{"shoulder bag",   "Чанти за лаптопи"});
        overrides.add(new String[]{"sling bag",      "Чанти за лаптопи"});
        overrides.add(new String[]{"slng bag",       "Чанти за лаптопи"});
        overrides.add(new String[]{"shoulder",       "Чанти за лаптопи"});
        overrides.add(new String[]{"sling",          "Чанти за лаптопи"});
        overrides.add(new String[]{" bag",           "Чанти за лаптопи"});
        overrides.add(new String[]{"t210",           "Чанти за лаптопи"});
        overrides.add(new String[]{"sleeve",         "Чанти за лаптопи"});
        overrides.add(new String[]{"rolltop",        "Чанти за лаптопи"});
        overrides.add(new String[]{"чанта",          "Чанти за лаптопи"});
        overrides.add(new String[]{"раница",         "Чанти за лаптопи"});

        // ── Covers / cases / docks / skins / accessories → Аксесоари ───────
        overrides.add(new String[]{"cover",          "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"tricover",       "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"versasleave",    "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"versasleeve",    "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"magsmart",       "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"protective case","Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"portfolio case", "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"case",           "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"folio",          "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"simpro",         "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"protect film",   "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"screen protector","Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"docking",        "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"dock station",   "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"hybrid dock",    "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"dock",           "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"dongle",         "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"skin",           "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"disney",         "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"pocket",         "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"iconia",         "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"yoga pen",       "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"stylus",         "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"stand",          "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"hub",            "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"starter kit",    "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"стикер",         "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"калъф",          "Аксесоари за лаптопи/таблети"});
        overrides.add(new String[]{"кейс",           "Аксесоари за лаптопи/таблети"});

        // ── Chargers / adapters → Зарядни за лаптопи ────────────────────────
        // "adap" covers: adapter, adaptr, adapte, adapt — all truncations/misspellings
        overrides.add(new String[]{"charger",        "Зарядни за лаптопи"});
        overrides.add(new String[]{"charging",       "Зарядни за лаптопи"});
        overrides.add(new String[]{"adap",           "Зарядни за лаптопи"});
        overrides.add(new String[]{"зарядно",        "Зарядни за лаптопи"});
        overrides.add(new String[]{"адаптер",        "Зарядни за лаптопи"});

        LAPTOP_CATEGORY_NAME_OVERRIDES = Collections.unmodifiableList(overrides);
    }

    private static final Map<String, String> MOST_CATEGORY_MAPPING;

    static {
        Map<String, String> map = new HashMap<>();

        // ── Нови категории от portal.mostbg.com ──────────────────────────
        map.put("MAIN BOARD",   "Дънни платки");
        map.put("MONITOR",      "Монитори");
        map.put("PRINTER",      "Принтери, скенери и консумативи");
        map.put("LAN",          "Мрежов хардуер");
        map.put("NOTEBOOK",     "Лаптопи");
        map.put("M - MEDIA",    "Мултимедиен хардуер");
        map.put("CARTRIDGE",    "Консумативи(тонери) за лазерни устройства");
        map.put("CD MEDIA",     "Носители CD, DVD, Blu-Ray");
        map.put("GAMES",        "Игри и мултимедия");
        map.put("GSM",          "Мобилни телефони");
        map.put("COMPUTER",     "Настолни компютри");
        map.put("ALL-IN-ONE",   "Компютър всичко в едно");

        // ── Категории, открити при разширено сканиране ───────────────────
        // (добавят се при всеки нов unmapped тип от логовете)
        map.put("CPU",          "Процесори");
        map.put("VIDEO CARD",   "Видео карти");
        map.put("MEMORY",       "Памети");
        map.put("MOTHERBOARD",  "Дънни платки");
        map.put("SSD",          "SSD");
        map.put("HDD",          "Твърди дискове");
        map.put("POWER SUPPLY", "Захранвания");
        map.put("CASE",         "Кутии за компютри");
        map.put("COOLING",      "Охладители");
        map.put("UPS",          "Непрекъсваеми токозахранващи устройства");
        map.put("HEADSET",      "Слушалки");
        map.put("KEYBOARD",     "Клавиатури");
        map.put("MOUSE",        "Мишки");
        map.put("WEBCAM",       "Уеб камери");
        map.put("WEB CAMERA",   "Уеб камери");
        map.put("SPEAKER",      "Звукови системи и тонколони");
        map.put("TABLET",       "Таблети");
        map.put("NAS",          "Настолен NAS");
        map.put("ACCESSORY",    "Аксесоари");

        // ── Категории от unmapped лог (добавени след първи sync) ────────────
        map.put("RAM",          "Памети");
        map.put("FAN",          "Охладители");
        map.put("SCANER",       "Принтери, скенери и консумативи");
        map.put("CD",           "Носители CD, DVD, Blu-Ray");
        map.put("HP",           "Консумативи(тонери) за лазерни устройства");
        map.put("HP_",          "Консумативи(тонери) за лазерни устройства");
        map.put("FLASH",        "Флаш памети");
        map.put("CONTR I/O",    "Входно-изходни контролери");
        map.put("CAMERA",       "Аксесоари");

        // ── Мрежово оборудване ─────────────────────────────────────────────
        map.put("ROUTER",       "Мрежов хардуер");
        map.put("SWITCH",       "Мрежов хардуер");
        map.put("WIRELESS",     "Мрежов хардуер");
        map.put("ACCESS POINT", "Мрежов хардуер");
        map.put("AP",           "Мрежов хардуер");
        map.put("MODEM",        "Мрежов хардуер");
        map.put("FIREWALL",     "Мрежов хардуер");
        map.put("NETWORK",      "Мрежов хардуер");

        // ── Съхранение ─────────────────────────────────────────────────────
        map.put("EXTERNAL HDD", "Твърди дискове");
        map.put("EXT HDD",      "Твърди дискове");
        map.put("EXTERNAL SSD", "SSD");
        map.put("EXT SSD",      "SSD");
        map.put("FLASH DRIVE",  "Флаш памети");
        map.put("USB FLASH",    "Флаш памети");
        map.put("PENDRIVE",     "Флаш памети");
        map.put("MEMORY CARD",  "Флаш памети");
        map.put("SD CARD",      "Флаш памети");
        map.put("NAS DRIVE",    "Настолен NAS");

        // ── Принтери и сканери ─────────────────────────────────────────────
        map.put("SCANNER",      "Принтери, скенери и консумативи");
        map.put("MFP",          "Принтери, скенери и консумативи");
        map.put("MFD",          "Принтери, скенери и консумативи");
        map.put("LABEL",        "Принтери, скенери и консумативи");
        map.put("INK",          "Консумативи(тонери) за лазерни устройства");
        map.put("TONER",        "Консумативи(тонери) за лазерни устройства");
        map.put("DRUM",         "Консумативи(тонери) за лазерни устройства");

        // ── Геймърска периферия ────────────────────────────────────────────
        map.put("GAMING MOUSE",     "Геймърски мишки");
        map.put("GAMING KEYBOARD",  "Геймърски клавиатури");
        map.put("GAMING HEADSET",   "Геймърски слушалки");
        map.put("GAMING CHAIR",     "Геймърска периферия");
        map.put("GAMEPAD",          "Геймърска периферия");
        map.put("JOYSTICK",         "Геймърска периферия");
        map.put("GAME CONTROLLER",  "Геймърска периферия");
        map.put("MOUSEPAD",         "Геймърска периферия");
        map.put("GAMING MOUSEPAD",  "Геймърска периферия");

        // ── Аксесоари ──────────────────────────────────────────────────────
        map.put("CABLE",        "Аксесоари");
        map.put("CABLES",       "Аксесоари");
        map.put("USB HUB",      "Аксесоари");
        map.put("HUB",          "Аксесоари");
        map.put("CHARGER",      "Аксесоари");
        map.put("ADAPTER",      "Аксесоари");
        map.put("DOCKING",      "Аксесоари");
        map.put("DOCK",         "Аксесоари");
        map.put("PROJECTOR",    "Аксесоари");
        map.put("SMART WATCH",  "Аксесоари");
        map.put("SMARTWATCH",   "Аксесоари");
        map.put("WEARABLE",     "Аксесоари");
        map.put("LAPTOP BAG",   "Аксесоари");
        map.put("BAG",          "Аксесоари");
        map.put("COVER",        "Аксесоари");
        map.put("STAND",        "Аксесоари");

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

            // Fallback map by nameBg (cross-platform reuse)
            Map<String, Parameter> existingByNormalizedNameBg = new HashMap<>();
            for (Parameter p : allExistingParams) {
                if (p.getNameBg() != null && p.getMostKey() == null) {
                    existingByNormalizedNameBg.put(p.getNameBg().toLowerCase().trim(), p);
                }
            }

            // Fallback map by nameEn — Most API property names are in English,
            // so match against existing parameters' English name to avoid duplicates
            Map<String, Parameter> existingByNormalizedNameEn = new HashMap<>();
            for (Parameter p : allExistingParams) {
                if (p.getNameEn() != null && p.getMostKey() == null
                        && !existingByNormalizedNameBg.containsValue(p)) {
                    existingByNormalizedNameEn.put(p.getNameEn().toLowerCase().trim(), p);
                }
            }

            log.info("Loaded {} existing parameters with mostKey from database", globalParamsCache.size());

            // Guard: categoryId → (normalizedNameBg → parameterId)
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

            List<ParameterOption> allExistingOptions = parameterOptionRepository.findAll();

            Map<String, ParameterOption> globalOptionsCache = new HashMap<>();
            for (ParameterOption option : allExistingOptions) {
                if (option.getParameter() != null &&
                        option.getParameter().getId() != null &&
                        option.getNameBg() != null) {
                    globalOptionsCache.put(
                            buildOptionCacheKey(option.getParameter().getId(), option.getNameBg()),
                            option);
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

                    // Resolve category — if unmapped or missing, still collect parameters
                    Category category = null;
                    String targetCategoryName = MOST_CATEGORY_MAPPING.get(categoryName);
                    if (targetCategoryName == null) {
                        log.debug("Unknown Most category '{}' — collecting parameters without category", categoryName);
                    } else {
                        Optional<Category> categoryOpt = categoryRepository.findByNameBg(targetCategoryName);
                        if (categoryOpt.isPresent()) {
                            category = categoryOpt.get();
                        } else {
                            log.warn("Target category '{}' not found in database — collecting parameters without category",
                                    targetCategoryName);
                        }
                    }

                    final Category finalCategory = category;
                    Map<String, String> productParams = extractMostParameters(product);

                    for (Map.Entry<String, String> param : productParams.entrySet()) {
                        String paramName = param.getKey();
                        String paramValue = param.getValue();

                        String mostKey = generateMostKey(paramName);

                        ParameterData paramData = allParametersData.computeIfAbsent(
                                mostKey,
                                k -> {
                                    ParameterData pd = new ParameterData();
                                    pd.nameBg = paramName;
                                    pd.nameEn = paramName;
                                    pd.mostKey = mostKey;
                                    pd.categories = new HashSet<>();
                                    pd.values = new HashSet<>();
                                    return pd;
                                }
                        );

                        if (finalCategory != null) paramData.categories.add(finalCategory);
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
                        // Cross-platform fallback 1: match by nameBg
                        String normalizedName = paramData.nameBg != null
                                ? paramData.nameBg.toLowerCase().trim() : null;
                        Parameter byNameBg = normalizedName != null
                                ? existingByNormalizedNameBg.remove(normalizedName) : null;

                        // Cross-platform fallback 2: match by nameEn (Most properties are in English)
                        if (byNameBg == null) {
                            String normalizedNameEn = paramData.nameEn != null
                                    ? paramData.nameEn.toLowerCase().trim() : null;
                            byNameBg = normalizedNameEn != null
                                    ? existingByNormalizedNameEn.remove(normalizedNameEn) : null;
                            if (byNameBg != null) {
                                log.debug("↑ Matched parameter '{}' by nameEn='{}' (assigned mostKey={})",
                                        byNameBg.getNameBg(), paramData.nameEn, paramData.mostKey);
                            }
                        }

                        if (byNameBg != null) {
                            // Adopt existing parameter: assign mostKey and merge categories
                            byNameBg.setMostKey(paramData.mostKey);
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
                            globalParamsCache.put(paramData.mostKey, parameter);
                            reused++;
                            log.debug("↑ Adopted parameter '{}' by nameBg (assigned mostKey={})",
                                    parameter.getNameBg(), paramData.mostKey);
                        } else {
                            parameter = new Parameter();
                            parameter.setNameBg(paramData.nameBg);
                            parameter.setNameEn(paramData.nameEn);
                            parameter.setMostKey(paramData.mostKey);
                            parameter.setPlatform(Platform.MOST);
                            parameter.setOrder(50);
                            parameter.setCreatedBy("system");
                            Set<Category> validCats = new HashSet<>();
                            for (Category c : paramData.categories) {
                                if (canAddCategoryToParam(null, paramData.nameBg, c, paramNamesByCategory)) {
                                    validCats.add(c);
                                }
                            }
                            parameter.setCategories(validCats);
                            parameter = parameterRepository.save(parameter);
                            for (Category c : parameter.getCategories()) {
                                registerParamInCategory(parameter.getId(), parameter.getNameBg(), c, paramNamesByCategory);
                            }
                            globalParamsCache.put(paramData.mostKey, parameter);
                            created++;
                            log.debug("✓ Created parameter: '{}' (mostKey={}) for {} categories",
                                    parameter.getNameBg(), paramData.mostKey, validCats.size());
                        }

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
                            if (!existingCategoryIds.contains(newCat.getId()) &&
                                    canAddCategoryToParam(parameter.getId(), parameter.getNameBg(), newCat, paramNamesByCategory)) {
                                existingCategories.add(newCat);
                                registerParamInCategory(parameter.getId(), parameter.getNameBg(), newCat, paramNamesByCategory);
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

            // Auto-enable filtering for newly created parameters that have 2–50 options.
            // is_filter is left NULL by default; this query marks them as filterable
            // so they appear in the filter panel without requiring a manual admin action.
            try {
                int isFilterUpdated = entityManager.createNativeQuery(
                        "UPDATE category_parameters SET is_filter = true " +
                        "WHERE is_filter IS NULL AND parameter_id IN (" +
                        "  SELECT parameter_id FROM parameter_options " +
                        "  GROUP BY parameter_id HAVING COUNT(*) BETWEEN 2 AND 50)")
                        .executeUpdate();
                log.info("Auto-set is_filter=true for {} category_parameter entries", isFilterUpdated);
            } catch (Exception e) {
                log.error("Failed to auto-set is_filter after MOST parameters sync: {}", e.getMessage());
            }

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

            String cacheKey = buildOptionCacheKey(parameter.getId(), value);

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
            log.info("=== STARTING Most products synchronization ===");

            // Load lookup maps as primitive ID maps — safe to share across REQUIRES_NEW transactions
            // (no managed entity objects crossing transaction boundaries)
            Map<String, Long> manufacturerIdsByName = manufacturerRepository.findAll().stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            Manufacturer::getId,
                            (existing, duplicate) -> existing
                    ));
            log.info("Loaded {} manufacturers", manufacturerIdsByName.size());

            Map<String, Long> categoryIdsByName = categoryRepository.findAll().stream()
                    .filter(c -> c.getNameBg() != null)
                    .collect(Collectors.toMap(
                            c -> c.getNameBg().toLowerCase().trim(),
                            Category::getId,
                            (existing, duplicate) -> existing
                    ));
            log.info("Loaded {} categories", categoryIdsByName.size());

            Map<String, Long> parameterIdsByMostKey = parameterRepository.findAll().stream()
                    .filter(p -> p.getMostKey() != null)
                    .collect(Collectors.toMap(
                            Parameter::getMostKey,
                            Parameter::getId,
                            (existing, duplicate) -> existing
                    ));
            log.info("Loaded {} parameters with mostKey", parameterIdsByMostKey.size());

            Map<Long, Map<String, Long>> optionIdsByParamAndValue = new HashMap<>();
            for (ParameterOption opt : parameterOptionRepository.findAll()) {
                if (opt.getParameter() != null && opt.getNameBg() != null) {
                    optionIdsByParamAndValue
                            .computeIfAbsent(opt.getParameter().getId(), k -> new HashMap<>())
                            .put(normalizeName(opt.getNameBg()), opt.getId());
                }
            }
            log.info("Loaded {} option entries", optionIdsByParamAndValue.size());

            List<Map<String, Object>> allProducts = mostApiService.getAllProducts();
            log.info("Fetched {} products from Most API", allProducts.size());

            if (allProducts.isEmpty()) {
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No products found", startTime);
                return;
            }

            long totalProcessed = 0, totalCreated = 0, totalUpdated = 0, totalErrors = 0;
            long skippedNoCategory = 0, skippedNoManufacturer = 0, skippedNoSku = 0;
            Set<String> unmappedCategories = new LinkedHashSet<>();
            Set<String> seenSkus = new HashSet<>();

            for (Map<String, Object> mostProduct : allProducts) {
                String mostSku = (String) mostProduct.get("partNumber");
                if (mostSku != null && !mostSku.isBlank()) {
                    seenSkus.add(mostSku);
                }
                try {
                    // Each product runs in its own REQUIRES_NEW transaction.
                    // One failure rolls back only that product — not the entire sync.
                    int result = self.processOneMostProduct(
                            mostProduct, manufacturerIdsByName, categoryIdsByName,
                            parameterIdsByMostKey, optionIdsByParamAndValue, unmappedCategories);

                    switch (result) {
                        case 1  -> { totalCreated++;  totalProcessed++; }
                        case 2  -> { totalUpdated++;  totalProcessed++; }
                        case 0  -> skippedNoCategory++;
                        case -1 -> skippedNoSku++;
                        case -2 -> skippedNoManufacturer++;
                        default -> totalErrors++;
                    }

                } catch (Exception e) {
                    totalErrors++;
                    log.error("Error processing product: {}", e.getMessage());
                }

                if (totalProcessed > 0 && totalProcessed % 50 == 0) {
                    log.info("Progress: processed={}, created={}, updated={}, errors={}",
                            totalProcessed, totalCreated, totalUpdated, totalErrors);
                }
            }

            if (!unmappedCategories.isEmpty()) {
                log.warn("=== MOST UNMAPPED CATEGORIES ({}) — add to MOST_CATEGORY_MAPPING: {} ===",
                        unmappedCategories.size(), unmappedCategories);
            }

            // Mark products absent from Most API as NOT_AVAILABLE
            int markedUnavailable = 0;
            if (!seenSkus.isEmpty()) {
                markedUnavailable = productRepository.markNotAvailableByPlatformSkuNotIn(Platform.MOST, seenSkus);
                log.info("Marked {} MOST products as NOT_AVAILABLE (absent from Most API)", markedUnavailable);
            } else {
                log.warn("seenSkus is empty — skipping mark-unseen to prevent mass status reset");
            }

            String message = String.format(
                    "Total: %d, Created: %d, Updated: %d, Skipped (No SKU): %d, Skipped (No Category): %d, Skipped (No Manufacturer): %d, Errors: %d, Unmapped: %d, MarkedUnavailable: %d",
                    totalProcessed, totalCreated, totalUpdated, skippedNoSku, skippedNoCategory,
                    skippedNoManufacturer, totalErrors, unmappedCategories.size(), markedUnavailable);

            // Catch-all: ensure category_parameters rows exist for every parameter
            // that actually landed on a MOST product. REQUIRES_NEW per-product transactions
            // do NOT update category_parameters, so some associations may be missing.
            try {
                int cpInserted = entityManager.createNativeQuery(
                        "INSERT INTO category_parameters (category_id, parameter_id) " +
                        "SELECT DISTINCT pr.category_id, pp.parameter_id " +
                        "FROM product_parameters pp " +
                        "JOIN products pr ON pr.id = pp.product_id " +
                        "WHERE pr.platform = 'MOST' " +
                        "ON CONFLICT (category_id, parameter_id) DO NOTHING")
                        .executeUpdate();
                log.info("MOST catch-all: inserted {} missing category_parameters rows", cpInserted);

                // Auto-enable filtering for newly linked parameters
                int isFilterUpdated = entityManager.createNativeQuery(
                        "UPDATE category_parameters SET is_filter = true " +
                        "WHERE is_filter IS NULL AND parameter_id IN (" +
                        "  SELECT parameter_id FROM parameter_options " +
                        "  GROUP BY parameter_id HAVING COUNT(*) BETWEEN 2 AND 50)")
                        .executeUpdate();
                log.info("MOST catch-all: auto-set is_filter=true for {} entries", isFilterUpdated);
            } catch (Exception e) {
                log.error("Failed MOST catch-all category_parameters sync: {}", e.getMessage());
            }

            int deduplicated = productRepository.deduplicateCrossPlatformBySku();
            log.info("Cross-platform deduplication: hidden {} higher-priced duplicates", deduplicated);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, totalCreated,
                    totalUpdated, totalErrors, message, startTime);

            log.info("=== COMPLETE: Products sync finished ===");

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("=== FAILED: Products synchronization error ===", e);
            throw e;
        }
    }

    /**
     * Processes a single Most product in its own transaction (REQUIRES_NEW).
     * Returns: 1=created, 2=updated, 0=skip(no category), -2=skip(no manufacturer), -1=error
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processOneMostProduct(
            Map<String, Object> mostProduct,
            Map<String, Long> manufacturerIdsByName,
            Map<String, Long> categoryIdsByName,
            Map<String, Long> parameterIdsByMostKey,
            Map<Long, Map<String, Long>> optionIdsByParamAndValue,
            Set<String> unmappedCategories) {

        String sku = (String) mostProduct.get("partNumber");
        String name = (String) mostProduct.get("name");

        if (sku == null || sku.isBlank() || name == null) {
            return -1;
        }

        String categoryName = (String) mostProduct.get("category");
        String targetCategoryName = MOST_CATEGORY_MAPPING.get(categoryName);
        if (targetCategoryName == null) {
            if (categoryName != null) unmappedCategories.add(categoryName);
            return 0;
        }

        // For products mapped to "Лаптопи", check if the name reveals an accessory type
        // and override to the correct category (bags, covers, chargers, etc.)
        if ("Лаптопи".equals(targetCategoryName)) {
            String nameLower = name.toLowerCase();
            for (String[] override : LAPTOP_CATEGORY_NAME_OVERRIDES) {
                if (nameLower.contains(override[0])) {
                    log.debug("Overriding category for '{}': Лаптопи → {} (matched '{}')", sku, override[1], override[0]);
                    targetCategoryName = override[1];
                    break;
                }
            }
        }

        Long categoryId = categoryIdsByName.get(targetCategoryName.toLowerCase().trim());
        if (categoryId == null) {
            log.warn("Category '{}' not found in DB for product {}", targetCategoryName, sku);
            return 0;
        }

        List<Product> existing = productRepository.findBySkuAndPlatform(sku, Platform.MOST);
        Product product;
        boolean isNew;

        if (!existing.isEmpty()) {
            product = existing.get(0);
            isNew = false;
            if (existing.size() > 1) {
                log.warn("Found {} duplicates for SKU {} (MOST), keeping first", existing.size(), sku);
                for (int i = 1; i < existing.size(); i++) productRepository.delete(existing.get(i));
            }
        } else {
            product = new Product();
            product.setSku(sku);
            product.setPlatform(Platform.MOST);
            isNew = true;
        }

        product.setCategory(entityManager.getReference(Category.class, categoryId));

        if (isNew) {
            product.setNameBg(name);
            product.setNameEn(name);
            product.setCreatedBy("system");

            String description = (String) mostProduct.get("general_description");
            if (description != null && !description.isBlank()) {
                product.setDescriptionBg(description);
                product.setDescriptionEn(description);
            }

            @SuppressWarnings("unchecked")
            List<String> gallery = (List<String>) mostProduct.get("gallery");
            if (gallery != null && !gallery.isEmpty()) {
                product.setPrimaryImageUrl(gallery.get(0));
            }

            String manufacturerName = (String) mostProduct.get("manufacturer");
            if (manufacturerName != null && !manufacturerName.isBlank()) {
                Long mfId = manufacturerIdsByName.get(normalizeManufacturerName(manufacturerName));
                if (mfId == null) {
                    log.warn("Manufacturer '{}' not found for SKU {}, skipping", manufacturerName, sku);
                    return -2;
                }
                product.setManufacturer(entityManager.getReference(Manufacturer.class, mfId));
            }
        }

        Object priceObj = mostProduct.get("price");
        if (priceObj != null) {
            BigDecimal price = convertToPrice(priceObj);
            String currency = (String) mostProduct.get("currency");
            if ("USD".equals(currency)) price = price.multiply(new BigDecimal(USD_TO_BGN_RATE));
            else if ("EUR".equals(currency)) price = price.multiply(new BigDecimal(EUR_TO_BGN_RATE));
            product.setPriceClient(price);
        }

        boolean inStock = Boolean.TRUE.equals(mostProduct.get("inStock"));
        product.setStatus(inStock ? ProductStatus.AVAILABLE : ProductStatus.NOT_AVAILABLE);
        product.calculateFinalPrice();
        boolean hasValidPrice = product.getFinalPrice() != null && product.getFinalPrice().compareTo(BigDecimal.ZERO) > 0;

        // Warranty / service products are never shown regardless of stock
        String nameLowerForHide = name.toLowerCase();
        boolean isWarranty = nameLowerForHide.contains("warranty")
                || nameLowerForHide.contains(" warr")
                || nameLowerForHide.contains("carry-in war")
                || nameLowerForHide.contains("nbd onsite")
                || nameLowerForHide.contains("nbd ")
                || nameLowerForHide.contains("prosupport")
                || nameLowerForHide.contains("care pack")
                || nameLowerForHide.contains("carepack")
                || nameLowerForHide.contains("service plan")
                || nameLowerForHide.contains("hw sup")
                || nameLowerForHide.contains("гаранция");

        boolean hasImage = product.getPrimaryImageUrl() != null
                && !product.getPrimaryImageUrl().isBlank();

        if (isWarranty) {
            product.setShow(false);
            product.setStatus(ProductStatus.NOT_AVAILABLE);
        } else if (!hasImage) {
            // Products without an image are never shown
            product.setShow(false);
        } else if (isNew) {
            // New products: auto-show only if in stock and has valid price
            product.setShow(inStock && hasValidPrice);
        } else {
            // Existing products: sync can only HIDE (out of stock / no price).
            // Never force-show a product that was manually hidden (e.g. no image).
            if (!inStock || !hasValidPrice) {
                product.setShow(false);
            }
        }

        product = productRepository.save(product);

        setMostParametersToProduct(product, mostProduct, parameterIdsByMostKey, optionIdsByParamAndValue);

        return isNew ? 1 : 2;
    }

    private void setMostParametersToProduct(
            Product product,
            Map<String, Object> mostProduct,
            Map<String, Long> parameterIdsByMostKey,
            Map<Long, Map<String, Long>> optionIdsByParamAndValue) {

        try {
            // DELETE existing params for this product via JPQL.
            // Since this runs inside its own REQUIRES_NEW transaction (no shared session state),
            // there is no L1 cache conflict — the DELETE executes cleanly before any INSERTs.
            entityManager.createQuery("DELETE FROM ProductParameter pp WHERE pp.product.id = :productId")
                    .setParameter("productId", product.getId())
                    .executeUpdate();

            Map<String, String> parameterMappings = extractMostParameters(mostProduct);
            Set<String> seenKeys = new HashSet<>();
            int mappedCount = 0, notFoundCount = 0;

            for (Map.Entry<String, String> paramEntry : parameterMappings.entrySet()) {
                String parameterValue = paramEntry.getValue();
                if (parameterValue == null || parameterValue.isBlank()) continue;

                String mostKey = generateMostKey(paramEntry.getKey());
                Long parameterId = parameterIdsByMostKey.get(mostKey);
                if (parameterId == null) { notFoundCount++; continue; }

                Map<String, Long> optionMap = optionIdsByParamAndValue.get(parameterId);
                if (optionMap == null) { notFoundCount++; continue; }

                Long optionId = optionMap.get(normalizeName(parameterValue));
                if (optionId == null) {
                    // Fuzzy fallback: strip all whitespace (handles "16 GB" vs "16GB")
                    String stripped = parameterValue.toLowerCase().replaceAll("\\s+", "");
                    optionId = optionMap.entrySet().stream()
                            .filter(e -> e.getKey().replaceAll("\\s+", "").equals(stripped))
                            .map(Map.Entry::getValue)
                            .findFirst().orElse(null);
                }
                if (optionId == null) { notFoundCount++; continue; }

                if (!seenKeys.add(parameterId + ":" + optionId)) continue; // dedup

                ProductParameter pp = new ProductParameter();
                pp.setProduct(product);
                pp.setParameter(entityManager.getReference(Parameter.class, parameterId));
                pp.setParameterOption(entityManager.getReference(ParameterOption.class, optionId));
                entityManager.persist(pp);
                mappedCount++;
            }

            if (notFoundCount > 0) {
                log.warn("Product {}: {} mapped, {} not found", product.getSku(), mappedCount, notFoundCount);
            }

        } catch (Exception e) {
            log.error("ERROR setting Most parameters for product {}: {}", product.getSku(), e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractMostParameters(Map<String, Object> mostProduct) {
        Map<String, String> parameters = new HashMap<>();

        // Read from the nested "properties" map — these are the actual product specs
        // from the Most XML <properties><property name="...">value</property></properties> section.
        // Top-level product fields (code, partnum, main_picture_url, etc.) are metadata, NOT parameters.
        Object propertiesObj = mostProduct.get("properties");
        if (!(propertiesObj instanceof Map)) {
            return parameters;
        }

        Map<String, String> properties = (Map<String, String>) propertiesObj;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            if (name == null || name.isBlank() || value == null || value.isBlank()
                    || "null".equalsIgnoreCase(value.trim())) {
                continue;
            }

            String trimmedValue = value.trim();
            if (trimmedValue.length() > 200) {
                continue;
            }

            parameters.put(name, trimmedValue);
        }

        return parameters;
    }

    private boolean canAddCategoryToParam(Long paramId, String nameBg, Category cat,
                                          Map<Long, Map<String, Long>> paramNamesByCategory) {
        if (cat.getId() == null || nameBg == null) return true;
        Map<String, Long> namesInCat = paramNamesByCategory.get(cat.getId());
        if (namesInCat == null) return true;
        Long existingId = namesInCat.get(nameBg.toLowerCase().trim());
        if (existingId == null) return true;
        if (existingId.equals(paramId)) return true;
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

    private String normalizeManufacturerName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ").replaceAll("[^a-zа-я0-9\\s]+", "");
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String buildOptionCacheKey(Long parameterId, String optionName) {
        return parameterId + ":::" + normalizeName(optionName);
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


    private String generateMostKey(String parameterName) {
        if (parameterName == null || parameterName.trim().isEmpty()) {
            return "unknown_param";
        }

        // Keep Cyrillic (а-яА-Я) and Latin (a-zA-Z), digits, strip everything else
        return parameterName.toLowerCase()
                .replaceAll("[^a-z0-9а-яё\\s]+", " ")
                .trim()
                .replaceAll("\\s+", "_");
    }
}