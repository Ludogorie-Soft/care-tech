package com.techstore.service.sync;

import com.techstore.dto.external.ImageDto;
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

        // Fetch existing products in bulk
        Set<Long> externalProductIdsInChunk = products.stream()
                .map(ProductRequestDto::getId)
                .collect(Collectors.toSet());

        Map<Long, Product> existingProductsMap = productRepository.findByExternalIdIn(externalProductIdsInChunk)
                .stream()
                .collect(Collectors.toMap(Product::getExternalId, p -> p));

        log.debug("Found {} existing products out of {} in chunk", existingProductsMap.size(), products.size());

        List<Product> productsToSave = new ArrayList<>();

        for (ProductRequestDto extProduct : products) {
            try {
                // ✅ VALIDATION: Check manufacturer exists
                Manufacturer manufacturer = manufacturersMap.get(extProduct.getManufacturerId());
                if (manufacturer == null) {
                    log.warn("Manufacturer with externalId {} not found for product {} ({}), skipping",
                            extProduct.getManufacturerId(), extProduct.getReferenceNumber(), extProduct.getId());
                    errors++;
                    continue; // Skip this product
                }

                Product product;

                if (existingProductsMap.containsKey(extProduct.getId())) {
                    // ✅ EXISTING PRODUCT - MINIMAL UPDATE
                    product = existingProductsMap.get(extProduct.getId());
                    log.trace("Updating existing product: {} (externalId: {})",
                            product.getSku(), product.getExternalId());
                    updateProductFieldsFromExternal(product, extProduct, manufacturer, category);
                    updated++;
                } else {
                    // ✅ NEW PRODUCT - CREATE ALL FIELDS
                    product = new Product();
                    product.setId(null);
                    log.trace("Creating new product: {} (externalId: {})",
                            extProduct.getReferenceNumber(), extProduct.getId());
                    updateProductFieldsFromExternal(product, extProduct, manufacturer, category);
                    created++;
                }

                productsToSave.add(product);
                processed++;

            } catch (Exception e) {
                errors++;
                log.error("Error processing product externalId={}, sku={}: {}",
                        extProduct.getId(),
                        extProduct.getReferenceNumber(),
                        e.getMessage());

                // Log full stack trace for first few errors
                if (errors <= 3) {
                    log.error("Full exception for debugging:", e);
                }
            }
        }

        // Save all products in chunk
        if (!productsToSave.isEmpty()) {
            try {
                log.debug("Saving {} products from chunk", productsToSave.size());
                productRepository.saveAll(productsToSave);
                log.debug("Successfully saved {} products", productsToSave.size());
            } catch (Exception e) {
                log.error("Error saving products batch: {}", e.getMessage(), e);
                errors += productsToSave.size();
            }
        }

        entityManager.flush();
        entityManager.clear();

        log.debug("Chunk processed - Processed: {}, Created: {}, Updated: {}, Errors: {}",
                processed, created, updated, errors);

        return new ChunkResult(processed, created, updated, errors);
    }

    private void updateProductFieldsFromExternal(Product product, ProductRequestDto extProduct,
                                                 Manufacturer manufacturer, Category category) {
        boolean isNew = (product.getId() == null);

        try {
            if (isNew) {
                // ✅ NEW PRODUCT - set all fields
                product.setExternalId(extProduct.getId());
                product.setWorkflowId(extProduct.getIdWF());
                product.setReferenceNumber(extProduct.getReferenceNumber());
                product.setModel(extProduct.getModel());
                product.setBarcode(extProduct.getBarcode());

                // ✅ CRITICAL: Only set manufacturer if not null
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

                // ✅ WRAP IN TRY-CATCH to prevent parameter errors from killing the product
                try {
                    setParametersToProduct(product, extProduct);
                } catch (Exception e) {
                    log.error("Error setting parameters for product {}: {}",
                            extProduct.getReferenceNumber(), e.getMessage());
                    // Continue anyway - product will be created without parameters
                    product.setProductParameters(new HashSet<>());
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

            if (!isNew) {
                log.trace("Updated product {} - status: {}, priceClient: {}",
                        product.getSku(), product.getStatus(), product.getPriceClient());
            }

        } catch (Exception e) {
            log.error("Critical error in updateProductFieldsFromExternal for product {}: {}",
                    extProduct.getReferenceNumber(), e.getMessage(), e);
            throw e; // Re-throw to be caught in processProductsChunk
        }
    }

    private void setParametersToProduct(Product product, ProductRequestDto extProduct) {
        if (extProduct.getParameters() == null || product.getCategory() == null) {
            product.setProductParameters(new HashSet<>());
            return;
        }

        Set<ProductParameter> newProductParameters = new HashSet<>();
        int mappedCount = 0;
        int notFoundCount = 0;

        Set<Long> externalParameterIds = extProduct.getParameters().stream()
                .map(ParameterValueRequestDto::getParameterId)
                .collect(Collectors.toSet());
        Set<Long> externalOptionIds = extProduct.getParameters().stream()
                .map(ParameterValueRequestDto::getOptionId)
                .collect(Collectors.toSet());

        Map<Long, Parameter> parametersByExternalId = parameterRepository
                .findByExternalIdInAndCategoryId(externalParameterIds, product.getCategory().getId())
                .stream()
                .collect(Collectors.toMap(Parameter::getExternalId, p -> p));

        Map<Long, ParameterOption> optionsByExternalId = parameterOptionRepository
                .findByExternalIdInAndParameterCategoryId(externalOptionIds, product.getCategory().getId())
                .stream()
                .collect(Collectors.toMap(ParameterOption::getExternalId, o -> o));

        for (ParameterValueRequestDto paramValue : extProduct.getParameters()) {
            try {
                Parameter parameter = parametersByExternalId.get(paramValue.getParameterId());
                if (parameter == null) {
                    log.trace("Parameter with external ID {} not found for category {} for product {}",
                            paramValue.getParameterId(), product.getCategory().getId(), extProduct.getReferenceNumber());
                    notFoundCount++;
                    continue;
                }

                ParameterOption option = optionsByExternalId.get(paramValue.getOptionId());
                if (option == null) {
                    log.trace("Parameter option with external ID {} not found for parameter {} for product {}",
                            paramValue.getOptionId(), paramValue.getParameterId(), extProduct.getReferenceNumber());
                    notFoundCount++;
                    continue;
                }

                if (!option.getParameter().getId().equals(parameter.getId())) {
                    log.trace("Parameter option {} does not belong to parameter {} for product {}",
                            paramValue.getOptionId(), paramValue.getParameterId(), extProduct.getReferenceNumber());
                    notFoundCount++;
                    continue;
                }

                ProductParameter pp = new ProductParameter();
                pp.setProduct(product);
                pp.setParameter(parameter);
                pp.setParameterOption(option);
                newProductParameters.add(pp);

                mappedCount++;

            } catch (Exception e) {
                log.error("Error mapping parameter for product {}: {}", extProduct.getReferenceNumber(), e.getMessage());
                notFoundCount++;
            }
        }

        if (notFoundCount > 0) {
            log.debug("Product {}: mapped {} parameters, {} not found",
                    extProduct.getReferenceNumber(), mappedCount, notFoundCount);
        }
        product.setProductParameters(newProductParameters);
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