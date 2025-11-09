package com.techstore.controller;

import com.techstore.dto.request.OrderStatusUpdateDTO;
import com.techstore.dto.request.ProductPromoRequest;
import com.techstore.dto.response.CategorySummaryDTO;
import com.techstore.dto.response.ManufacturerSummaryDto;
import com.techstore.dto.response.OrderResponseDTO;
import com.techstore.dto.response.OrderStatisticsResponseDTO;
import com.techstore.dto.response.ProductResponseDTO;
import com.techstore.enums.OrderStatus;
import com.techstore.service.OrderService;
import com.techstore.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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