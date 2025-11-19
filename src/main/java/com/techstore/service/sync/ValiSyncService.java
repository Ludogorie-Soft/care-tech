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
import com.techstore.service.CachedLookupService;
import com.techstore.service.ValiApiService;
import com.techstore.util.LogHelper;
import com.techstore.util.SyncHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final CachedLookupService cachedLookupService;
    private final SyncHelper syncHelper;
    private final LogHelper logHelper;

    @Value("#{'${excluded.categories.external-ids}'.split(',')}")
    private Set<Long> excludedCategories;

    @Value("${app.sync.batch-size:30}")
    private int batchSize;

    @Value("${app.sync.max-chunk-duration-minutes:5}")
    private int maxChunkDurationMinutes;

    @Transactional
    public void syncManufacturers() {
        String syncType = "VALI_MANUFACTURERS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting manufacturers synchronization");

            List<ManufacturerRequestDto> externalManufacturers = valiApiService.getManufacturers();
            Map<Long, Manufacturer> existingManufacturers = cachedLookupService.getAllManufacturersMap();

            long created = 0, updated = 0;

            for (ManufacturerRequestDto extManufacturer : externalManufacturers) {
                Manufacturer manufacturer = existingManufacturers.get(extManufacturer.getId());

                if (manufacturer == null) {
                    manufacturer = createManufacturerFromExternal(extManufacturer);
                    manufacturer = manufacturerRepository.save(manufacturer);
                    existingManufacturers.put(manufacturer.getExternalId(), manufacturer);
                    created++;
                } else {
                    updateManufacturerFromExternal(manufacturer, extManufacturer);
                    manufacturer = manufacturerRepository.save(manufacturer);
                    existingManufacturers.put(manufacturer.getExternalId(), manufacturer);
                    updated++;
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    (long) externalManufacturers.size(), created, updated, 0, null, startTime);
            log.info("Manufacturers synchronization completed - Created: {}, Updated: {}", created, updated);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during manufacturers synchronization", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void syncCategories() {
        String syncType = "VALI_CATEGORIES";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting categories synchronization");

            List<CategoryRequestFromExternalDto> externalCategories = valiApiService.getCategories();
            Map<Long, Category> existingCategories = cachedLookupService.getAllCategoriesMap();

            long created = 0, updated = 0, skipped = 0;

            for (CategoryRequestFromExternalDto extCategory : externalCategories) {
                if (excludedCategories.contains(extCategory.getId())) {
                    skipped++;
                    continue;
                }

                Category category = existingCategories.get(extCategory.getId());

                if (category == null) {
                    category = createCategoryFromExternal(extCategory);
                    category = categoryRepository.save(category);
                    existingCategories.put(category.getExternalId(), category);
                    created++;
                } else {
                    updateCategoryFromExternal(category, extCategory);
                    category = categoryRepository.save(category);
                    existingCategories.put(category.getExternalId(), category);
                    updated++;
                }
            }

            // Update parent relationships after all categories are saved
            updateCategoryParents(externalCategories, existingCategories);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, externalCategories.size(),
                    created, updated, 0,
                    skipped > 0 ? String.format("Skipped %d excluded categories", skipped) : null,
                    startTime);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during categories synchronization", e); // Log the full exception
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void syncParameters() {
        String syncType = "VALI_PARAMETERS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Vali parameters synchronization with options");

            List<Category> categories = categoryRepository.findAll();
            long totalProcessed = 0, created = 0, updated = 0, errors = 0;

            for (Category category : categories) {
                try {
                    Map<String, Parameter> existingParameters = cachedLookupService.getParametersByCategory(category);

                    List<ParameterRequestDto> externalParameters = valiApiService.getParametersByCategory(category.getExternalId());

                    if (externalParameters == null || externalParameters.isEmpty()) {
                        log.debug("No parameters found for category: {}", category.getNameBg());
                        continue;
                    }

                    List<Parameter> parametersToSave = new ArrayList<>();

                    for (ParameterRequestDto extParam : externalParameters) {
                        Parameter parameter = existingParameters.get(extParam.getId().toString());

                        if (parameter == null) {
                            parameter = parameterRepository
                                    .findByExternalIdAndCategoryId(extParam.getId(), category.getId())
                                    .orElseGet(() -> createParameterFromExternal(extParam, category));
                            created++;
                        } else {
                            updateParameterFromExternal(parameter, extParam);
                            updated++;
                        }

                        parametersToSave.add(parameter);
                    }

                    if (!parametersToSave.isEmpty()) {
                        parameterRepository.saveAll(parametersToSave);

                        for (int i = 0; i < parametersToSave.size(); i++) {
                            Parameter parameter = parametersToSave.get(i);
                            ParameterRequestDto extParam = externalParameters.get(i);

                            try {
                                if (parameter.getExternalId() != null && extParam.getOptions() != null) {
                                    syncValiParameterOptions(parameter, extParam.getOptions());
                                }
                            } catch (Exception e) {
                                log.error("Error syncing options for parameter {}: {}",
                                        parameter.getNameBg(), e.getMessage(), e); // Log the full exception
                                errors++;
                            }
                        }
                    }

                    totalProcessed += externalParameters.size();

                } catch (Exception e) {
                    log.error("Error syncing parameters for category {}: {}", category.getExternalId(), e.getMessage(), e); // Log the full exception
                    errors++;
                }
            }

            String message = String.format("Vali parameters processed: %d, created: %d, updated: %d",
                    totalProcessed, created, updated);
            if (errors > 0) {
                message += String.format(", errors: %d", errors);
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, created, updated, errors,
                    errors > 0 ? message : null, startTime);

            log.info("Vali parameters synchronization completed - Processed: {}, Created: {}, Updated: {}, Errors: {}",
                    totalProcessed, created, updated, errors);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Vali parameters synchronization", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void syncProducts() {
        String syncType = "VALI_PRODUCTS";
        log.info("Starting chunked products synchronization");
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        long totalProcessed = 0, created = 0, updated = 0, errors = 0;

        try {
            // Load all manufacturers once
            Map<Long, Manufacturer> manufacturersMap = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getExternalId() != null)
                    .collect(Collectors.toMap(
                            Manufacturer::getExternalId,
                            m -> m,
                            (existing, duplicate) -> {
                                log.warn("Duplicate manufacturer externalId: {}, IDs: {} and {}, keeping first",
                                        existing.getExternalId(), existing.getId(), duplicate.getId());
                                return existing;
                            }
                    ));

            List<Category> categories = categoryRepository.findAll();
            log.info("Found {} categories to process for products", categories.size());

            for (Category category : categories) {
                try {
                    CategorySyncResult result = syncProductsByCategory(category, manufacturersMap); // Pass manufacturersMap
                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;

                } catch (Exception e) {
                    log.error("Error processing products for category {}: {}", category.getExternalId(), e.getMessage(), e); // Log the full exception
                    errors++;
                }
            }

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, totalProcessed, created, updated, errors,
                    errors > 0 ? String.format("Completed with %d errors", errors) : null, startTime);
            log.info("Products synchronization completed - Created: {}, Updated: {}, Errors: {}", created, updated, errors);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, totalProcessed, created, updated, errors, e.getMessage(), startTime);
            log.error("Error during products synchronization", e);
            throw new RuntimeException(e);
        }
    }

    private void syncValiParameterOptions(Parameter parameter, List<ParameterOptionRequestDto> externalOptions) {
        if (externalOptions == null || externalOptions.isEmpty()) {
            return;
        }

        // Use externalId as key for existing options
        Map<Long, ParameterOption> existingOptions = parameterOptionRepository
                .findByParameterIdOrderByOrderAsc(parameter.getId()) // Corrected method name
                .stream()
                .filter(opt -> opt.getExternalId() != null)
                .collect(Collectors.toMap(
                        ParameterOption::getExternalId,
                        o -> o,
                        (existing, duplicate) -> {
                            log.warn("Duplicate parameter option externalId '{}' for parameter {}, keeping first (IDs: {} and {})",
                                    existing.getExternalId(), parameter.getNameBg(), existing.getId(), duplicate.getId());
                            return existing;
                        }
                ));

        List<ParameterOption> optionsToSave = new ArrayList<>();

        for (ParameterOptionRequestDto extOption : externalOptions) {
            ParameterOption option = existingOptions.get(extOption.getId()); // Use external ID for lookup

            if (option == null) {
                option = createValiParameterOptionFromExternal(extOption, parameter);
                if (option != null) {
                    optionsToSave.add(option);
                }
            } else {
                updateValiParameterOptionFromExternal(option, extOption);
                optionsToSave.add(option); // Add to list for saveAll
            }
        }

        if (!optionsToSave.isEmpty()) {
            parameterOptionRepository.saveAll(optionsToSave);
        }
    }

    private ParameterOption createValiParameterOptionFromExternal(ParameterOptionRequestDto extOption, Parameter parameter) {
        try {
            ParameterOption option = new ParameterOption();
            option.setExternalId(extOption.getId());
            option.setParameter(parameter);
            option.setOrder(extOption.getOrder());

            if (extOption.getName() != null) {
                extOption.getName().forEach(name -> {
                    if ("bg".equals(name.getLanguageCode())) {
                        option.setNameBg(name.getText());
                    } else if ("en".equals(name.getLanguageCode())) {
                        option.setNameEn(name.getText());
                    }
                });
            }

            return option;
        } catch (Exception e) {
            log.error("Error creating Vali parameter option with external ID {}: {}", extOption.getId(), e.getMessage(), e); // Log the full exception
            return null;
        }
    }

    private void updateValiParameterOptionFromExternal(ParameterOption option, ParameterOptionRequestDto extOption) {
        try {
            option.setOrder(extOption.getOrder());

            if (extOption.getName() != null) {
                extOption.getName().forEach(name -> {
                    if ("bg".equals(name.getLanguageCode())) {
                        option.setNameBg(name.getText());
                    } else if ("en".equals(name.getLanguageCode())) {
                        option.setNameEn(name.getText());
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error updating Vali parameter option with ID {}: {}", option.getId(), e.getMessage(), e); // Log the full exception
        }
    }

    // ===========================================
    // PRODUCT PARAMETERS MAPPING
    // ===========================================

    private void setParametersToProduct(Product product, ProductRequestDto extProduct) {
        if (extProduct.getParameters() == null || product.getCategory() == null) {
            product.setProductParameters(new HashSet<>());
            return;
        }

        Set<ProductParameter> newProductParameters = new HashSet<>();
        int mappedCount = 0;
        int notFoundCount = 0;

        // Collect all external parameter and option IDs for this product
        Set<Long> externalParameterIds = extProduct.getParameters().stream()
                .map(ParameterValueRequestDto::getParameterId)
                .collect(Collectors.toSet());
        Set<Long> externalOptionIds = extProduct.getParameters().stream()
                .map(ParameterValueRequestDto::getOptionId)
                .collect(Collectors.toSet());

        // Fetch all relevant parameters and options in bulk
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
                    log.warn("Parameter with external ID {} not found for category {} for product {}",
                            paramValue.getParameterId(), product.getCategory().getId(), extProduct.getReferenceNumber());
                    notFoundCount++;
                    continue;
                }

                ParameterOption option = optionsByExternalId.get(paramValue.getOptionId());
                if (option == null) {
                    log.warn("Parameter option with external ID {} not found for parameter {} (external ID) for product {}",
                            paramValue.getOptionId(), paramValue.getParameterId(), extProduct.getReferenceNumber());
                    notFoundCount++;
                    continue;
                }

                // Validate that the option belongs to the parameter
                if (!option.getParameter().getId().equals(parameter.getId())) {
                    log.warn("Parameter option {} (external ID) does not belong to parameter {} (external ID) for product {}",
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
                log.error("Error mapping parameter for product {}: {}", extProduct.getReferenceNumber(), e.getMessage(), e); // Log the full exception
                notFoundCount++;
            }
        }

        if (notFoundCount > 0) {
            log.warn("Finished mapping parameters for product {}. Mapped: {}, Not Found: {}",
                    extProduct.getReferenceNumber(), mappedCount, notFoundCount);
        }
        product.setProductParameters(newProductParameters);
    }

    // ===========================================
    // CATEGORY HELPERS
    // ===========================================

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

    private void updateCategoryFromExternal(Category category, CategoryRequestFromExternalDto extCategory) {
        category.setShow(extCategory.getShow());
        category.setSortOrder(extCategory.getOrder());

        String oldNameBg = category.getNameBg();
        String oldNameEn = category.getNameEn();

        if (extCategory.getName() != null) {
            extCategory.getName().forEach(name -> {
                if ("bg".equals(name.getLanguageCode())) {
                    category.setNameBg(name.getText());
                } else if ("en".equals(name.getLanguageCode())) {
                    category.setNameEn(name.getText());
                }
            });
        }

        boolean nameChanged = !Objects.equals(category.getNameBg(), oldNameBg) ||
                !Objects.equals(category.getNameEn(), oldNameEn); // Use Objects.equals for null-safe comparison

        if (category.getSlug() == null || category.getSlug().isEmpty() || nameChanged) {
            String baseName = category.getNameEn() != null ? category.getNameEn() : category.getNameBg();
            category.setSlug(generateUniqueSlugForVali(baseName, category));
        }
    }

    private String generateUniqueSlugForVali(String categoryName, Category category) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return "category-" + System.currentTimeMillis();
        }

        String baseSlug = syncHelper.createSlugFromName(categoryName);

        // Use the optimized repository method
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

    private void updateCategoryParents(List<CategoryRequestFromExternalDto> externalCategories, Map<Long, Category> existingCategories) {
        // Set parent references without saving - Hibernate will handle this on transaction commit
        for (CategoryRequestFromExternalDto extCategory : externalCategories) {
            if (extCategory.getParent() != null && extCategory.getParent() != 0) {
                Category category = existingCategories.get(extCategory.getId());
                Category parent = existingCategories.get(extCategory.getParent());

                if (category != null && parent != null && !parent.equals(category)) {
                    category.setParent(parent);
                }
            }
        }
    }

    // ===========================================
    // MANUFACTURER HELPERS
    // ===========================================

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

    private void updateManufacturerFromExternal(Manufacturer manufacturer, ManufacturerRequestDto extManufacturer) {
        manufacturer.setName(extManufacturer.getName());

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
    }

    // ===========================================
    // PARAMETER HELPERS
    // ===========================================

    private Parameter createParameterFromExternal(ParameterRequestDto extParameter, Category category) {
        Parameter parameter = new Parameter();
        parameter.setExternalId(extParameter.getId());
        parameter.setCategory(category);
        parameter.setOrder(extParameter.getOrder());
        parameter.setPlatform(Platform.VALI);

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

    private void updateParameterFromExternal(Parameter parameter, ParameterRequestDto extParameter) {
        parameter.setOrder(extParameter.getOrder());

        if (extParameter.getName() != null) {
            extParameter.getName().forEach(name -> {
                if ("bg".equals(name.getLanguageCode())) {
                    parameter.setNameBg(name.getText());
                } else if ("en".equals(name.getLanguageCode())) {
                    parameter.setNameEn(name.getText());
                }
            });
        }
    }

    // ===========================================
    // PRODUCT SYNC BY CATEGORY
    // ===========================================

    private CategorySyncResult syncProductsByCategory(Category category, Map<Long, Manufacturer> manufacturersMap) { // Added manufacturersMap
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
                    ChunkResult result = processProductsChunk(chunk, manufacturersMap, category); // Pass category
                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;

                    if (i < chunks.size() - 1) {
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    log.error("Error processing product chunk for category {}: {}", category.getExternalId(), e.getMessage(), e); // Log the full exception
                    errors += chunk.size();
                }
            }

        } catch (Exception e) {
            log.error("Error getting products for category {}: {}", category.getExternalId(), e.getMessage(), e); // Log the full exception
            errors++;
        }

        return new CategorySyncResult(totalProcessed, created, updated, errors);
    }

    private ChunkResult processProductsChunk(List<ProductRequestDto> products, Map<Long, Manufacturer> manufacturersMap, Category category) {
        long processed = 0, created = 0, updated = 0, errors = 0;
        long chunkStartTime = System.currentTimeMillis();

        // Collect all external product IDs in the current chunk
        Set<Long> externalProductIdsInChunk = products.stream()
                .map(ProductRequestDto::getId)
                .collect(Collectors.toSet());

        // Fetch existing products from the database in bulk
        Map<Long, Product> existingProductsMap = productRepository.findByExternalIdIn(externalProductIdsInChunk)
                .stream()
                .collect(Collectors.toMap(Product::getExternalId, p -> p));

        List<Product> productsToSave = new ArrayList<>();

        for (ProductRequestDto extProduct : products) {
            try {
                Product product;
                // Extract the specific manufacturer for the current product
                Manufacturer manufacturer = manufacturersMap.get(extProduct.getManufacturerId());

                if (existingProductsMap.containsKey(extProduct.getId())) {
                    product = existingProductsMap.get(extProduct.getId());
                    updateProductFieldsFromExternal(product, extProduct, manufacturer, category); // Pass specific manufacturer
                    updated++;
                } else {
                    product = new Product();
                    product.setId(null); // Ensure it's a new entity
                    updateProductFieldsFromExternal(product, extProduct, manufacturer, category); // Pass specific manufacturer
                    created++;
                }
                productsToSave.add(product);
                processed++;

                // Check for chunk duration to prevent very long transactions
                if ((System.currentTimeMillis() - chunkStartTime) > (maxChunkDurationMinutes * 60 * 1000)) {
                    log.warn("Chunk processing for category {} exceeded max duration ({} minutes). Breaking early.",
                            category.getExternalId(), maxChunkDurationMinutes);
                    break;
                }

            } catch (Exception e) {
                errors++;
                log.error("Error processing product with external ID {}: {}", extProduct.getId(), e.getMessage(), e); // Log the full exception
            }
        }

        if (!productsToSave.isEmpty()) {
            productRepository.saveAll(productsToSave);
        }

        entityManager.flush();
        entityManager.clear();

        return new ChunkResult(processed, created, updated, errors);
    }

    // ===========================================
    // PRODUCT HELPERS
    // ===========================================

    // Removed createProductFromExternal and updateProductFromExternal as they are merged into processProductsChunk
    // and updateProductFieldsFromExternal now handles both creation and update logic.

    private void updateProductFieldsFromExternal(Product product, ProductRequestDto extProduct, Manufacturer manufacturer, Category category) {
        product.setExternalId(extProduct.getId());
        product.setWorkflowId(extProduct.getIdWF());
        product.setReferenceNumber(extProduct.getReferenceNumber());
        product.setModel(extProduct.getModel());
        product.setBarcode(extProduct.getBarcode());
        product.setManufacturer(manufacturer);
        product.setStatus(ProductStatus.fromCode(extProduct.getStatus()));
        product.setPriceClient(extProduct.getPriceClient());
        product.setPricePartner(extProduct.getPricePartner());
        product.setPricePromo(extProduct.getPricePromo());
        product.setPriceClientPromo(extProduct.getPriceClientPromo());
        product.setShow(extProduct.getShow());
        product.setWarranty(extProduct.getWarranty());
        product.setWeight(extProduct.getWeight());
        product.setPlatform(Platform.VALI);

        // Set category directly from the passed category object
        product.setCategory(category);

        setImagesToProduct(product, extProduct);
        setNamesToProduct(product, extProduct);
        setDescriptionToProduct(product, extProduct);
        setParametersToProduct(product, extProduct);
        product.calculateFinalPrice();
    }

    private void setCategoryToProduct(Product product, ProductRequestDto extProduct) {
        // This method is no longer needed as category is passed directly to updateProductFieldsFromExternal
        // and set on the product.
        // Keeping it for now, but it will be removed or refactored if not used elsewhere.
        if (extProduct.getCategories() == null || extProduct.getCategories().isEmpty()) {
            return;
        }

        Long categoryId = extProduct.getCategories().get(0).getId();
        Optional<Category> categoryOpt = categoryRepository.findByExternalId(categoryId);

        if (categoryOpt.isPresent()) {
            product.setCategory(categoryOpt.get());
        } else {
            log.warn("Category with external ID {} not found for product {}",
                    categoryId, extProduct.getReferenceNumber());
        }
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

    // ===========================================
    // UTILITY METHODS
    // ===========================================

    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }

    // ===========================================
    // RESULT CLASSES
    // ===========================================

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
