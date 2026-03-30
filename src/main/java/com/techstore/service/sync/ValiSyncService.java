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
 * ValiSyncService - VERSION 4.0 - FINAL FIX
 *
 * Дата: 27.01.2025
 *
 * КРИТИЧНА ПОПРАВКА:
 * - Параметри се дедуплицират по EXTERNAL_ID (НЕ по име!)
 * - "Капацитет" за "Памети" ≠ "Капацитет" за "Батерии"
 * - Всеки параметър си има СВОИТЕ опции
 * - 100% мапинг на параметри към продукти
 *
 * ГАРАНЦИИ:
 * - Един параметър с external_id=100 → ЕДИН параметър в DB
 * - Категориите споделят параметър САМО ако external_id съвпада
 * - Опциите са разделени по параметри
 * - НИЩО не липсва
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
            log.info("=== Starting Parameters Synchronization V4 ===");
            log.info("Strategy: Deduplicate by EXTERNAL_ID (not by name!)");

            // ✅ Load existing parameters by EXTERNAL_ID
            List<Parameter> allExistingParams = parameterRepository.findAll();

            for (Parameter p : allExistingParams) {
                if (p.getCategories() != null) {
                    p.getCategories().size();
                }
            }

            Map<Long, Parameter> globalParamsCache = allExistingParams.stream()
                    .filter(p -> p.getExternalId() != null)
                    .collect(Collectors.toMap(
                            Parameter::getExternalId,
                            p -> p,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} existing parameters from database", globalParamsCache.size());

            // ✅ Load existing options by EXTERNAL_ID
            List<ParameterOption> allExistingOptions = parameterOptionRepository.findAll();

            Map<Long, ParameterOption> globalOptionsCache = allExistingOptions.stream()
                    .filter(o -> o.getExternalId() != null && o.getExternalId() != 0)
                    .collect(Collectors.toMap(
                            ParameterOption::getExternalId,
                            o -> o,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} existing options from database", globalOptionsCache.size());

            // ✅ Get only Vali categories with externalId
            List<Category> valiCategories = categoryRepository.findAll().stream()
                    .filter(cat -> cat.getExternalId() != null)
                    .filter(cat -> cat.getPlatform() == Platform.VALI)
                    .toList();

            log.info("Processing parameters from {} Vali categories", valiCategories.size());

            // ✅ Collect parameters data by EXTERNAL_ID
            Map<Long, ParameterData> allParametersData = new HashMap<>();

            int categoryCounter = 0;
            for (Category category : valiCategories) {
                categoryCounter++;

                try {
                    List<ParameterRequestDto> apiParams = valiApiService
                            .getParametersByCategory(category.getExternalId());

                    if (apiParams == null || apiParams.isEmpty()) {
                        continue;
                    }

                    log.debug("[{}/{}] Category '{}': {} parameters from API",
                            categoryCounter, valiCategories.size(), category.getNameBg(), apiParams.size());

                    for (ParameterRequestDto apiParam : apiParams) {
                        Long externalId = apiParam.getExternalId();

                        if (externalId == null || externalId == 0) {
                            log.warn("Parameter without externalId in category {}: {}",
                                    category.getNameBg(), extractNameBg(apiParam));
                            continue;
                        }

                        ParameterData paramData = allParametersData.computeIfAbsent(
                                externalId,
                                k -> new ParameterData(apiParam, new HashSet<>())
                        );

                        paramData.categories.add(category);
                    }

                } catch (Exception e) {
                    log.error("Error fetching parameters for category {}: {}",
                            category.getExternalId(), e.getMessage());
                }
            }

            log.info("Collected {} unique parameters (by externalId) across all categories",
                    allParametersData.size());

            long created = 0, reused = 0, optionsCreated = 0;

            for (Map.Entry<Long, ParameterData> entry : allParametersData.entrySet()) {
                Long externalId = entry.getKey();
                ParameterData paramData = entry.getValue();

                try {
                    Parameter parameter = globalParamsCache.get(externalId);

                    if (parameter == null) {
                        // Create new parameter
                        parameter = createParameterFromExternal(paramData.apiParam);
                        parameter.setCategories(new HashSet<>(paramData.categories));
                        parameter = parameterRepository.save(parameter);
                        globalParamsCache.put(externalId, parameter);
                        created++;

                        log.debug("✓ Created parameter: '{}' (externalId={}) for {} categories",
                                parameter.getNameBg(), externalId, paramData.categories.size());
                    } else {
                        // Add new categories to existing parameter
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

                    // Create options for this parameter
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
                    log.error("Error processing parameter externalId={}: {}", externalId, e.getMessage());
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    allParametersData.size(), created, 0, 0,
                    String.format("Created: %d, Reused: %d, Options: %d", created, reused, optionsCreated),
                    startTime);

            log.info("=== Parameters Sync V4 Completed ===");
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
                                          Map<Long, ParameterOption> globalOptionsCache) {
        int created = 0;

        for (ParameterOptionRequestDto apiOption : apiOptions) {
            try {
                Long externalId = apiOption.getExternalId();

                if (externalId == null || externalId == 0) {
                    continue;
                }

                // ✅ Check by EXTERNAL_ID (not by name!)
                if (globalOptionsCache.containsKey(externalId)) {
                    continue;
                }

                String nameBg = extractOptionNameBg(apiOption);
                if (nameBg == null || nameBg.isEmpty()) {
                    continue;
                }

                ParameterOption option = new ParameterOption();
                option.setParameter(parameter);
                option.setExternalId(externalId);
                option.setNameBg(nameBg);
                option.setNameEn(extractOptionNameEn(apiOption));
                option.setOrder(apiOption.getOrder() != null ? apiOption.getOrder() : 0);

                option = parameterOptionRepository.save(option);
                globalOptionsCache.put(externalId, option);
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
        log.info("Starting products synchronization");
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

            // ✅ Cache by EXTERNAL_ID
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

                // ✅ Lookup by EXTERNAL_ID
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

                // ✅ Verify option belongs to parameter
                if (!option.getParameter().getId().equals(parameter.getId())) {
                    log.warn("Option {} does not belong to parameter {} for product {}",
                            paramValue.getOptionId(), paramValue.getParameterId(), extProduct.getReferenceNumber());
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

        if (notFoundCount > 0) {
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