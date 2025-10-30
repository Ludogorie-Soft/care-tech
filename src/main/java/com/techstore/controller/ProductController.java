package com.techstore.controller;

import com.techstore.dto.response.ManufacturerResponseDto;
import com.techstore.dto.response.ProductResponseDTO;
import com.techstore.dto.request.ProductCreateRequestDTO;
import com.techstore.dto.request.ProductImageOperationsDTO;
import com.techstore.dto.request.ProductImageUpdateDTO;
import com.techstore.dto.request.ProductUpdateRequestDTO;
import com.techstore.dto.response.ProductImageUploadResponseDTO;
import com.techstore.service.ProductService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nameEn") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "en") String language) {

        SortInfo sortInfo = parseSortBy(sortBy, sortDir);

        Sort sort = sortInfo.direction().equalsIgnoreCase("desc") ?
                Sort.by(sortInfo.field()).descending() : Sort.by(sortInfo.field()).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductResponseDTO> products = productService.getAllProducts(pageable, language);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/manufacturers")
    public ResponseEntity<List<ManufacturerResponseDto>> getManufacturersByCategoryId(
            @RequestParam("categoryId") Long categoryId
    ) {
        List<ManufacturerResponseDto> response = productService.getManufacturersByCategory(categoryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve detailed product information")
    public ResponseEntity<ProductResponseDTO> getProductById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "en") String language) {
        ProductResponseDTO product = productService.getProductById(id, language);
        return ResponseEntity.ok(product);
    }

    @GetMapping(value = "/category/{categoryId}")
    public ResponseEntity<Page<ProductResponseDTO>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "en") String language) {

        SortInfo sortInfo = parseSortBy(sortBy, sortDir);

        log.debug("Category {} - sortBy: '{}' -> field: '{}', direction: '{}'",
                categoryId, sortBy, sortInfo.field(), sortInfo.direction());

        Sort sort = sortInfo.direction().equalsIgnoreCase("desc") ?
                Sort.by(sortInfo.field()).descending() : Sort.by(sortInfo.field()).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductResponseDTO> products = productService.getProductsByCategory(categoryId, pageable, language);
        return ResponseEntity.ok(products);
    }

    @GetMapping(value = "/brand/{brandId}")
    @Operation(summary = "Get products by brand", description = "Retrieve products filtered by manufacturer/brand")
    public ResponseEntity<Page<ProductResponseDTO>> getProductsByBrand(
            @PathVariable Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nameEn") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "en") String language) {

        SortInfo sortInfo = parseSortBy(sortBy, sortDir);

        Sort sort = sortInfo.direction().equalsIgnoreCase("desc") ?
                Sort.by(sortInfo.field()).descending() : Sort.by(sortInfo.field()).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductResponseDTO> products = productService.getProductsByBrand(brandId, pageable, language);
        return ResponseEntity.ok(products);
    }

    /**
     * Parses sortBy parameter and extracts field name and direction.
     * Supports formats like: "price_asc", "price_desc", or just "price"
     *
     * @param sortBy the sort field name (may contain _asc or _desc suffix)
     * @param defaultDirection the default sort direction if not specified in sortBy
     * @return SortInfo containing the validated field name and direction
     */
    private SortInfo parseSortBy(String sortBy, String defaultDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return new SortInfo("id", defaultDirection);
        }

        String lower = sortBy.toLowerCase().trim();
        String direction = defaultDirection;

        // Extract direction from suffix
        if (lower.endsWith("_desc")) {
            direction = "desc";
            lower = lower.substring(0, lower.length() - 5); // Remove "_desc"
            log.debug("Extracted 'desc' from sortBy '{}'", sortBy);
        } else if (lower.endsWith("_asc")) {
            direction = "asc";
            lower = lower.substring(0, lower.length() - 4); // Remove "_asc"
            log.debug("Extracted 'asc' from sortBy '{}'", sortBy);
        }

        // Map field name
        String field = switch (lower) {
            case "price", "finalprice" -> "finalPrice";
            case "priceclient" -> "priceClient";
            case "pricepartner" -> "pricePartner";
            case "name", "nameen" -> "nameEn";
            case "namebg" -> "nameBg";
            case "created", "createdat" -> "createdAt";
            case "updated", "updatedat" -> "updatedAt";
            case "model" -> "model";
            case "reference", "referencenumber" -> "referenceNumber";
            case "featured" -> "featured";
            case "active" -> "active";
            case "discount" -> "discount";
            case "id" -> "id";
            case "barcode" -> "barcode";
            default -> {
                // Check if it's already a valid field name
                List<String> validFields = List.of(
                        "id", "nameEn", "nameBg", "finalPrice", "priceClient",
                        "pricePartner", "model", "referenceNumber", "createdAt",
                        "updatedAt", "featured", "active", "discount", "barcode"
                );

                if (validFields.contains(sortBy)) {
                    yield sortBy;
                } else {
                    log.warn("Invalid sort field '{}', using default 'id'", sortBy);
                    yield "id";
                }
            }
        };

        log.debug("Parsed sortBy '{}' -> field: '{}', direction: '{}'", sortBy, field, direction);
        return new SortInfo(field, direction);
    }

    /**
     * Record to hold sort field and direction information
     */
    private record SortInfo(String field, String direction) {}

    @Hidden
    @GetMapping(value = "/{id}/related")
    @Operation(summary = "Get related products", description = "Get products related to the specified product")
    public ResponseEntity<List<ProductResponseDTO>> getRelatedProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(defaultValue = "en") String language) {

        List<ProductResponseDTO> relatedProducts = productService.getRelatedProducts(id, limit, language);
        return ResponseEntity.ok(relatedProducts);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create product", description = "Create a new product with required images in single operation")
    public ResponseEntity<ProductResponseDTO> createProduct(
            @RequestPart("product") @Valid ProductCreateRequestDTO productData,
            @RequestPart("primaryImage") MultipartFile primaryImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages,
            @RequestParam(defaultValue = "en") String language) {

        log.info("Creating product with reference number: {} and {} images",
                productData.getReferenceNumber(),
                1 + (additionalImages != null ? additionalImages.size() : 0));

        ProductResponseDTO createdProduct = productService.createProduct(
                productData, primaryImage, additionalImages, language);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update product with image management", description = "Update product and manage images in single operation")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") @Valid ProductUpdateRequestDTO productData,
            @RequestPart(value = "newPrimaryImage", required = false) MultipartFile newPrimaryImage,
            @RequestPart(value = "newAdditionalImages", required = false) List<MultipartFile> newAdditionalImages,
            @RequestPart(value = "imageOperations", required = false) ProductImageOperationsDTO imageOperations,
            @RequestParam(defaultValue = "en") String language) {

        log.info("Updating product with id: {} with image operations", id);

        ProductResponseDTO updatedProduct = productService.updateProductWithImages(
                id, productData, newPrimaryImage, newAdditionalImages, imageOperations, language);

        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Soft delete a product (Admin only)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product with id: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "Permanently delete product", description = "Permanently delete a product (Super Admin only)")
    public ResponseEntity<Void> permanentDeleteProduct(@PathVariable Long id) {
        log.info("Permanently deleting product with id: {}", id);
        productService.permanentDeleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Hidden
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add image to existing product", description = "Add single image to existing product")
    public ResponseEntity<ProductImageUploadResponseDTO> addImageToProduct(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean isPrimary) {

        log.info("Adding image to existing product {} (isPrimary: {})", id, isPrimary);
        ProductImageUploadResponseDTO response = productService.addImageToProduct(id, file, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Hidden
    @DeleteMapping("/{id}/images")
    @Operation(summary = "Delete product image", description = "Delete specific image from product")
    public ResponseEntity<Void> deleteProductImage(
            @PathVariable Long id,
            @RequestParam String imageUrl) {

        log.info("Deleting image {} from product {}", imageUrl, id);
        productService.deleteProductImage(id, imageUrl);
        return ResponseEntity.noContent().build();
    }

    @Hidden
    @PutMapping(value = "/{id}/images/reorder")
    @Operation(summary = "Reorder product images", description = "Reorder existing product images")
    public ResponseEntity<ProductResponseDTO> reorderProductImages(
            @PathVariable Long id,
            @RequestBody @Valid List<ProductImageUpdateDTO> images,
            @RequestParam(defaultValue = "en") String language) {

        log.info("Reordering images for product {}", id);
        ProductResponseDTO updatedProduct = productService.reorderProductImages(id, images, language);
        return ResponseEntity.ok(updatedProduct);
    }
}