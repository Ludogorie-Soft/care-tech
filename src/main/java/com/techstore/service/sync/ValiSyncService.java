package com.techstore.service.sync;

import com.techstore.dto.external.ImageDto;
import com.techstore.dto.external.NameDto;
import com.techstore.dto.request.CategoryRequestFromExternalDto;
import com.techstore.dto.request.ManufacturerRequestDto;
import com.techstore.dto.request.ParameterOptionRequestDto;
import com.techstore.dto.request.ParameterRequestDto;
import com.techstore.dto.request.ParameterValueRequestDto;
import com.techstore.dto.request.ProductRequestDto;
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
                            (existing, duplicate) -> {
                                log.warn("Duplicate manufacturer: {}, IDs: {} and {}, keeping first",
                                        existing.getName(), existing.getId(), duplicate.getId());
                                return existing;
                            }
                    ));

            log.info("Found {} existing manufacturers in database", existingManufacturers.size());

            long created = 0, skipped = 0;

            for (ManufacturerRequestDto extManufacturer : externalManufacturers) {
                String normalizedName = normalizeManufacturerName(extManufacturer.getName());
                Manufacturer manufacturer = existingManufacturers.get(normalizedName);

                if (manufacturer == null) {
                    // ✅ CREATE ONLY
                    manufacturer = createManufacturerFromExternal(extManufacturer);
                    manufacturer = manufacturerRepository.save(manufacturer);
                    existingManufacturers.put(normalizedName, manufacturer);
                    created++;
                    log.debug("Created manufacturer: {} (externalId: {})",
                            extManufacturer.getName(), extManufacturer.getId());
                } else {
                    // ✅ SKIP - already exists
                    skipped++;
                    log.trace("Manufacturer already exists, skipping: {}", extManufacturer.getName());
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

                log.info("Processing category chunk {}-{} of {}", i, end, externalCategories.size());

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
                        // ✅ CREATE ONLY
                        category = createCategoryFromExternal(extCategory);
                        categoriesToSave.add(category);
                        existingCategories.put(category.getExternalId(), category);
                        created++;
                        log.debug("Creating new category: {} (externalId: {})",
                                category.getNameBg(), extCategory.getId());
                    } else {
                        // ✅ SKIP - already exists
                        skipped++;
                        log.trace("Category already exists, skipping: {}", category.getNameBg());
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

            log.info("Updating parent relationships...");
            updateCategoryParentsOptimized(externalCategories, existingCategories);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, externalCategories.size(),
                    created, 0, 0,
                    String.format("Skipped %d existing categories", skipped), startTime);

            log.info("Categories sync completed - Created: {}, Skipped: {}", created, skipped);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during categories synchronization", e);
            throw new RuntimeException(e);
        }
    }

    private void updateCategoryParentsOptimized(List<CategoryRequestFromExternalDto> externalCategories,
                                                Map<Long, Category> existingCategories) {
        int batchSize = 50;
        int updateCount = 0;

        List<Category> categoriesToUpdate = new ArrayList<>();

        for (CategoryRequestFromExternalDto extCategory : externalCategories) {
            if (extCategory.getParent() != null && extCategory.getParent() != 0) {
                Category category = existingCategories.get(extCategory.getId());
                Category parent = existingCategories.get(extCategory.getParent());

                if (category != null && parent != null && !parent.equals(category)) {
                    if (category.getParent() == null || !category.getParent().getId().equals(parent.getId())) {
                        category.setParent(parent);
                        categoriesToUpdate.add(category);
                        updateCount++;

                        if (categoriesToUpdate.size() >= batchSize) {
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

        log.info("Updated parent relationships for {} categories", updateCount);
    }

    @Transactional
    public void syncParameters() {
        String syncType = "VALI_PARAMETERS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Vali parameters synchronization - CREATE ONLY mode");

            List<Category> categories = categoryRepository.findAll();
            log.info("Processing parameters for {} categories", categories.size());

            long totalProcessed = 0, created = 0, skipped = 0, errors = 0;

            Map<String, Parameter> globalParametersCache = parameterRepository.findAll()
                    .stream()
                    .filter(p -> p.getNameBg() != null)
                    .collect(Collectors.toMap(
                            p -> normalizeParameterName(p.getNameBg()),
                            p -> p,
                            (existing, duplicate) -> {
                                log.warn("Duplicate parameter name '{}', keeping first", existing.getNameBg());
                                return existing;
                            }
                    ));

            log.info("Loaded {} existing parameters", globalParametersCache.size());

            for (Category category : categories) {
                if (category.getExternalId() == null) {
                    log.debug("Skipping category {} - no externalId", category.getNameBg());
                    continue;
                }

                try {
                    List<ParameterRequestDto> externalParameters = valiApiService
                            .getParametersByCategory(category.getExternalId());

                    if (externalParameters == null || externalParameters.isEmpty()) {
                        log.debug("No parameters found for category: {}", category.getNameBg());
                        continue;
                    }

                    log.debug("Processing {} parameters for category: {}",
                            externalParameters.size(), category.getNameBg());

                    for (ParameterRequestDto extParam : externalParameters) {
                        try {
                            String nameBg = getParameterNameBg(extParam);
                            if (nameBg == null || nameBg.isEmpty()) {
                                log.warn("Parameter missing nameBg, skipping");
                                continue;
                            }

                            String normalizedName = normalizeParameterName(nameBg);
                            Parameter parameter = globalParametersCache.get(normalizedName);

                            if (parameter == null) {
                                // ✅ CREATE NEW PARAMETER
                                parameter = createParameterFromExternal(extParam);
                                parameter.setCategories(new HashSet<>());
                                parameter.getCategories().add(category);

                                parameter = parameterRepository.save(parameter);
                                globalParametersCache.put(normalizedName, parameter);
                                created++;

                                log.debug("Created parameter: '{}' for category '{}'",
                                        nameBg, category.getNameBg());
                            } else {
                                // ✅ PARAMETER EXISTS - ONLY ADD CATEGORY IF MISSING
                                if (!parameter.getCategories().contains(category)) {
                                    parameter.getCategories().add(category);
                                    parameterRepository.save(parameter);
                                    log.debug("Added category '{}' to existing parameter '{}'",
                                            category.getNameBg(), nameBg);
                                } else {
                                    log.trace("Parameter '{}' already exists in category, skipping", nameBg);
                                }
                                skipped++;
                            }

                            if (extParam.getOptions() != null) {
                                syncValiParameterOptionsCreateOnly(parameter, extParam.getOptions());
                            }

                            totalProcessed++;

                        } catch (Exception e) {
                            log.error("Error syncing parameter: {}", e.getMessage());
                            errors++;
                        }
                    }

                } catch (Exception e) {
                    log.error("Error syncing parameters for category {}: {}",
                            category.getExternalId(), e.getMessage());
                    errors++;
                }
            }

            String message = String.format("Created: %d, Skipped: %d existing", created, skipped);
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, created,
                    0, errors, message, startTime);

            log.info("Vali parameters sync completed - {}, Errors: {}", message, errors);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Vali parameters synchronization", e);
            throw new RuntimeException(e);
        }
    }

    private String getParameterNameBg(ParameterRequestDto extParam) {
        if (extParam.getName() == null) return null;

        return extParam.getName().stream()
                .filter(name -> "bg".equals(name.getLanguageCode()))
                .map(name -> name.getText())
                .findFirst()
                .orElse(null);
    }

    private String normalizeParameterName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String normalizeParameterValue(String value) {
        if (value == null) return "";
        return value.toLowerCase().trim().replaceAll("\\s+", " ");
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

    private void syncValiParameterOptionsCreateOnly(Parameter parameter,
                                                    List<ParameterOptionRequestDto> externalOptions) {
        if (externalOptions == null || externalOptions.isEmpty()) {
            return;
        }

        Map<String, ParameterOption> existingOptions = parameterOptionRepository
                .findByParameterIdOrderByOrderAsc(parameter.getId())
                .stream()
                .filter(opt -> opt.getNameBg() != null)
                .collect(Collectors.toMap(
                        opt -> normalizeParameterValue(opt.getNameBg()),
                        opt -> opt,
                        (existing, duplicate) -> existing
                ));

        int createdCount = 0;
        int skippedCount = 0;

        for (ParameterOptionRequestDto extOption : externalOptions) {
            String nameBg = null;
            String nameEn = null;

            if (extOption.getName() != null) {
                for (var name : extOption.getName()) {
                    if ("bg".equals(name.getLanguageCode())) {
                        nameBg = name.getText();
                    } else if ("en".equals(name.getLanguageCode())) {
                        nameEn = name.getText();
                    }
                }
            }

            if (nameBg == null || nameBg.isEmpty()) {
                continue;
            }

            String normalizedValue = normalizeParameterValue(nameBg);

            if (!existingOptions.containsKey(normalizedValue)) {
                ParameterOption option = new ParameterOption();
                option.setNameBg(nameBg);
                option.setNameEn(nameEn);
                option.setParameter(parameter);
                option.setExternalId(extOption.getExternalId());
                option.setOrder(extOption.getOrder() != null ? extOption.getOrder() : existingOptions.size() + createdCount);

                parameterOptionRepository.save(option);
                existingOptions.put(normalizedValue, option);
                createdCount++;

                log.trace("Created option '{}' for parameter '{}'", nameBg, parameter.getNameBg());
            } else {
                skippedCount++;
                log.trace("Option '{}' already exists, skipping", nameBg);
            }
        }

        if (createdCount > 0 || skippedCount > 0) {
            log.debug("Parameter '{}': created {} options, skipped {} existing",
                    parameter.getNameBg(), createdCount, skippedCount);
        }
    }

    @Transactional
    public void syncProducts() {
        String syncType = "VALI_PRODUCTS";
        log.info("Starting products synchronization - MINIMAL UPDATE mode");
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        long totalProcessed = 0, created = 0, updated = 0, errors = 0;

        try {
            // ✅ LOAD MANUFACTURERS
            Map<Long, Manufacturer> manufacturersMap = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getExternalId() != null)
                    .collect(Collectors.toMap(
                            Manufacturer::getExternalId,
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} manufacturers with externalId", manufacturersMap.size());

            // ✅ DEBUG: Show some manufacturer IDs
            if (manufacturersMap.size() > 0) {
                log.info("Sample manufacturer IDs: {}",
                        manufacturersMap.keySet().stream().limit(10).collect(Collectors.toList()));
            } else {
                log.error("⚠️⚠️⚠️ NO MANUFACTURERS FOUND! Products sync will fail! ⚠️⚠️⚠️");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0,
                        "No manufacturers found", startTime);
                return;
            }

            List<Category> categories = categoryRepository.findAll();
            log.info("Found {} categories to process for products", categories.size());

            int categoryCounter = 0;
            int processedCategories = 0;

            for (Category category : categories) {
                categoryCounter++;

                // ✅ Skip categories without externalId
                if (category.getExternalId() == null) {
                    log.debug("Skipping category {} - no externalId", category.getNameBg());
                    continue;
                }

                try {
                    log.info("Processing category {}/{}: {} (externalId: {})",
                            categoryCounter, categories.size(), category.getNameBg(), category.getExternalId());

                    CategorySyncResult result = syncProductsByCategory(category, manufacturersMap);
                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;
                    processedCategories++;

                    log.info("Category {} completed - Processed: {}, Created: {}, Updated: {}, Errors: {}",
                            category.getNameBg(), result.processed, result.created, result.updated, result.errors);

                } catch (Exception e) {
                    log.error("Error processing products for category {}: {}",
                            category.getExternalId(), e.getMessage(), e);
                    errors++;
                }
            }

            log.info("Processed {} categories out of {} total", processedCategories, categories.size());

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, created, updated, errors,
                    errors > 0 ? String.format("Completed with %d errors", errors) : null, startTime);
            log.info("Products sync completed - Total: {}, Created: {}, Updated: {}, Errors: {}",
                    totalProcessed, created, updated, errors);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, totalProcessed, created, updated, errors,
                    e.getMessage(), startTime);
            log.error("Error during products synchronization", e);
            throw new RuntimeException(e);
        }
    }

    private CategorySyncResult syncProductsByCategory(Category category,
                                                      Map<Long, Manufacturer> manufacturersMap) {
        long totalProcessed = 0, created = 0, updated = 0, errors = 0;

        try {
            List<ProductRequestDto> allProducts = valiApiService.getProductsByCategory(category.getExternalId());

            if (allProducts.isEmpty()) {
                log.debug("No products found for category: {}", category.getNameBg());
                return new CategorySyncResult(0, 0, 0, 0);
            }

            log.info("Fetched {} products for category: {}", allProducts.size(), category.getNameBg());

            // Process in chunks
            List<List<ProductRequestDto>> chunks = partitionList(allProducts, batchSize);
            log.info("Split into {} chunks of size {}", chunks.size(), batchSize);

            for (int i = 0; i < chunks.size(); i++) {
                List<ProductRequestDto> chunk = chunks.get(i);

                try {
                    log.debug("Processing chunk {}/{} ({} products)", i + 1, chunks.size(), chunk.size());

                    ChunkResult result = processProductsChunk(chunk, manufacturersMap, category);
                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;

                    // Flush after each chunk
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

            log.info("Category {} sync result - Processed: {}, Created: {}, Updated: {}, Errors: {}",
                    category.getNameBg(), totalProcessed, created, updated, errors);

        } catch (Exception e) {
            log.error("Error getting products for category {}: {}", category.getExternalId(), e.getMessage(), e);
            errors++;
        }

        return new CategorySyncResult(totalProcessed, created, updated, errors);
    }

    private ChunkResult processProductsChunk(List<ProductRequestDto> products,
                                             Map<Long, Manufacturer> manufacturersMap,
                                             Category category) {
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
                    log.warn("Manufacturer not found for product {}, skipping", extProduct.getReferenceNumber());
                    errors++;
                    continue;
                }

                Product product;

                if (existingProductsMap.containsKey(extProduct.getId())) {
                    product = existingProductsMap.get(extProduct.getId());
                    updateProductFieldsFromExternal(product, extProduct, manufacturer, category);
                    updated++;
                } else {
                    product = new Product();
                    product.setId(null);
                    updateProductFieldsFromExternal(product, extProduct, manufacturer, category);
                    created++;
                }

                productsToSave.add(product);
                processed++;

            } catch (Exception e) {
                errors++;
                log.error("Error processing product {}: {}", extProduct.getReferenceNumber(), e.getMessage());
                // ✅ Clear session след грешка
                entityManager.clear();
            }
        }

        if (!productsToSave.isEmpty()) {
            try {
                productRepository.saveAll(productsToSave);
                // ✅ Flush успешно
                entityManager.flush();
            } catch (Exception e) {
                log.error("Error saving products batch: {}", e.getMessage(), e);
                errors += productsToSave.size();
                // ✅ Clear след грешка
                entityManager.clear();
            }
        }

        // ✅ Винаги clear накрая
        entityManager.clear();

        return new ChunkResult(processed, created, updated, errors);
    }

    private void updateProductFieldsFromExternal(Product product, ProductRequestDto extProduct,
                                                 Manufacturer manufacturer, Category category) {
        boolean isNew = (product.getId() == null);

        try {
            if (isNew) {
                product.setExternalId(extProduct.getId());
                product.setWorkflowId(extProduct.getIdWF());
                product.setReferenceNumber(extProduct.getReferenceNumber());
                product.setModel(extProduct.getModel());
                product.setBarcode(extProduct.getBarcode());

                if (manufacturer != null) {
                    product.setManufacturer(manufacturer);
                } else {
                    log.warn("Product {} has no manufacturer (manufacturerId: {})",
                            extProduct.getReferenceNumber(), extProduct.getManufacturerId());
                }

                product.setCategory(category);
                product.setWarranty(extProduct.getWarranty());
                product.setWeight(extProduct.getWeight());
                product.setPlatform(Platform.VALI);
                product.setShow(extProduct.getShow());

                setImagesToProduct(product, extProduct);
                setNamesToProduct(product, extProduct);
                setDescriptionToProduct(product, extProduct);

                // ✅ NEW PRODUCTS: Always set parameters
                try {
                    setParametersToProduct(product, extProduct);
                } catch (Exception e) {
                    log.error("Error setting parameters for product {}: {}",
                            extProduct.getReferenceNumber(), e.getMessage(), e);
                    product.setProductParameters(new HashSet<>());
                }
            } else {
                // ✅ EXISTING PRODUCTS: Check if parameters were manually edited
                boolean hasManualParameters = hasManuallyEditedParameters(product);

                if (hasManualParameters) {
                    log.info("⚠️ Product {} has MANUALLY edited parameters - SKIPPING parameter sync",
                            product.getReferenceNumber());
                } else {
                    // No manual edits - safe to update from API
                    try {
                        setParametersToProduct(product, extProduct);
                    } catch (Exception e) {
                        log.error("Error updating parameters for product {}: {}",
                                extProduct.getReferenceNumber(), e.getMessage(), e);
                        // Keep existing parameters on error
                    }
                }
            }

            // ✅ ALWAYS UPDATE (for both new and existing)
            product.setStatus(ProductStatus.fromCode(extProduct.getStatus()));
            product.setPriceClient(extProduct.getPriceClient());
            product.setPricePartner(extProduct.getPricePartner());
            product.setPricePromo(extProduct.getPricePromo());
            product.setPriceClientPromo(extProduct.getPriceClientPromo());

            product.calculateFinalPrice();
            product.setCreatedBy("system");

        } catch (Exception e) {
            log.error("Critical error in updateProductFieldsFromExternal for product {}: {}",
                    extProduct.getReferenceNumber(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Проверява дали продуктът има ръчно редактирани параметри
     *
     * Критерии за "ръчно редактиран":
     * 1. Има параметри без externalId (добавени ръчно)
     * 2. Има параметри от друга платформа (не VALI)
     * 3. Има параметри, които не съществуват в API response-а
     */
    private boolean hasManuallyEditedParameters(Product product) {
        if (product.getProductParameters() == null || product.getProductParameters().isEmpty()) {
            return false;
        }

        for (ProductParameter pp : product.getProductParameters()) {
            if (pp.getParameter() == null) {
                continue;
            }

            // ✅ Проверка 1: Няма externalId = добавен ръчно
            if (pp.getParameter().getExternalId() == null) {
                log.debug("Manual parameter detected: {} (no externalId)", pp.getParameter().getNameBg());
                return true;
            }

            // ✅ Проверка 2: Друга платформа = ръчно добавен
            if (pp.getParameter().getPlatform() != null && pp.getParameter().getPlatform() != Platform.VALI) {
                log.debug("Manual parameter detected: {} (platform: {})",
                        pp.getParameter().getNameBg(), pp.getParameter().getPlatform());
                return true;
            }

            // ✅ Проверка 3: createdBy != "system" = ръчно добавен
            if (pp.getCreatedBy() != null && !"system".equals(pp.getCreatedBy())) {
                log.debug("Manual parameter detected: {} (createdBy: {})",
                        pp.getParameter().getNameBg(), pp.getCreatedBy());
                return true;
            }
        }

        return false;
    }

    private void setParametersToProduct(Product product, ProductRequestDto extProduct) {
        if (extProduct.getParameters() == null || product.getCategory() == null) {
            return;
        }

        List<Parameter> categoryParameters = parameterRepository
                .findByCategoryIdOrderByOrderAsc(product.getCategory().getId());

        Map<String, Parameter> parametersByName = categoryParameters.stream()
                .filter(p -> p.getNameBg() != null)
                .collect(Collectors.toMap(
                        p -> normalizeParameterName(p.getNameBg()),
                        p -> p,
                        (existing, duplicate) -> existing
                ));

        Set<ProductParameter> newProductParameters = new HashSet<>();

        for (ParameterValueRequestDto paramValue : extProduct.getParameters()) {
            try {
                if (paramValue.getParameterName() == null || paramValue.getParameterName().isEmpty()) {
                    continue;
                }

                String paramNameBg = paramValue.getParameterName().stream()
                        .filter(n -> "bg".equals(n.getLanguageCode()))
                        .map(NameDto::getText)
                        .findFirst()
                        .orElse(null);

                if (paramNameBg == null || paramNameBg.trim().isEmpty()) {
                    continue;
                }

                String normalizedName = normalizeParameterName(paramNameBg);
                Parameter parameter = parametersByName.get(normalizedName);

                if (parameter == null) {
                    continue;
                }

                if (paramValue.getOptionName() == null || paramValue.getOptionName().isEmpty()) {
                    continue;
                }

                String optionNameBg = paramValue.getOptionName().stream()
                        .filter(n -> "bg".equals(n.getLanguageCode()))
                        .map(NameDto::getText)
                        .findFirst()
                        .orElse(null);

                if (optionNameBg == null || optionNameBg.trim().isEmpty()) {
                    continue;
                }

                ParameterOption option = findOrCreateParameterOption(parameter, optionNameBg);

                // ✅ КРИТИЧНО: Skip ако option е null
                if (option == null || option.getId() == null) {
                    log.warn("Skipping parameter {} - option creation failed", paramNameBg);
                    continue;
                }

                ProductParameter pp = new ProductParameter();
                pp.setProduct(product);
                pp.setParameter(parameter);
                pp.setParameterOption(option);
                newProductParameters.add(pp);

            } catch (Exception e) {
                log.error("Error mapping parameter: {}", e.getMessage());
            }
        }

        // ✅ Set parameters
        if (product.getId() != null) {
            if (product.getProductParameters() != null) {
                product.getProductParameters().clear();
            }
            if (product.getProductParameters() == null) {
                product.setProductParameters(new HashSet<>());
            }
            product.getProductParameters().addAll(newProductParameters);
        } else {
            product.setProductParameters(newProductParameters);
        }

        log.info("Set {} parameters to product {}",
                newProductParameters.size(), product.getReferenceNumber());
    }

    private ParameterOption findOrCreateParameterOption(Parameter parameter, String value) {
        try {
            String normalizedValue = normalizeParameterValue(value);

            List<ParameterOption> options = parameterOptionRepository
                    .findByParameterIdOrderByOrderAsc(parameter.getId());

            for (ParameterOption opt : options) {
                if (opt.getNameBg() != null &&
                        normalizedValue.equals(normalizeParameterValue(opt.getNameBg()))) {
                    return opt;
                }
            }

            // Създай нов
            ParameterOption newOption = new ParameterOption();
            newOption.setParameter(parameter);
            newOption.setNameBg(value);
            newOption.setNameEn(value);
            newOption.setOrder(options.size());

            // ✅ КРИТИЧНО: Save и flush ВЕДНАГА
            newOption = parameterOptionRepository.save(newOption);
            parameterOptionRepository.flush();

            return newOption;

        } catch (Exception e) {
            log.error("Error creating parameter option for '{}': {}",
                    value != null && value.length() > 100 ? value.substring(0, 100) + "..." : value,
                    e.getMessage());
            // ✅ КРИТИЧНО: Clear session след грешка
            entityManager.clear();
            return null;
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

        String discriminator = syncHelper.extractDiscriminator(categoryName);
        if (discriminator != null && !discriminator.isEmpty()) {
            String discriminatedSlug = baseSlug + "-" + discriminator;
            if (!categoryRepository.existsBySlugAndIdNot(discriminatedSlug, category.getId())) {
                return discriminatedSlug;
            }
        }

        int counter = 1;
        String numberedSlug;
        do {
            numberedSlug = baseSlug + "-" + counter;
            counter++;
        } while (categoryRepository.existsBySlugAndIdNot(numberedSlug, category.getId()));

        return numberedSlug;
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

    private static void setImagesToProduct(Product product, ProductRequestDto extProduct) {
        if (extProduct.getImages() != null && !extProduct.getImages().isEmpty()) {
            product.setPrimaryImageUrl(extProduct.getImages().get(0).getHref());

            List<String> newAdditionalImages = extProduct.getImages().stream()
                    .skip(1)
                    .map(ImageDto::getHref)
                    .toList();

            if (product.getAdditionalImages() != null) {
                product.getAdditionalImages().clear();
                product.getAdditionalImages().addAll(newAdditionalImages);
            } else {
                product.setAdditionalImages(new ArrayList<>(newAdditionalImages));
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
        long processed;
        long created;
        long updated;
        long errors;

        CategorySyncResult(long processed, long created, long updated, long errors) {
            this.processed = processed;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }
    }

    private static class ChunkResult {
        long processed;
        long created;
        long updated;
        long errors;

        ChunkResult(long processed, long created, long updated, long errors) {
            this.processed = processed;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }
    }
}