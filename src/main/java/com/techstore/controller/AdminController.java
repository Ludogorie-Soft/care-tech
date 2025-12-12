package com.techstore.controller;

import com.techstore.dto.CategoryResponseDTO;
import com.techstore.dto.request.*;
import com.techstore.dto.response.*;
import com.techstore.entity.Product;
import com.techstore.enums.OrderStatus;
import com.techstore.service.*;
import com.techstore.service.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;
    private final ProductService productService;
    private final ParameterService parameterService;
    private final CategoryService categoryService;
    private final ManufacturerService manufacturerService;

    @GetMapping("/products/pageable")
    public ResponseEntity<Page<ProductResponseDTO>> getAllAdminProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "bg") String lang) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        Page<ProductResponseDTO> productsPage = productService.findAllAdminProducts(pageable, lang);

        return ResponseEntity.ok(productsPage);
    }

    @GetMapping("parameters/pageable")
    public ResponseEntity<Page<ParameterResponseDto>> getAllAdminParameters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "bg") String lang) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        Page<ParameterResponseDto> parametersPage = parameterService.findAllAdminParameters(pageable, lang);

        return ResponseEntity.ok(parametersPage);
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CategoryRequestDto requestDTO) {
        CategoryResponseDTO createdCategory = categoryService.createCategory(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    @PutMapping(value = "/categories/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDto requestDTO) {

        log.info("Updating category with id: {}", id);
        CategoryResponseDTO updatedCategory = categoryService.updateCategory(id, requestDTO);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping(value = "/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        log.info("Deleting category with id: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products/with-markup")
    public ResponseEntity<Page<ProductResponseDTO>> getProductsWithMarkup(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "bg") String lang
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        Page<ProductResponseDTO>  products = productService.findProductsWithMarkup(pageable, lang);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/categories/with-markup")
    public ResponseEntity<List<CategoryResponseDTO>> getCategoriesWithMarkupProducts() {
        List<CategoryResponseDTO> categories = categoryService.findCategoriesWithMarkupProducts();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/manufacturers/with-markup")
    public ResponseEntity<List<ManufacturerResponseDto>> getManufacturersWithMarkupProducts() {
        List<ManufacturerResponseDto> manufacturers = manufacturerService.findManufacturersWithMarkupProducts();
        return ResponseEntity.ok(manufacturers);
    }

    @PutMapping("/{id}/markup")
    public ResponseEntity<ProductResponseDTO> updateProductMarkup(
            @PathVariable Long id,
            @RequestBody @Valid ProductMarkupUpdateDTO markupData,
            @RequestParam(defaultValue = "en") String language) {

        log.info("Updating markup for product {} to {}%", id, markupData.getMarkupPercentage());
        ProductResponseDTO updatedProduct = productService.updateProductMarkup(id, markupData.getMarkupPercentage(), language);
        return ResponseEntity.ok(updatedProduct);
    }

    @PutMapping("/categories/{id}/markup")
    public ResponseEntity<MarkupResponseDTO> applyCategoryMarkup(
            @PathVariable Long id,
            @Valid @RequestBody ProductMarkupUpdateDTO markupData) {

        log.info("Applying bulk markup to category {} with {}%", id, markupData.getMarkupPercentage());
        MarkupResponseDTO response = productService.applyMarkupToCategory(id, markupData);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/manufacturers/{id}/markup")
    public ResponseEntity<MarkupResponseDTO> applyManufacturerMarkup(
            @PathVariable Long id,
            @Valid @RequestBody ProductMarkupUpdateDTO markupData) {

        log.info("Applying bulk markup to manufacturer {} with {}%", id, markupData.getMarkupPercentage());
        MarkupResponseDTO response = productService.applyMarkupToManufacturer(id, markupData);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/products/promo")
    public ResponseEntity<ProductResponseDTO> createPromo(@RequestBody ProductPromoRequest request, @RequestParam(defaultValue = "en") String language) {
        ProductResponseDTO response = adminService.createPromo(request, language);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories/promo")
    public ResponseEntity<List<CategorySummaryDTO>> getAllPromoCategories() {
        List<CategorySummaryDTO> response = adminService.findByIsCategoryPromoActive();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/manufacturers/promo")
    public ResponseEntity<List<ManufacturerSummaryDto>> getAllPromoManufacturers() {
        List<ManufacturerSummaryDto> response = adminService.findByIsManufacturerPromoActive();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/products/promo-by-category")
    public ResponseEntity<List<ProductResponseDTO>> createPromoByCategory(
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("discount")BigDecimal discount,
            @RequestParam(defaultValue = "en", name = "lang") String lang
    ) {
        List<ProductResponseDTO> response = adminService.createPromoByCategory(categoryId, discount, lang);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/products/promo-by-manufacturer")
    public ResponseEntity<List<ProductResponseDTO>> createPromoByManufacturer(
            @RequestParam("manufacturerId") Long manufacturerId,
            @RequestParam("discount") BigDecimal discount,
            @RequestParam(defaultValue = "en", name = "lang") String lang
    ) {
        List<ProductResponseDTO> response = adminService.createPromoByManufacturer(manufacturerId, discount, lang);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/parameters/filter-activate/{id}")
    public ResponseEntity<ParameterResponseDto> updateParameterVisibilityAsFilter(
            @PathVariable("id") Long id) {
        ParameterResponseDto parameter = parameterService.changeParameterVisibilityAsFilter(id);
        return ResponseEntity.ok(parameter);
    }

    // ============ ORDER MANAGEMENT ENDPOINTS ============

    /**
     * Get all orders with pagination and sorting
     */
    @GetMapping("/orders/all")
    public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponseDTO> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/parameters/category/{categoryId}/reorder")
    @Operation(summary = "Reorder parameters for a category")
    public ResponseEntity<List<ParameterResponseDto>> reorderParameters(
            @PathVariable Long categoryId,
            @RequestBody @Valid List<ParameterOrderDto> reorderDtos,
            @RequestParam(defaultValue = "en") String language) {

        log.info("Admin request to reorder parameters for category ID: {}", categoryId);
        List<ParameterResponseDto> updatedList = parameterService.reorderParameters(categoryId, reorderDtos, language);
        return ResponseEntity.ok(updatedList);
    }

    /**
     * Get orders by status with sorting by date
     *
     * Example: GET /api/admin/orders/by-status/PENDING?page=0&size=20&sortDirection=DESC
     */
    @GetMapping("/orders/by-status/{status}")
    public ResponseEntity<Page<OrderResponseDTO>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Getting orders by status: {} with sort direction: {}", status, sortDirection);

        Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponseDTO> orders = orderService.getOrdersByStatus(status, pageable);

        log.info("Found {} orders with status {}", orders.getTotalElements(), status);
        return ResponseEntity.ok(orders);
    }

    /**
     * Search orders by order number
     *
     * Example: GET /api/admin/orders/search-by-order-number?orderNumber=ORD-2025-00123&page=0&size=20
     */
    @GetMapping("/orders/search-by-order-number")
    public ResponseEntity<Page<OrderResponseDTO>> searchOrders(
            @RequestParam String orderNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Searching orders by order number: {}", orderNumber);

        Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponseDTO> orders = orderService.searchByOrderNumber(orderNumber, pageable);

        log.info("Found {} orders matching '{}'", orders.getTotalElements(), orderNumber);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get orders by date range
     *
     * Example: GET /api/admin/orders/date-range?dateFrom=2025-01-01T00:00:00&dateTo=2025-01-31T23:59:59&page=0&size=20
     */
    @GetMapping("/orders/date-range")
    public ResponseEntity<Page<OrderResponseDTO>> getOrdersByDateRange(
            @RequestParam String dateFrom,
            @RequestParam String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Getting orders from {} to {}", dateFrom, dateTo);

        LocalDateTime from = LocalDateTime.parse(dateFrom);
        LocalDateTime to = LocalDateTime.parse(dateTo);

        Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponseDTO> orders = orderService.getOrdersByDateRange(from, to, pageable);

        log.info("Found {} orders in date range", orders.getTotalElements());
        return ResponseEntity.ok(orders);
    }

    /**
     * Get latest N orders (for dashboard widgets)
     *
     * Example: GET /api/admin/orders/latest?limit=10
     */
    @GetMapping("/orders/latest")
    public ResponseEntity<List<OrderResponseDTO>> getLatestOrders(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Getting latest {} orders", limit);

        // Validate limit
        if (limit != 10 && limit != 20 && limit != 50) {
            limit = 10; // Default to 10 if invalid
        }

        List<OrderResponseDTO> orders = orderService.getLatestOrders(limit);

        log.info("Returning {} latest orders", orders.size());
        return ResponseEntity.ok(orders);
    }

    /**
     * Get comprehensive order statistics
     */
    @GetMapping("/orders/statistics")
    public ResponseEntity<OrderStatisticsResponseDTO> getOrderStatistics() {
        log.info("Getting order statistics");
        OrderStatisticsResponseDTO statistics = orderService.getOrderStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Update order status
     */
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateDTO request) {

        OrderResponseDTO order = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(order);
    }
}