package com.techstore.controller;

import com.techstore.dto.filter.CategoryFiltersRequest;
import com.techstore.dto.filter.CategoryFiltersResponse;
import com.techstore.dto.request.ProductSearchRequest;
import com.techstore.dto.response.ProductSearchResponse;
import com.techstore.service.ProductSearchService;
import com.techstore.service.filter.CategoryFilterService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductSearchController {

    private final ProductSearchService searchService;
    private final CategoryFilterService categoryFilterService;

    @PostMapping("/search")
    public ResponseEntity<ProductSearchResponse> searchProducts(
            @RequestBody ProductSearchRequest request) {

        log.info("Search request: query='{}', attributeFilters={}, page={}",
                request.getQuery(), request.getAttributeFilters(), request.getPage());

        ProductSearchResponse response = searchService.searchProducts(request);

        log.info("Search completed: {} results in {}ms",
                response.getTotalElements(), response.getSearchTime());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ProductSearchResponse> searchProductsSimple(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "bg") String lang,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> manufacturers,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {

        ProductSearchRequest request = ProductSearchRequest.builder()
                .query(q)
                .language(lang)
                .categories(categories)
                .manufacturers(manufacturers)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .active(true)
                .build();

        return searchProducts(request);
    }

    /** Filter panel of a category from the canonical filter layer (groups, values, counts). */
    @PostMapping("/categories/{categoryId}/filters")
    public ResponseEntity<CategoryFiltersResponse> getCategoryFilters(
            @PathVariable Long categoryId,
            @RequestBody(required = false) CategoryFiltersRequest request) {
        return ResponseEntity.ok(categoryFilterService.getFilters(categoryId, request, isAdmin()));
    }

    /** From the token's authorities, so the call that runs on every filter click costs no user lookup. */
    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    /** Translates a pre-V38 {@code param_<id>=<optionIds>} selection into canonical attribute filters. */
    @PostMapping("/categories/{categoryId}/filters/translate-legacy")
    public ResponseEntity<Map<Long, List<Long>>> translateLegacyFilters(
            @PathVariable Long categoryId,
            @RequestBody Map<Long, List<Long>> legacyFilters) {
        return ResponseEntity.ok(categoryFilterService.translateLegacy(categoryId, legacyFilters));
    }

    @GetMapping("/categories/{categoryId}/products")
    public ResponseEntity<ProductSearchResponse> searchByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "bg") String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "price_asc") String sortBy) {

        ProductSearchResponse response =
                searchService.searchByCategory(categoryId, language, page, size, sortBy);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/featured")
    public ResponseEntity<ProductSearchResponse> getFeaturedProducts(
            @RequestParam(defaultValue = "bg") String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchResponse response =
                searchService.searchFeaturedProducts(language, page, size);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sale")
    public ResponseEntity<ProductSearchResponse> getProductsOnSale(
            @RequestParam(defaultValue = "bg") String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "price_asc") String sortBy) {

        ProductSearchResponse response =
                searchService.searchProductsOnSale(language, page, size, sortBy);

        return ResponseEntity.ok(response);
    }
}