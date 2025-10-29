package com.techstore.controller;

import com.techstore.dto.request.OrderStatusUpdateDTO;
import com.techstore.dto.request.ProductPromoRequest;
import com.techstore.dto.response.CategorySummaryDTO;
import com.techstore.dto.response.ManufacturerSummaryDto;
import com.techstore.dto.response.OrderResponseDTO;
import com.techstore.dto.response.ProductResponseDTO;
import com.techstore.service.OrderService;
import com.techstore.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/orders/all")
    public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponseDTO> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateDTO request) {

        OrderResponseDTO order = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(order);
    }
}
