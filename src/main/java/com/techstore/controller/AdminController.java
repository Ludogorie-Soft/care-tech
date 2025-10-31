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
            @RequestParam("isPromo") Boolean isPromo,
            @RequestParam(defaultValue = "en", name = "lang") String lang
    ) {
        List<ProductResponseDTO> response = adminService.createPromoByCategory(categoryId, isPromo, discount, lang);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/products/promo-by-manufacturer")
    public ResponseEntity<List<ProductResponseDTO>> createPromoByManufacturer(
            @RequestParam("manufacturerId") Long manufacturerId,
            @RequestParam("discount") BigDecimal discount,
            @RequestParam("isPromo") Boolean isPromo,
            @RequestParam(defaultValue = "en", name = "lang") String lang
    ) {
        List<ProductResponseDTO> response = adminService.createPromoByManufacturer(manufacturerId, isPromo, discount, lang);
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