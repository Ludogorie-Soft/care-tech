package com.techstore.controller;

import com.techstore.dto.request.ProductPromoRequest;
import com.techstore.dto.response.ProductResponseDTO;
import com.techstore.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
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

    @PutMapping("/products/promo")
    public ResponseEntity<ProductResponseDTO> createPromo(@RequestBody ProductPromoRequest request, @RequestParam(defaultValue = "en") String language) {
        ProductResponseDTO response = adminService.createPromo(request, language);
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
}
