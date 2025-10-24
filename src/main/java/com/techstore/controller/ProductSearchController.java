package com.techstore.controller;

import com.techstore.dto.request.ProductSearchRequest;
import com.techstore.dto.response.FacetValue;
import com.techstore.dto.response.ProductSearchResponse;
import com.techstore.service.ProductSearchService;
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

    @PostMapping("/search")
    public ResponseEntity<ProductSearchResponse> searchProducts(
            @RequestBody ProductSearchRequest request) {

        log.info("Search request: query='{}', filters={}, page={}",
                request.getQuery(), request.getFilters(), request.getPage());

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

    @GetMapping("/categories/{categoryId}/parameters")
    public ResponseEntity<Map<String, List<FacetValue>>> getCategoryParameters(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "bg") String language) {

        log.info("Fetching parameters for category {} in language {}", categoryId, language);

        Map<String, List<FacetValue>> parameters =
                searchService.getAvailableParametersWithCountsForCategory(categoryId, language);

        return ResponseEntity.ok(parameters);
    }

    @GetMapping("/categories/{categoryId}/products")
    public ResponseEntity<ProductSearchResponse> searchByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "bg") String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchResponse response =
                searchService.searchByCategory(categoryId, language, page, size);

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
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchResponse response =
                searchService.searchProductsOnSale(language, page, size);

        return ResponseEntity.ok(response);
    }
}