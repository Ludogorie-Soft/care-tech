package com.techstore.service;

import com.techstore.dto.request.ProductCreateRequestDTO;
import com.techstore.dto.request.ProductImageOperationsDTO;
import com.techstore.dto.request.ProductImageUpdateDTO;
import com.techstore.dto.request.ProductParameterCreateDTO;
import com.techstore.dto.request.ProductUpdateRequestDTO;
import com.techstore.dto.response.*;
import com.techstore.entity.Category;
import com.techstore.entity.Manufacturer;
import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import com.techstore.entity.Product;
import com.techstore.entity.ProductParameter;
import com.techstore.enums.ProductStatus;
import com.techstore.exception.BusinessLogicException;
import com.techstore.exception.DuplicateResourceException;
import com.techstore.exception.ValidationException;
import com.techstore.mapper.ManufacturerMapper;
import com.techstore.mapper.ParameterMapper;
import com.techstore.repository.*;
import com.techstore.util.ExceptionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ParameterRepository parameterRepository;
    private final ParameterOptionRepository parameterOptionRepository;
    private final S3Service s3Service;
    private final ParameterMapper parameterMapper;
    private final ManufacturerMapper manufacturerMapper;
    private final ProductParameterRepository productParameterRepository;
    private final CacheManager cacheManager;

    private static final int MAX_IMAGES_PER_PRODUCT = 20;
    private static final int MAX_PARAMETERS_PER_PRODUCT = 100;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Transactional(readOnly = true)
    public List<ManufacturerResponseDto> getManufacturersByCategory(Long categoryId) {
        return productRepository.findManufacturersByCategoryId(categoryId).stream()
                .map(manufacturerMapper::toResponseDto).toList();
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductResponseDTO createProduct(
            ProductCreateRequestDTO productData,
            MultipartFile primaryImage,
            List<MultipartFile> additionalImages, String lang) {

        log.info("Creating product with reference: {}", productData.getReferenceNumber());
        validateProductCreateRequest(productData, true);
        validatePrimaryImage(primaryImage);
        validateAdditionalImages(additionalImages);
        checkForDuplicateProduct(productData.getReferenceNumber(), null);

        List<String> uploadedImageUrls = new ArrayList<>();
        try {
            String primaryImageUrl = uploadImageSafely(primaryImage, "products");
            uploadedImageUrls.add(primaryImageUrl);

            List<String> additionalImageUrls = uploadAdditionalImages(additionalImages, uploadedImageUrls);

            Product product = createProductFromCreateRequest(productData);
            product.setPrimaryImageUrl(primaryImageUrl);
            if (!additionalImageUrls.isEmpty()) {
                product.setAdditionalImages(additionalImageUrls);
            }

            product = productRepository.save(product);
            log.info("Product created successfully with id: {}", product.getId());

            clearProductCache();

            return convertToResponseDTO(product, lang);

        } catch (Exception e) {
            log.error("Product creation failed, cleaning up uploaded images", e);
            cleanupImagesOnError(uploadedImageUrls);
            throw e;
        }
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductImageUploadResponseDTO addImageToProduct(Long productId, MultipartFile file, boolean isPrimary) {
        log.info("Adding image to product {} (isPrimary: {})", productId, isPrimary);
        validateProductId(productId);
        validateImageFile(file);

        Product product = findProductByIdOrThrow(productId);
        validateImageLimits(product);

        String imageUrl = uploadImageSafely(file, "products");
        try {
            updateProductImagesForAdd(product, imageUrl, isPrimary);
            productRepository.save(product);
            clearProductCache();
            return createImageUploadResponse(file, imageUrl, isPrimary, product);
        } catch (Exception e) {
            cleanupImageOnError(imageUrl);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #lang")
    public Page<ProductResponseDTO> getAllProducts(Pageable pageable, String lang) {
        validatePaginationParameters(pageable);
        validateLanguage(lang);
        return productRepository.findByActiveTrue(pageable).map(p -> convertToResponseDTO(p, lang));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id + '_' + #lang")
    public ProductResponseDTO getProductById(Long id, String lang) {
        validateProductId(id);
        validateLanguage(lang);
        Product product = findProductByIdOrThrow(id);
        return convertToResponseDTO(product, lang);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProductsByCategory(Long categoryId, Pageable pageable, String lang) {
        validateCategoryId(categoryId);
        validatePaginationParameters(pageable);
        validateLanguage(lang);
        findCategoryByIdOrThrow(categoryId);
        return productRepository.findActiveByCategoryExcludingNotAvailable(categoryId, pageable)
                .map(p -> convertToResponseDTO(p, lang));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProductsByBrand(Long brandId, Pageable pageable, String lang) {
        validateManufacturerId(brandId);
        validatePaginationParameters(pageable);
        validateLanguage(lang);
        findManufacturerByIdOrThrow(brandId);
        return productRepository.findActiveByManufacturerExcludingNotAvailable(brandId, pageable)
                .map(p -> convertToResponseDTO(p, lang));
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getRelatedProducts(Long productId, int limit, String lang) {
        validateProductId(productId);
        validateRelatedProductsLimit(limit);
        validateLanguage(lang);
        Product product = findProductByIdOrThrow(productId);
        validateProductForRelated(product);
        Pageable pageable = Pageable.ofSize(limit);
        return productRepository.findRelatedProducts(productId, product.getCategory().getId(), product.getManufacturer().getId(), pageable)
                .stream().map(p -> convertToResponseDTO(p, lang)).toList();
    }

    @Transactional(readOnly = true)
    public Set<ProductParameterResponseDto> getProductsParametersByCategory(Long categoryId, String lang) {
        List<Object[]> results = productParameterRepository.findParameterOptionsByCategoryAndActiveProducts(categoryId);
        Map<Long, ProductParameterResponseDto> resultMap = new HashMap<>();
        for (Object[] row : results) {
            Long parameterId = (Long) row[0];
            String parameterNameEn = (String) row[1];
            String parameterNameBg = (String) row[2];
            Long optionId = (Long) row[3];
            String optionNameEn = (String) row[4];
            String optionNameBg = (String) row[5];
            Integer optionOrder = (Integer) row[6];

            ParameterOptionResponseDto optionDto = ParameterOptionResponseDto.builder()
                    .id(optionId)
                    .name("en".equals(lang) ? optionNameEn : optionNameBg)
                    .order(optionOrder != null ? optionOrder : 0)
                    .build();

            resultMap.computeIfAbsent(parameterId, k -> ProductParameterResponseDto.builder()
                            .parameterId(parameterId)
                            .parameterNameEn(parameterNameEn)
                            .parameterNameBg(parameterNameBg)
                            .options(new HashSet<>())
                            .build())
                    .getOptions().add(optionDto);
        }
        resultMap.values().forEach(param -> {
            Set<ParameterOptionResponseDto> uniqueOptions = param.getOptions().stream()
                    .sorted(Comparator.comparing(ParameterOptionResponseDto::getOrder))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            param.setOptions(uniqueOptions);
        });
        return new HashSet<>(resultMap.values());
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductResponseDTO updateProductWithImages(
            Long id,
            ProductUpdateRequestDTO productData,
            MultipartFile newPrimaryImage,
            List<MultipartFile> newAdditionalImages,
            String lang) {

        log.info("Updating product with id: {}", id);
        validateProductId(id);
        validateProductUpdateRequest(productData);
        Product product = findProductByIdOrThrow(id);
        checkForDuplicateProduct(productData.getReferenceNumber(), id);

        List<String> s3UploadsTracker = new ArrayList<>();
        List<String> s3CleanupList = new ArrayList<>();

        try {
            handleImageDeletions(product, productData, s3CleanupList);
            handleNewImageUploads(product, newPrimaryImage, newAdditionalImages, s3UploadsTracker, s3CleanupList);
            handleImageReordering(product, productData);
            validateProductHasImages(product);
            updateProductFieldsFromRest(product, productData);
            Product savedProduct = productRepository.save(product);

            if (!s3CleanupList.isEmpty()) {
                cleanupImagesOnError(s3CleanupList);
            }

            log.info("Product updated successfully with id: {}", savedProduct.getId());
            clearProductCache();
            return convertToResponseDTO(savedProduct, lang);

        } catch (Exception e) {
            log.error("Product update failed for id: {}. Rolling back S3 uploads.", id, e);
            cleanupImagesOnError(s3UploadsTracker);
            throw e;
        }
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductResponseDTO reorderProductImages(Long productId, List<ProductImageUpdateDTO> images, String lang) {
        log.info("Reordering images for product {}", productId);
        validateProductId(productId);
        Product product = findProductByIdOrThrow(productId);
        ProductUpdateRequestDTO productData = new ProductUpdateRequestDTO();
        productData.setImages(images);
        handleImageReordering(product, productData);
        Product savedProduct = productRepository.save(product);
        clearProductCache();
        return convertToResponseDTO(savedProduct, lang);
    }

    private void handleImageDeletions(Product product, ProductUpdateRequestDTO productData, List<String> s3CleanupList) {
        if (productData.getImagesToDelete() == null || productData.getImagesToDelete().isEmpty()) {
            return;
        }
        List<String> imagesToDelete = productData.getImagesToDelete();
        log.info("Deleting {} images from product {}", imagesToDelete.size(), product.getId());

        Set<String> toDeleteSet = new HashSet<>(imagesToDelete);
        String currentPrimary = product.getPrimaryImageUrl();
        List<String> currentAdditional = product.getAdditionalImages() != null ? new ArrayList<>(product.getAdditionalImages()) : new ArrayList<>();

        int totalImages = (currentPrimary != null ? 1 : 0) + currentAdditional.size();
        if (toDeleteSet.size() >= totalImages) {
            throw new BusinessLogicException("Cannot delete all images. A product must have at least one image.");
        }

        if (currentPrimary != null && toDeleteSet.contains(currentPrimary)) {
            product.setPrimaryImageUrl(null);
            s3CleanupList.add(currentPrimary);
        }

        if (!currentAdditional.isEmpty()) {
            currentAdditional.removeIf(imgUrl -> {
                if (toDeleteSet.contains(imgUrl)) {
                    s3CleanupList.add(imgUrl);
                    return true;
                }
                return false;
            });
            product.setAdditionalImages(currentAdditional);
        }

        if (product.getPrimaryImageUrl() == null && product.getAdditionalImages() != null && !product.getAdditionalImages().isEmpty()) {
            product.setPrimaryImageUrl(product.getAdditionalImages().remove(0));
        }
    }

    private void handleNewImageUploads(Product product, MultipartFile newPrimaryImage, List<MultipartFile> newAdditionalImages, List<String> s3UploadsTracker, List<String> s3CleanupList) {
        if (product.getAdditionalImages() == null) {
            product.setAdditionalImages(new ArrayList<>());
        }

        if (newPrimaryImage != null && !newPrimaryImage.isEmpty()) {
            validateImageFile(newPrimaryImage);
            String newUrl = uploadImageSafely(newPrimaryImage, "products");
            s3UploadsTracker.add(newUrl);

            if (product.getPrimaryImageUrl() != null) {
                s3CleanupList.add(product.getPrimaryImageUrl());
            }
            product.setPrimaryImageUrl(newUrl);
        }

        if (newAdditionalImages != null && !newAdditionalImages.isEmpty()) {
            validateAdditionalImages(newAdditionalImages);
            for (MultipartFile file : newAdditionalImages) {
                if (file != null && !file.isEmpty()) {
                    String newUrl = uploadImageSafely(file, "products");
                    s3UploadsTracker.add(newUrl);
                    product.getAdditionalImages().add(newUrl);
                }
            }
        }
    }

    private void handleImageReordering(Product product, ProductUpdateRequestDTO productData) {
        if (productData.getImages() == null || productData.getImages().isEmpty()) {
            return;
        }
        List<ProductImageUpdateDTO> reorderRequest = productData.getImages();
        log.info("Reordering images for product {}", product.getId());

        List<String> allCurrentImages = new ArrayList<>();
        if (product.getPrimaryImageUrl() != null) {
            allCurrentImages.add(product.getPrimaryImageUrl());
        }
        if (product.getAdditionalImages() != null) {
            allCurrentImages.addAll(product.getAdditionalImages());
        }
        Set<String> allCurrentImagesSet = new HashSet<>(allCurrentImages);

        for (ProductImageUpdateDTO dto : reorderRequest) {
            if (!allCurrentImagesSet.contains(dto.getImageUrl())) {
                throw new ValidationException("Reorder list contains an image URL that does not exist on the product: " + dto.getImageUrl());
            }
        }

        String newPrimaryUrl = reorderRequest.stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getIsPrimary()))
                .map(ProductImageUpdateDTO::getImageUrl)
                .findFirst()
                .orElseThrow(() -> new ValidationException("Exactly one image must be marked as primary in the reorder request."));

        List<String> newAdditionalImages = reorderRequest.stream()
                .filter(dto -> !newPrimaryUrl.equals(dto.getImageUrl()))
                .sorted(Comparator.comparing(ProductImageUpdateDTO::getOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ProductImageUpdateDTO::getImageUrl)
                .collect(Collectors.toList());

        product.setPrimaryImageUrl(newPrimaryUrl);
        product.setAdditionalImages(newAdditionalImages);
    }

    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        validateProductId(id);
        Product product = findProductByIdOrThrow(id);
        validateProductDeletion(product);
        List<String> allImages = collectAllProductImages(product);
        product.setActive(false);
        productRepository.save(product);
        cleanupImagesOnError(allImages);
        log.info("Product soft deleted successfully with id: {}", id);
        clearProductCache();
    }

    @CacheEvict(value = "products", allEntries = true)
    public void permanentDeleteProduct(Long id) {
        log.warn("Permanently deleting product with id: {}", id);
        validateProductId(id);
        Product product = findProductByIdOrThrow(id);
        validatePermanentProductDeletion(product);
        List<String> allImages = collectAllProductImages(product);
        productRepository.deleteById(id);
        cleanupImagesOnError(allImages);
        log.warn("Product permanently deleted successfully with id: {}", id);
        clearProductCache();
    }

    @CacheEvict(value = "products", allEntries = true)
    public void deleteProductImage(Long productId, String imageUrl) {
        log.info("Deleting image {} from product {}", imageUrl, productId);
        validateProductId(productId);
        validateImageUrl(imageUrl);
        Product product = findProductByIdOrThrow(productId);
        boolean wasDeleted = removeImageFromProduct(product, imageUrl);
        if (!wasDeleted) {
            throw new ValidationException("Image not found for this product");
        }
        ensureProductHasPrimaryImage(product);
        productRepository.save(product);
        cleanupImageOnError(imageUrl);
        log.info("Deleted image {} from product {}", imageUrl, productId);
        clearProductCache();
    }

    @Transactional(readOnly = true)
    public String getOriginalImageUrl(Long productId, boolean isPrimary, int index) {
        log.debug("Getting original image URL for product: {} (isPrimary: {}, index: {})",
                productId, isPrimary, index);

        validateProductId(productId);
        if (index < 0) {
            throw new ValidationException("Image index cannot be negative");
        }

        Product product = findProductByIdOrThrow(productId);

        String imageUrl = null;
        if (isPrimary) {
            imageUrl = product.getPrimaryImageUrl();
        } else {
            if (product.getAdditionalImages() != null && index < product.getAdditionalImages().size()) {
                imageUrl = product.getAdditionalImages().get(index);
            }
        }

        log.debug("Found image URL for product {}: {}", productId, imageUrl);
        return imageUrl;
    }

    private void validateProductId(Long id) {
        if (id == null || id <= 0) throw new ValidationException("Product ID must be a positive number");
    }
    private void validateCategoryId(Long id) {
        if (id == null || id <= 0) throw new ValidationException("Category ID must be a positive number");
    }
    private void validateManufacturerId(Long id) {
        if (id == null || id <= 0) throw new ValidationException("Manufacturer ID must be a positive number");
    }
    private void validateLanguage(String lang) {
        if (!StringUtils.hasText(lang) || !lang.matches("^(en|bg)$")) throw new ValidationException("Language must be 'en' or 'bg'");
    }
    private void validatePaginationParameters(Pageable p) {
        if (p.getPageNumber() < 0) throw new ValidationException("Page number cannot be negative");
        if (p.getPageSize() <= 0 || p.getPageSize() > 100) throw new ValidationException("Page size must be between 1 and 100");
    }
    private void validateProductCreateRequest(ProductCreateRequestDTO r, boolean isCreate) {
        if (r == null) throw new ValidationException("Product request cannot be null");
        validateReferenceNumber(r.getReferenceNumber(), isCreate);
        validateProductName(r.getNameEn(), "EN");
        validateCategoryId(r.getCategoryId());
        validateManufacturerId(r.getManufacturerId());
        validateProductStatus(r.getStatus());
        validateOptionalProductFields(r);
        validateProductParameters(r.getParameters());
    }
    private void validateProductUpdateRequest(ProductUpdateRequestDTO r) {
        validateProductCreateRequest(r, false);
        if (r.getImages() != null) validateImageUpdateList(r.getImages());
        if (r.getImagesToDelete() != null) validateImagesToDelete(r.getImagesToDelete());
    }
    private void validateReferenceNumber(String ref, boolean isRequired) {
        if (isRequired && !StringUtils.hasText(ref)) throw new ValidationException("Reference number is required");
        if (StringUtils.hasText(ref) && (ref.trim().length() > 100 || ref.trim().length() < 3)) throw new ValidationException("Reference number must be between 3 and 100 characters");
    }
    private void validateProductName(String name, String lang) {
        if (!StringUtils.hasText(name)) throw new ValidationException(String.format("Product name (%s) is required", lang));
        if (name.trim().length() > 500 || name.trim().length() < 2) throw new ValidationException(String.format("Product name (%s) must be between 2 and 500 characters", lang));
    }
    private void validateProductStatus(Integer status) {
        if (status == null || status < 0 || status > 4) throw new ValidationException("Product status must be between 0 and 4");
    }
    private void validateOptionalProductFields(ProductCreateRequestDTO r) {
        if (StringUtils.hasText(r.getNameBg()) && r.getNameBg().length() > 500) throw new ValidationException("Product name (BG) cannot exceed 500 characters");
        if (StringUtils.hasText(r.getDescriptionEn()) && r.getDescriptionEn().length() > 2000) throw new ValidationException("Product description (EN) cannot exceed 2000 characters");
        if (StringUtils.hasText(r.getDescriptionBg()) && r.getDescriptionBg().length() > 2000) throw new ValidationException("Product description (BG) cannot exceed 2000 characters");
        if (StringUtils.hasText(r.getModel()) && r.getModel().length() > 100) throw new ValidationException("Product model cannot exceed 100 characters");
        if (StringUtils.hasText(r.getBarcode()) && r.getBarcode().length() > 50) throw new ValidationException("Product barcode cannot exceed 50 characters");
        validateProductPrices(r);
        validateProductMeasurements(r);
    }
    private void validateProductPrices(ProductCreateRequestDTO r) {
        if (r.getPriceClient() != null && r.getPriceClient().compareTo(BigDecimal.ZERO) < 0) throw new ValidationException("Client price cannot be negative");
        if (r.getPricePartner() != null && r.getPricePartner().compareTo(BigDecimal.ZERO) < 0) throw new ValidationException("Partner price cannot be negative");
    }
    private void validateProductMeasurements(ProductCreateRequestDTO r) {
        if (r.getWeight() != null && r.getWeight().compareTo(BigDecimal.ZERO) < 0) throw new ValidationException("Product weight cannot be negative");
        if (r.getWarranty() != null && r.getWarranty() < 0) throw new ValidationException("Product warranty cannot be negative");
    }
    private void validateProductParameters(List<ProductParameterCreateDTO> params) {
        if (params == null) return;
        if (params.stream().mapToInt(p -> p.getParameterOptionId().size()).sum() > MAX_PARAMETERS_PER_PRODUCT) throw new ValidationException("Product cannot have more than " + MAX_PARAMETERS_PER_PRODUCT + " parameter options in total");
        Set<String> uniqueParamOptions = new HashSet<>();
        for (ProductParameterCreateDTO p : params) {
            if (p.getParameterId() == null) throw new ValidationException("Parameter ID is required");
            if (p.getParameterOptionId() == null || p.getParameterOptionId().isEmpty()) throw new ValidationException("Parameter option ID list cannot be empty for parameter: " + p.getParameterId());
            for (Long optionId : p.getParameterOptionId()) {
                if (optionId == null) throw new ValidationException("Parameter option ID cannot be null");
                if (!uniqueParamOptions.add(p.getParameterId() + ":" + optionId)) throw new ValidationException("Duplicate parameter-option combination found");
            }
        }
    }
    private void validatePrimaryImage(MultipartFile img) {
        if (img == null || img.isEmpty()) throw new ValidationException("Primary image is required");
        validateImageFile(img);
    }
    private void validateAdditionalImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return;
        if (images.size() > MAX_IMAGES_PER_PRODUCT - 1) throw new ValidationException("Cannot have more than " + (MAX_IMAGES_PER_PRODUCT - 1) + " additional images");
        images.forEach(this::validateImageFile);
    }
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ValidationException("Image file cannot be empty");
        if (file.getSize() > MAX_FILE_SIZE) throw new ValidationException("Image file size cannot exceed " + MAX_FILE_SIZE + " bytes");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) throw new ValidationException("File must be an image");
        if (!List.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp").contains(contentType.toLowerCase())) throw new ValidationException("Image type not allowed");
    }
    private void validateImageLimits(Product p) {
        int currentImageCount = (p.getPrimaryImageUrl() != null ? 1 : 0) + (p.getAdditionalImages() != null ? p.getAdditionalImages().size() : 0);
        if (currentImageCount >= MAX_IMAGES_PER_PRODUCT) throw new BusinessLogicException("Product already has maximum of " + MAX_IMAGES_PER_PRODUCT + " images");
    }
    private void validateSearchQuery(String q) {
        if (!StringUtils.hasText(q) || q.trim().length() > 200 || q.trim().length() < 2) throw new ValidationException("Search query must be between 2 and 200 characters");
    }
    private void validateRelatedProductsLimit(int limit) {
        if (limit <= 0 || limit > 50) throw new ValidationException("Related products limit must be between 1 and 50");
    }
    private void validateImageUrl(String url) {
        if (!StringUtils.hasText(url)) throw new ValidationException("Image URL cannot be empty");
    }
    private void validateProductForRelated(Product p) {
        if (p.getCategory() == null) throw new BusinessLogicException("Product must have a category to find related products");
        if (p.getManufacturer() == null) throw new BusinessLogicException("Product must have a manufacturer to find related products");
    }
    private void validateProductDeletion(Product p) { /* No-op for now, can add checks for orders etc. */ }
    private void validatePermanentProductDeletion(Product p) {
        validateProductDeletion(p);
        if (p.getActive()) throw new BusinessLogicException("Product must be deactivated before permanent deletion");
    }
    private void validateImageUpdateList(List<ProductImageUpdateDTO> images) {
        if (images.isEmpty()) return;
        for (ProductImageUpdateDTO img : images) if (!StringUtils.hasText(img.getImageUrl())) throw new ValidationException("Image URL cannot be empty in update list");
    }
    private void validateImagesToDelete(List<String> urls) {
        for (String url : urls) if (!StringUtils.hasText(url)) throw new ValidationException("Image URL to delete cannot be empty");
    }
    private void validateProductHasImages(Product p) {
        if (p.getPrimaryImageUrl() == null && (p.getAdditionalImages() == null || p.getAdditionalImages().isEmpty())) throw new BusinessLogicException("Product must have at least one image");
    }
    public Product findProductByIdOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new BusinessLogicException("Product not found with id: " + id));
    }
    private Category findCategoryByIdOrThrow(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new BusinessLogicException("Category not found with id: " + id));
    }
    private Manufacturer findManufacturerByIdOrThrow(Long id) {
        return manufacturerRepository.findById(id).orElseThrow(() -> new BusinessLogicException("Manufacturer not found with id: " + id));
    }
    private void checkForDuplicateProduct(String ref, Long excludeId) {
        if (!StringUtils.hasText(ref)) return;
        productRepository.findByReferenceNumber(ref).ifPresent(p -> {
            if (excludeId == null || !p.getId().equals(excludeId)) {
                throw new DuplicateResourceException("Product already exists with reference number: " + ref);
            }
        });
    }
    private String uploadImageSafely(MultipartFile file, String folder) {
        try {
            return s3Service.uploadProductImage(file, folder);
        } catch (Exception e) {
            log.error("Error uploading image: {}", e.getMessage(), e);
            throw new BusinessLogicException("Failed to upload image: " + e.getMessage());
        }
    }
    private List<String> uploadAdditionalImages(List<MultipartFile> images, List<String> tracker) {
        if (images == null || images.isEmpty()) return Collections.emptyList();
        return images.stream().filter(i -> i != null && !i.isEmpty()).map(i -> {
            String url = uploadImageSafely(i, "products");
            tracker.add(url);
            return url;
        }).collect(Collectors.toList());
    }
    private void cleanupImagesOnError(List<String> urls) {
        if (urls != null && !urls.isEmpty()) s3Service.deleteImages(urls);
    }
    private void cleanupImageOnError(String url) {
        if (StringUtils.hasText(url)) s3Service.deleteImage(url);
    }
    private List<String> collectAllProductImages(Product p) {
        List<String> all = new ArrayList<>();
        if (p.getPrimaryImageUrl() != null) all.add(p.getPrimaryImageUrl());
        if (p.getAdditionalImages() != null) all.addAll(p.getAdditionalImages());
        return all;
    }
    private void updateProductImagesForAdd(Product p, String url, boolean isPrimary) {
        if (p.getAdditionalImages() == null) p.setAdditionalImages(new ArrayList<>());
        if (isPrimary) {
            if (p.getPrimaryImageUrl() != null) p.getAdditionalImages().add(0, p.getPrimaryImageUrl());
            p.setPrimaryImageUrl(url);
        } else {
            p.getAdditionalImages().add(url);
        }
    }
    private boolean removeImageFromProduct(Product p, String url) {
        if (url.equals(p.getPrimaryImageUrl())) {
            p.setPrimaryImageUrl(null);
            return true;
        }
        return p.getAdditionalImages() != null && p.getAdditionalImages().remove(url);
    }
    private void ensureProductHasPrimaryImage(Product p) {
        if (p.getPrimaryImageUrl() == null) {
            if (p.getAdditionalImages() != null && !p.getAdditionalImages().isEmpty()) {
                p.setPrimaryImageUrl(p.getAdditionalImages().remove(0));
            } else {
                throw new BusinessLogicException("Cannot delete last image. Product must have at least one image.");
            }
        }
    }
    private ProductImageUploadResponseDTO createImageUploadResponse(MultipartFile f, String url, boolean isPrimary, Product p) {
        int order = isPrimary ? 0 : (p.getAdditionalImages() != null ? p.getAdditionalImages().size() : 1);
        return ProductImageUploadResponseDTO.builder().imageUrl(url).fileName(f.getOriginalFilename()).fileSize(f.getSize()).contentType(f.getContentType()).isPrimary(isPrimary).order(order).build();
    }
    private Product createProductFromCreateRequest(ProductCreateRequestDTO dto) {
        Product p = new Product();
        updateProductFieldsFromRest(p, dto);
        return p;
    }
    private void updateProductFieldsFromRest(Product p, ProductCreateRequestDTO dto) {
        p.setReferenceNumber(dto.getReferenceNumber());
        p.setNameEn(dto.getNameEn());
        p.setNameBg(dto.getNameBg());
        p.setDescriptionEn(dto.getDescriptionEn());
        p.setDescriptionBg(dto.getDescriptionBg());
        p.setModel(dto.getModel());
        p.setBarcode(dto.getBarcode());
        p.setCategory(findCategoryByIdOrThrow(dto.getCategoryId()));
        p.setManufacturer(findManufacturerByIdOrThrow(dto.getManufacturerId()));
        p.setStatus(ProductStatus.fromCode(dto.getStatus()));
        p.setPriceClient(dto.getPriceClient());
        p.setPricePartner(dto.getPricePartner());
        p.setPricePromo(dto.getPricePromo());
        p.setPriceClientPromo(dto.getPriceClientPromo());
        p.setMarkupPercentage(dto.getMarkupPercentage());
        p.setShow(dto.getShow());
        p.setWarranty(dto.getWarranty());
        p.setWeight(dto.getWeight());
        p.setActive(dto.getActive());
        p.setFeatured(dto.getFeatured());
        p.calculateFinalPrice();
        setParametersFromRest(p, dto.getParameters());
    }
    private void setParametersFromRest(Product p, List<ProductParameterCreateDTO> params) {
        if (params == null) {
            p.setProductParameters(new HashSet<>());
            return;
        }
        Set<ProductParameter> newProductParameters = new HashSet<>();
        for (ProductParameterCreateDTO paramDto : params) {
            Parameter parameter = parameterRepository.findById(paramDto.getParameterId()).orElseThrow(() -> new BusinessLogicException("Parameter not found: " + paramDto.getParameterId()));
            for (Long optionId : paramDto.getParameterOptionId()) {
                ParameterOption option = parameterOptionRepository.findById(optionId).orElseThrow(() -> new BusinessLogicException("ParameterOption not found: " + optionId));
                if (!option.getParameter().getId().equals(parameter.getId())) throw new ValidationException(String.format("Parameter option %d does not belong to parameter %d", optionId, paramDto.getParameterId()));
                ProductParameter pp = new ProductParameter();
                pp.setProduct(p);
                pp.setParameter(parameter);
                pp.setParameterOption(option);
                newProductParameters.add(pp);
            }
        }
        p.setProductParameters(newProductParameters);
    }
    private ProductResponseDTO convertToResponseDTO(Product p, String lang) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(p.getId());
        dto.setNameEn(p.getNameEn());
        dto.setNameBg(p.getNameBg());
        dto.setDescriptionEn(p.getDescriptionEn());
        dto.setDescriptionBg(p.getDescriptionBg());
        dto.setSlug(p.getSlug());
        dto.setReferenceNumber(p.getReferenceNumber());
        dto.setModel(p.getModel());
        dto.setBarcode(p.getBarcode());
        dto.setPriceClient(p.getPriceClient());
        dto.setPricePartner(p.getPricePartner());
        dto.setPricePromo(p.getPricePromo());
        dto.setPriceClientPromo(p.getPriceClientPromo());
        dto.setMarkupPercentage(p.getMarkupPercentage());
        dto.setFinalPrice(p.getFinalPrice());
        dto.setDiscount(p.getDiscount());
        dto.setActive(p.getActive());
        dto.setFeatured(p.getFeatured());
        dto.setShow(p.getShow());
        if (p.getPrimaryImageUrl() != null) dto.setPrimaryImageUrl(p.getPrimaryImageUrl());
        if (p.getAdditionalImages() != null) dto.setAdditionalImages(p.getAdditionalImages());
//        if (p.getPrimaryImageUrl() != null) dto.setPrimaryImageUrl("/api/images/product/" + p.getId() + "/primary");
//        if (p.getAdditionalImages() != null && !p.getAdditionalImages().isEmpty()) {
//            List<String> proxyUrls = new ArrayList<>();
//            for (int i = 0; i < p.getAdditionalImages().size(); i++) proxyUrls.add("/api/images/product/" + p.getId() + "/additional/" + i);
//            dto.setAdditionalImages(proxyUrls);
//        }
        dto.setWarranty(p.getWarranty());
        dto.setWeight(p.getWeight());
        Map<Long, ProductParameterResponseDto> uniqueSpecs = new HashMap<>();
        for (ProductParameter pp : p.getProductParameters()) {
            if (pp.getParameter() == null || pp.getParameterOption() == null) continue;
            uniqueSpecs.computeIfAbsent(pp.getParameter().getId(), k -> convertToProductParameterResponse(pp, lang))
                    .getOptions().add(parameterMapper.toOptionResponseDto(pp.getParameterOption(), lang));
        }
        dto.setSpecifications(new ArrayList<>(uniqueSpecs.values()));
        if (p.getCategory() != null) dto.setCategory(convertToCategorySummary(p.getCategory()));
        if (p.getManufacturer() != null) dto.setManufacturer(convertToManufacturerSummary(p.getManufacturer()));
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        dto.setOnSale(p.isOnSale());
        dto.setStatus(p.getStatus() != null ? p.getStatus().getCode() : 0);
        dto.setWorkflowId(p.getWorkflowId());
        return dto;
    }
    private ProductParameterResponseDto convertToProductParameterResponse(ProductParameter pp, String lang) {
        Parameter param = pp.getParameter();
        return ProductParameterResponseDto.builder()
                .parameterId(param.getId())
                .parameterNameEn(param.getNameEn())
                .parameterNameBg(param.getNameBg())
                .options(new HashSet<>())
                .build();
    }
    private CategorySummaryDTO convertToCategorySummary(Category c) {
        return CategorySummaryDTO.builder().id(c.getId()).nameEn(c.getNameEn()).nameBg(c.getNameBg()).slug(c.getSlug()).show(c.getShow()).parent(c.getParent() != null ? convertToCategorySummary(c.getParent()) : null).build();
    }
    private ManufacturerSummaryDto convertToManufacturerSummary(Manufacturer m) {
        return ManufacturerSummaryDto.builder().id(m.getId()).name(m.getName()).build();
    }

    private void clearProductCache() {
        Cache cache = cacheManager.getCache("products");
        if (cache != null) {
            cache.clear();
        }
    }
}