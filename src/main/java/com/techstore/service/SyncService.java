package com.techstore.service;

import com.techstore.dto.external.ImageDto;
import com.techstore.dto.external.ProductRequestDto;
import com.techstore.dto.request.CategoryRequestDto;
import com.techstore.dto.request.DocumentRequestDto;
import com.techstore.dto.request.ManufacturerRequestDto;
import com.techstore.dto.request.ParameterOptionRequestDto;
import com.techstore.dto.request.ParameterRequestDto;
import com.techstore.dto.request.ParameterValueRequestDto;
import com.techstore.entity.Category;
import com.techstore.entity.Manufacturer;
import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import com.techstore.entity.Product;
import com.techstore.entity.ProductDocument;
import com.techstore.entity.ProductParameter;
import com.techstore.entity.SyncLog;
import com.techstore.enums.ProductStatus;
import com.techstore.exception.ResourceNotFoundException;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ParameterOptionRepository;
import com.techstore.repository.ParameterRepository;
import com.techstore.repository.ProductDocumentRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.SyncLogRepository;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final ValiApiService valiApiService;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;
    private final ParameterRepository parameterRepository;
    private final ParameterOptionRepository parameterOptionRepository;
    private final SyncLogRepository syncLogRepository;
    private final EntityManager entityManager;
    private final DataSource dataSource;
    private final ProductDocumentRepository productDocumentRepository;


    @Value("${app.sync.enabled:true}")
    private boolean syncEnabled;

    @Value("${app.sync.batch-size:30}")
    private int batchSize;

    @Value("${app.sync.max-chunk-duration-minutes:5}")
    private int maxChunkDurationMinutes;

    // ============ SCHEDULED SYNC ============
    @Scheduled(cron = "${app.sync.cron}")
    public void scheduledSync() {
        if (!syncEnabled) {
            log.info("Synchronization is disabled");
            return;
        }

        log.info("Starting scheduled synchronization at {}", LocalDateTime.now());
        try {
            syncCategories();
            log.info("Scheduled category synchronization completed at {}", LocalDateTime.now());

            syncManufacturers();
            log.info("Scheduled manufacturers synchronization completed at {}", LocalDateTime.now());

            syncParameters();
            log.info("Scheduled parameters synchronization completed at {}", LocalDateTime.now());

            syncProducts();
            log.info("Scheduled products synchronization completed at {}", LocalDateTime.now());

            syncDocuments();
            log.info("Scheduled documents synchronization completed at {}", LocalDateTime.now());

        } catch (Exception e) {
            log.error("CRITICAL: Scheduled synchronization failed", e);
        }
    }

    // ============ MONITORING ============
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorSyncHealth() {
        try {
            List<SyncLog> stuckSyncs = syncLogRepository.findByStatusAndCreatedAtBefore(
                    "IN_PROGRESS", LocalDateTime.now().minusHours(2));

            if (!stuckSyncs.isEmpty()) {
                log.warn("Found {} stuck sync processes", stuckSyncs.size());
                stuckSyncs.forEach(sync -> {
                    sync.setStatus("FAILED");
                    sync.setErrorMessage("Stuck - marked as failed by monitor");
                    syncLogRepository.save(sync);
                });
            }

            monitorConnectionPool();

        } catch (Exception e) {
            log.error("Error during sync health check", e);
        }
    }

    // ============ CATEGORIES SYNC ============
    @Transactional
    public void syncCategories() {
        SyncLog syncLog = createSyncLogSimple("CATEGORIES");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting categories synchronization");

            List<CategoryRequestDto> externalCategories = valiApiService.getCategories();
            Map<Long, Category> existingCategories = categoryRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(Category::getExternalId, c -> c));

            long created = 0, updated = 0;

            for (CategoryRequestDto extCategory : externalCategories) {
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

            updateCategoryParents(externalCategories, existingCategories);

            updateSyncLogSimple(syncLog, "SUCCESS", (long) externalCategories.size(), created, updated, 0, null, startTime);
            log.info("Categories synchronization completed - Created: {}, Updated: {}", created, updated);

        } catch (Exception e) {
            updateSyncLogSimple(syncLog, "FAILED", 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during categories synchronization", e);
            throw e;
        }
    }

    // ============ MANUFACTURERS SYNC ============
    @Transactional
    public void syncManufacturers() {
        SyncLog syncLog = createSyncLogSimple("MANUFACTURERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting manufacturers synchronization");

            List<ManufacturerRequestDto> externalManufacturers = valiApiService.getManufacturers();
            Map<Long, Manufacturer> existingManufacturers = manufacturerRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(Manufacturer::getExternalId, m -> m));

            long created = 0, updated = 0;

            for (ManufacturerRequestDto extManufacturer : externalManufacturers) {
                Manufacturer manufacturer = existingManufacturers.get(extManufacturer.getId());

                if (manufacturer == null) {
                    manufacturer = createManufacturerFromExternal(extManufacturer);
                    created++;
                } else {
                    updateManufacturerFromExternal(manufacturer, extManufacturer);
                    updated++;
                }

                manufacturerRepository.save(manufacturer);
            }

            updateSyncLogSimple(syncLog, "SUCCESS", (long) externalManufacturers.size(), created, updated, 0, null, startTime);
            log.info("Manufacturers synchronization completed - Created: {}, Updated: {}", created, updated);

        } catch (Exception e) {
            updateSyncLogSimple(syncLog, "FAILED", 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during manufacturers synchronization", e);
            throw e;
        }
    }

    // ============ PARAMETERS SYNC ============
    @Transactional
    public void syncParameters() {
        SyncLog syncLog = createSyncLogSimple("PARAMETERS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting chunked parameters synchronization");

            List<Category> categories = categoryRepository.findAll();
            long totalProcessed = 0, created = 0, updated = 0, errors = 0;

            for (Category category : categories) {
                try {
                    ParameterSyncResult result = syncParametersByCategory(category);
                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;

                    log.debug("Completed parameters for category {} - Processed: {}, Created: {}, Updated: {}, Errors: {}",
                            category.getExternalId(), result.processed, result.created, result.updated, result.errors);

                } catch (Exception e) {
                    log.error("Error syncing parameters for category {}: {}", category.getExternalId(), e.getMessage());
                    errors++;
                }
            }

            updateSyncLogSimple(syncLog, "SUCCESS", totalProcessed, created, updated, errors,
                    errors > 0 ? String.format("Completed with %d errors", errors) : null, startTime);
            log.info("Parameters synchronization completed - Created: {}, Updated: {}, Errors: {}", created, updated, errors);

        } catch (Exception e) {
            updateSyncLogSimple(syncLog, "FAILED", 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during parameters synchronization", e);
            throw e;
        }
    }

    private ParameterSyncResult syncParametersByCategory(Category category) {
        long totalProcessed = 0, created = 0, updated = 0, errors = 0;

        try {
            List<ParameterRequestDto> allParameters = valiApiService.getParametersByCategory(category.getExternalId());

            if (allParameters.isEmpty()) {
                return new ParameterSyncResult(0, 0, 0, 0);
            }

            List<List<ParameterRequestDto>> chunks = partitionList(allParameters, batchSize);
            log.debug("Processing {} parameters in {} chunks for category {}",
                    allParameters.size(), chunks.size(), category.getExternalId());

            for (int i = 0; i < chunks.size(); i++) {
                List<ParameterRequestDto> chunk = chunks.get(i);

                try {
                    ParameterChunkResult result = processParametersChunk(chunk, category);
                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;

                    if (i < chunks.size() - 1) {
                        Thread.sleep(100);
                    }

                } catch (Exception e) {
                    log.error("Error processing parameter chunk {}/{} for category {}: {}",
                            i + 1, chunks.size(), category.getExternalId(), e.getMessage());
                    errors += chunk.size();
                }
            }

        } catch (Exception e) {
            log.error("Error getting parameters for category {}: {}", category.getExternalId(), e.getMessage());
            errors++;
        }

        return new ParameterSyncResult(totalProcessed, created, updated, errors);
    }

    private ParameterChunkResult processParametersChunk(List<ParameterRequestDto> parameters, Category category) {
        long processed = 0, created = 0, updated = 0, errors = 0;
        long chunkStartTime = System.currentTimeMillis();

        Map<Long, Parameter> existingParameters = parameterRepository
                .findByCategoryIdOrderByOrderAsc(category.getId())
                .stream()
                .collect(Collectors.toMap(Parameter::getExternalId, p -> p));

        for (ParameterRequestDto extParameter : parameters) {
            try {
                Parameter parameter = existingParameters.get(extParameter.getId());

                if (parameter == null) {
                    parameter = createParameterFromExternal(extParameter, category);
                    parameter = parameterRepository.save(parameter);
                    created++;
                } else {
                    updateParameterFromExternal(parameter, extParameter);
                    parameter = parameterRepository.save(parameter);
                    updated++;
                }

                syncParameterOptionsChunked(parameter, extParameter.getOptions());
                processed++;

                if ((System.currentTimeMillis() - chunkStartTime) > (maxChunkDurationMinutes * 60 * 1000)) {
                    log.warn("Parameter chunk processing taking too long, will continue in next chunk");
                    break;
                }

            } catch (Exception e) {
                errors++;
                log.error("Error processing parameter {}: {}", extParameter.getId(), e.getMessage());
            }
        }

        entityManager.flush();
        entityManager.clear();

        return new ParameterChunkResult(processed, created, updated, errors);
    }

    // ============ PRODUCTS SYNC ============
    @Transactional
    public void syncProducts() {
        log.info("Starting chunked products synchronization");
        SyncLog syncLog = createSyncLogSimple("PRODUCTS");
        long startTime = System.currentTimeMillis();

        long totalProcessed = 0, created = 0, updated = 0, errors = 0;

        try {
            List<Category> categories = categoryRepository.findAll();
            log.info("Found {} categories to process for products", categories.size());

            for (Category category : categories) {
                try {
                    CategorySyncResult result = syncProductsByCategory(category);
                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;

                    log.debug("Completed products for category {} - Processed: {}, Created: {}, Updated: {}, Errors: {}",
                            category.getExternalId(), result.processed, result.created, result.updated, result.errors);

                } catch (Exception e) {
                    log.error("Error processing products for category {}: {}", category.getExternalId(), e.getMessage());
                    errors++;
                }
            }

            updateSyncLogSimple(syncLog, "SUCCESS", totalProcessed, created, updated, errors,
                    errors > 0 ? String.format("Completed with %d errors", errors) : null, startTime);
            log.info("Products synchronization completed - Created: {}, Updated: {}, Errors: {}", created, updated, errors);

        } catch (Exception e) {
            updateSyncLogSimple(syncLog, "FAILED", totalProcessed, created, updated, errors, e.getMessage(), startTime);
            log.error("Error during products synchronization", e);
            throw e;
        }
    }

    @Transactional
    public void syncDocuments() {
        SyncLog syncLog = createSyncLogSimple("DOCUMENTS");
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting documents synchronization");

            List<DocumentRequestDto> externalDocuments = valiApiService.getAllDocuments();

            if (externalDocuments.isEmpty()) {
                log.info("No documents found in external API");
                updateSyncLogSimple(syncLog, "SUCCESS", 0, 0, 0, 0, null, startTime);
                return;
            }

            List<List<DocumentRequestDto>> chunks = partitionList(externalDocuments, batchSize);
            log.info("Processing {} documents in {} chunks", externalDocuments.size(), chunks.size());

            long totalProcessed = 0, created = 0, updated = 0, errors = 0;

            for (int i = 0; i < chunks.size(); i++) {
                List<DocumentRequestDto> chunk = chunks.get(i);

                try {
                    DocumentChunkResult result = processDocumentsChunk(chunk);
                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;

                    log.debug("Processed document chunk {}/{} - Created: {}, Updated: {}, Errors: {}",
                            i + 1, chunks.size(), result.created, result.updated, result.errors);

                    if (i < chunks.size() - 1) {
                        Thread.sleep(150);
                    }

                } catch (Exception e) {
                    log.error("Error processing document chunk {}/{}: {}", i + 1, chunks.size(), e.getMessage());
                    errors += chunk.size();
                }
            }

            updateSyncLogSimple(syncLog, "SUCCESS", totalProcessed, created, updated, errors,
                    errors > 0 ? String.format("Completed with %d errors", errors) : null, startTime);
            log.info("Documents synchronization completed - Created: {}, Updated: {}, Errors: {}", created, updated, errors);

        } catch (Exception e) {
            updateSyncLogSimple(syncLog, "FAILED", 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during documents synchronization", e);
            throw e;
        }
    }

    // ============ SINGLE CATEGORY SYNC (For testing) ============
    public void syncProductsByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        log.info("Starting sync for single category: {}", categoryId);
        CategorySyncResult result = syncProductsByCategory(category);
        log.info("Single category sync completed - Processed: {}, Created: {}, Updated: {}, Errors: {}",
                result.processed, result.created, result.updated, result.errors);
    }

    public void fetchAll() {
        log.info("Starting manual full synchronization");
        syncCategories();
        syncManufacturers();
        syncParameters();
        syncProducts();
        syncDocuments();
        log.info("Manual full synchronization completed");
    }

    private CategorySyncResult syncProductsByCategory(Category category) {
        long totalProcessed = 0, created = 0, updated = 0, errors = 0;

        try {
            Map<Long, Manufacturer> manufacturersMap = manufacturerRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(Manufacturer::getExternalId, m -> m));

            List<ProductRequestDto> allProducts = valiApiService.getProductsByCategory(category.getExternalId());

            if (allProducts.isEmpty()) {
                log.debug("No products found for category {}", category.getExternalId());
                return new CategorySyncResult(0, 0, 0, 0);
            }

            List<List<ProductRequestDto>> chunks = partitionList(allProducts, batchSize);
            log.debug("Processing {} products in {} chunks for category {}",
                    allProducts.size(), chunks.size(), category.getExternalId());

            for (int i = 0; i < chunks.size(); i++) {
                List<ProductRequestDto> chunk = chunks.get(i);

                try {
                    ChunkResult result = processProductsChunk(chunk, manufacturersMap);
                    totalProcessed += result.processed;
                    created += result.created;
                    updated += result.updated;
                    errors += result.errors;

                    if (i < chunks.size() - 1) {
                        Thread.sleep(200);
                    }

                } catch (Exception e) {
                    log.error("Error processing product chunk {}/{} for category {}: {}",
                            i + 1, chunks.size(), category.getExternalId(), e.getMessage());
                    errors += chunk.size();
                }
            }

        } catch (Exception e) {
            log.error("Error getting products for category {}: {}", category.getExternalId(), e.getMessage());
            errors++;
        }

        return new CategorySyncResult(totalProcessed, created, updated, errors);
    }

    private ChunkResult processProductsChunk(List<ProductRequestDto> products, Map<Long, Manufacturer> manufacturersMap) {
        long processed = 0, created = 0, updated = 0, errors = 0;
        long chunkStartTime = System.currentTimeMillis();

        for (ProductRequestDto extProduct : products) {
            try {
                Optional<Product> existingProduct = productRepository.findByExternalId(extProduct.getId());

                if (existingProduct.isPresent()) {
                    updateProductFromExternal(existingProduct.get(), extProduct, manufacturersMap);
                    updated++;
                } else {
                    createProductFromExternal(extProduct, manufacturersMap);
                    created++;
                }

                processed++;

                if (processed % 10 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }

                if ((System.currentTimeMillis() - chunkStartTime) > (maxChunkDurationMinutes * 60 * 1000)) {
                    log.warn("Product chunk processing taking too long, will continue in next chunk");
                    break;
                }

            } catch (Exception e) {
                errors++;
                log.error("Error processing product {}: {}", extProduct.getId(), e.getMessage());
            }
        }

        entityManager.flush();
        entityManager.clear();

        return new ChunkResult(processed, created, updated, errors);
    }

    // ============ UTILITY METHODS ============
    private <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return chunks;
    }

    private SyncLog createSyncLogSimple(String syncType) {
        try {
            SyncLog syncLog = new SyncLog();
            syncLog.setSyncType(syncType);
            syncLog.setStatus("IN_PROGRESS");
            return syncLogRepository.save(syncLog);
        } catch (Exception e) {
            log.error("Failed to create sync log: {}", e.getMessage());
            SyncLog dummyLog = new SyncLog();
            dummyLog.setId(-1L);
            dummyLog.setSyncType(syncType);
            return dummyLog;
        }
    }

    private void updateSyncLogSimple(SyncLog syncLog, String status, long processed,
                                     long created, long updated, long errors,
                                     String errorMessage, long startTime) {
        try {
            if (syncLog.getId() != null && syncLog.getId() > 0) {
                syncLog.setStatus(status);
                syncLog.setRecordsProcessed(processed);
                syncLog.setRecordsCreated(created);
                syncLog.setRecordsUpdated(updated);
                syncLog.setDurationMs(System.currentTimeMillis() - startTime);

                if (errorMessage != null) {
                    syncLog.setErrorMessage(errorMessage);
                }

                syncLogRepository.save(syncLog);
            }
        } catch (Exception e) {
            log.error("Failed to update sync log: {}", e.getMessage());
        }
    }

    private void syncParameterOptionsChunked(Parameter parameter, List<ParameterOptionRequestDto> allOptions) {
        if (allOptions == null || allOptions.isEmpty()) {
            return;
        }

        Map<Long, ParameterOption> existingOptions = parameterOptionRepository
                .findByParameterIdOrderByOrderAsc(parameter.getId())
                .stream()
                .collect(Collectors.toMap(ParameterOption::getExternalId, o -> o));

        List<List<ParameterOptionRequestDto>> chunks = partitionList(allOptions, 20);

        for (List<ParameterOptionRequestDto> chunk : chunks) {
            for (ParameterOptionRequestDto extOption : chunk) {
                try {
                    ParameterOption option = existingOptions.get(extOption.getId());

                    if (option == null) {
                        option = createParameterOptionFromExternal(extOption, parameter);
                        parameterOptionRepository.save(option);
                    } else {
                        updateParameterOptionFromExternal(option, extOption);
                        parameterOptionRepository.save(option);
                    }
                } catch (Exception e) {
                    log.error("Error processing parameter option {}: {}", extOption.getId(), e.getMessage());
                }
            }
        }
    }

    private void monitorConnectionPool() {
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource ds = (HikariDataSource) dataSource;
                int active = ds.getHikariPoolMXBean().getActiveConnections();
                int total = ds.getHikariPoolMXBean().getTotalConnections();
                int idle = ds.getHikariPoolMXBean().getIdleConnections();

                log.debug("Connection Pool Status - Active: {}, Idle: {}, Total: {}", active, idle, total);

                if (active > (total * 0.8)) {
                    log.warn("Connection pool usage high: {}/{} ({}%)", active, total, (active * 100 / total));
                }
            }
        } catch (Exception e) {
            log.debug("Could not retrieve connection pool stats: {}", e.getMessage());
        }
    }

    private DocumentChunkResult processDocumentsChunk(List<DocumentRequestDto> documents) {
        long processed = 0, created = 0, updated = 0, errors = 0;
        long chunkStartTime = System.currentTimeMillis();

        for (DocumentRequestDto extDocument : documents) {
            try {
                Optional<Product> productOpt = productRepository.findByExternalId(extDocument.getProductId());

                if (productOpt.isEmpty()) {
                    log.warn("Product not found for document {} with productId: {}",
                            extDocument.getId(), extDocument.getProductId());
                    errors++;
                    continue;
                }

                Product product = productOpt.get();

                Optional<ProductDocument> existingDocument = productDocumentRepository
                        .findByProductIdAndDocumentUrl(product.getId(), extDocument.getDocumentUrl());

                if (existingDocument.isPresent()) {
                    updateDocumentFromExternal(existingDocument.get(), extDocument);
                    updated++;
                } else {
                    createDocumentFromExternal(extDocument, product);
                    created++;
                }

                processed++;

                if (processed % 20 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }

                if ((System.currentTimeMillis() - chunkStartTime) > (maxChunkDurationMinutes * 60 * 1000)) {
                    log.warn("Document chunk processing taking too long, will continue in next chunk");
                    break;
                }

            } catch (Exception e) {
                errors++;
                log.error("Error processing document {}: {}", extDocument.getId(), e.getMessage());
            }
        }

        entityManager.flush();
        entityManager.clear();

        return new DocumentChunkResult(processed, created, updated, errors);
    }

    private void createDocumentFromExternal(DocumentRequestDto extDocument, Product product) {
        ProductDocument document = new ProductDocument();
        document.setProduct(product);
        updateDocumentFieldsFromExternal(document, extDocument);

        try {
            productDocumentRepository.save(document);
            log.debug("Created document for product: {}", product.getExternalId());
        } catch (Exception e) {
            log.error("Failed to create document for product {}: {}",
                    product.getExternalId(), e.getMessage());
            throw e;
        }
    }

    private void updateDocumentFromExternal(ProductDocument document, DocumentRequestDto extDocument) {
        updateDocumentFieldsFromExternal(document, extDocument);

        try {
            productDocumentRepository.save(document);
            log.debug("Updated document for product: {}", document.getProduct().getExternalId());
        } catch (Exception e) {
            log.error("Failed to update document for product {}: {}",
                    document.getProduct().getExternalId(), e.getMessage());
            throw e;
        }
    }

    private void updateDocumentFieldsFromExternal(ProductDocument document, DocumentRequestDto extDocument) {
        document.setDocumentUrl(extDocument.getDocumentUrl());

        if (extDocument.getComment() != null) {
            extDocument.getComment().forEach(comment -> {
                switch (comment.getLanguageCode()) {
                    case "bg" -> document.setCommentBg(comment.getText());
                    case "en" -> document.setCommentEn(comment.getText());
                }
            });
        }
    }

    private static class DocumentChunkResult {
        long processed, created, updated, errors;

        DocumentChunkResult(long processed, long created, long updated, long errors) {
            this.processed = processed;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }
    }

    public void syncDocumentsByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        log.info("Starting document sync for product: {}", productId);

        try {
            List<DocumentRequestDto> documents = valiApiService.getDocumentsByProduct(product.getExternalId());

            long created = 0, updated = 0, errors = 0;

            for (DocumentRequestDto extDocument : documents) {
                try {
                    Optional<ProductDocument> existingDocument = productDocumentRepository
                            .findByProductIdAndDocumentUrl(product.getId(), extDocument.getDocumentUrl());

                    if (existingDocument.isPresent()) {
                        updateDocumentFromExternal(existingDocument.get(), extDocument);
                        updated++;
                    } else {
                        createDocumentFromExternal(extDocument, product);
                        created++;
                    }
                } catch (Exception e) {
                    errors++;
                    log.error("Error processing document {} for product {}: {}",
                            extDocument.getId(), productId, e.getMessage());
                }
            }

            log.info("Document sync completed for product {} - Created: {}, Updated: {}, Errors: {}",
                    productId, created, updated, errors);

        } catch (Exception e) {
            log.error("Error syncing documents for product {}: {}", productId, e.getMessage());
            throw e;
        }
    }

    // ============ RESULT CLASSES ============
    private static class ChunkResult {
        long processed, created, updated, errors;

        ChunkResult(long processed, long created, long updated, long errors) {
            this.processed = processed;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }
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

    private static class ParameterSyncResult {
        long processed, created, updated, errors;

        ParameterSyncResult(long processed, long created, long updated, long errors) {
            this.processed = processed;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }
    }

    private static class ParameterChunkResult {
        long processed, created, updated, errors;

        ParameterChunkResult(long processed, long created, long updated, long errors) {
            this.processed = processed;
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }
    }

    // ============ ENTITY CREATION/UPDATE METHODS ============
    private void createProductFromExternal(ProductRequestDto extProduct, Map<Long, Manufacturer> manufacturersMap) {
        Manufacturer manufacturer = manufacturersMap.get(extProduct.getManufacturerId());

        if (manufacturer == null) {
            log.warn("Manufacturer not found for product {}: {}. Creating product without manufacturer.",
                    extProduct.getId(), extProduct.getManufacturerId());
        }

        Product product = new Product();
        product.setId(null);
        updateProductFieldsFromExternal(product, extProduct, manufacturer);

        try {
            productRepository.save(product);
            log.debug("Created product with externalId: {}", extProduct.getId());
        } catch (Exception e) {
            log.error("Failed to create product with externalId {}: {}",
                    extProduct.getId(), e.getMessage());
            throw e;
        }
    }

    private void updateProductFromExternal(Product product, ProductRequestDto extProduct, Map<Long, Manufacturer> manufacturersMap) {
        Manufacturer manufacturer = manufacturersMap.get(extProduct.getManufacturerId());

        if (manufacturer == null) {
            log.warn("Manufacturer not found for product {}: {}. Updating product without manufacturer.",
                    extProduct.getId(), extProduct.getManufacturerId());
        }

        updateProductFieldsFromExternal(product, extProduct, manufacturer);

        try {
            productRepository.save(product);
            log.debug("Updated product with externalId: {}", extProduct.getId());
        } catch (Exception e) {
            log.error("Failed to update product with externalId {}: {}", extProduct.getId(), e.getMessage());
            throw e;
        }
    }

    private void updateProductFieldsFromExternal(Product product, ProductRequestDto extProduct, Manufacturer manufacturer) {
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

        setWeightSafely(product, extProduct);
        setCategoryToProduct(product, extProduct);
        setImagesToProduct(product, extProduct);
        setNamesToProduct(product, extProduct);
        setDescriptionToProduct(product, extProduct);
        setParametersToProduct(product, extProduct);
        product.calculateFinalPrice();
    }

    private void setWeightSafely(Product product, ProductRequestDto extProduct) {
        BigDecimal weight = extProduct.getWeight();

        if (weight != null) {
            if (weight.compareTo(BigDecimal.valueOf(9.99)) > 0) {
                log.warn("Weight {} for product {} exceeds database precision, setting to 9.99",
                        weight, extProduct.getId());
                product.setWeight(BigDecimal.valueOf(9.99));
            } else if (weight.compareTo(BigDecimal.valueOf(-9.99)) < 0) {
                log.warn("Weight {} for product {} is below database precision, setting to -9.99",
                        weight, extProduct.getId());
                product.setWeight(BigDecimal.valueOf(-9.99));
            } else {
                product.setWeight(weight);
            }
        } else {
            product.setWeight(null);
        }
    }

    private void setCategoryToProduct(Product product, ProductRequestDto extProduct) {
        if (extProduct.getCategories() != null && !extProduct.getCategories().isEmpty()) {
            categoryRepository.findByExternalId(extProduct.getCategories().get(0).getId())
                    .ifPresent(product::setCategory);
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
                switch (name.getLanguageCode()) {
                    case "bg" -> product.setNameBg(name.getText());
                    case "en" -> product.setNameEn(name.getText());
                }
            });
        }
    }

    private static void setDescriptionToProduct(Product product, ProductRequestDto extProduct) {
        if (extProduct.getDescription() != null) {
            extProduct.getDescription().forEach(desc -> {
                switch (desc.getLanguageCode()) {
                    case "bg" -> product.setDescriptionBg(desc.getText());
                    case "en" -> product.setDescriptionEn(desc.getText());
                }
            });
        }
    }

    private void setParametersToProduct(Product product, ProductRequestDto extProduct) {
        if (extProduct.getParameters() != null && product.getCategory() != null) {
            Set<ProductParameter> newProductParameters = new HashSet<>();

            for (ParameterValueRequestDto paramValue : extProduct.getParameters()) {
                Optional<Parameter> parameterOpt = parameterRepository
                        .findByExternalIdAndCategoryId(paramValue.getParameterId(), product.getCategory().getId());

                if (parameterOpt.isPresent()) {
                    Parameter parameter = parameterOpt.get();

                    Optional<ParameterOption> optionOpt = parameterOptionRepository
                            .findByExternalIdAndParameterId(paramValue.getOptionId(), parameter.getId());

                    if (optionOpt.isPresent()) {
                        ParameterOption option = optionOpt.get();

                        ProductParameter pp = new ProductParameter();
                        pp.setProduct(product);
                        pp.setParameter(parameter);
                        pp.setParameterOption(option);

                        newProductParameters.add(pp);
                    } else {
                        log.warn("Parameter option not found - OptionId: {} for ParameterId: {} in ProductId: {}",
                                paramValue.getOptionId(), parameter.getId(), extProduct.getId());
                    }
                } else {
                    log.warn("Parameter not found - ParameterId: {} for CategoryId: {} in ProductId: {}",
                            paramValue.getParameterId(), product.getCategory().getId(), extProduct.getId());
                }
            }

            product.setProductParameters(newProductParameters);
            log.debug("Set {} product parameters for product with externalId: {}",
                    newProductParameters.size(), extProduct.getId());
        } else {
            product.setProductParameters(new HashSet<>());

            if (extProduct.getParameters() == null) {
                log.debug("No parameters provided for product with externalId: {}", extProduct.getId());
            }
            if (product.getCategory() == null) {
                log.warn("Product category is null for product with externalId: {}", extProduct.getId());
            }
        }
    }

    private String generateSlug(String name) {
        return name == null ? null :
                name.toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "");
    }

    private Category createCategoryFromExternal(CategoryRequestDto extCategory) {
        Category category = new Category();
        category.setExternalId(extCategory.getId());
        category.setShow(extCategory.getShow());
        category.setSortOrder(extCategory.getOrder());

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
        category.setSlug(generateSlug(baseName));

        return category;
    }

    private void updateCategoryFromExternal(Category category, CategoryRequestDto extCategory) {
        category.setShow(extCategory.getShow());
        category.setSortOrder(extCategory.getOrder());

        if (extCategory.getName() != null) {
            extCategory.getName().forEach(name -> {
                if ("bg".equals(name.getLanguageCode())) {
                    category.setNameBg(name.getText());
                } else if ("en".equals(name.getLanguageCode())) {
                    category.setNameEn(name.getText());
                }
            });
        }

        if (category.getSlug() == null || category.getSlug().isEmpty()) {
            String baseName = category.getNameEn() != null ? category.getNameEn() : category.getNameBg();
            category.setSlug(generateSlug(baseName));
        }
    }

    private void updateCategoryParents(List<CategoryRequestDto> externalCategories, Map<Long, Category> existingCategories) {
        for (CategoryRequestDto extCategory : externalCategories) {
            if (extCategory.getParent() != null && extCategory.getParent() != 0) {
                Category category = existingCategories.get(extCategory.getId());
                Category parent = existingCategories.get(extCategory.getParent());

                if (category != null && parent != null) {
                    category.setParent(parent);
                    categoryRepository.save(category);
                }
            }
        }
    }

    private Manufacturer createManufacturerFromExternal(ManufacturerRequestDto extManufacturer) {
        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setExternalId(extManufacturer.getId());
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

    private Parameter createParameterFromExternal(ParameterRequestDto extParameter, Category category) {
        Parameter parameter = new Parameter();
        parameter.setExternalId(extParameter.getId());
        parameter.setCategory(category);
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

    private ParameterOption createParameterOptionFromExternal(ParameterOptionRequestDto extOption, Parameter parameter) {
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
    }

    private void updateParameterOptionFromExternal(ParameterOption option, ParameterOptionRequestDto extOption) {
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
    }
}