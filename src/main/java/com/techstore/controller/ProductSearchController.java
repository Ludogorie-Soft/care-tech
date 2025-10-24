package com.techstore.controller;

import com.techstore.dto.request.ProductSearchRequest;
import com.techstore.dto.response.FacetValue;
import com.techstore.dto.response.ProductSearchResponse;
import com.techstore.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for product search with ID-based parameter filtering
 *
 * Example Usage Flow:
 *
 * 1. Get available parameters for a category:
 *    GET /api/categories/15/parameters?language=bg
 *
 * 2. Build filter request with parameter/option IDs:
 *    POST /api/products/search
 *    {
 *      "filters": {
 *        "5": [12, 13],  // RAM: 8GB or 16GB
 *        "7": [25]       // Color: Black
 *      }
 *    }
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ProductSearchController {

    private final ProductSearchService searchService;

    /**
     * Main search endpoint with ID-based filtering
     *
     * POST /api/products/search
     *
     * Request body:
     * {
     *   "query": "laptop",
     *   "language": "bg",
     *   "filters": {
     *     "5": [12, 13],    // Parameter ID 5 (RAM), Options 12 (8GB), 13 (16GB)
     *     "7": [25]         // Parameter ID 7 (Color), Option 25 (Black)
     *   },
     *   "minPrice": 500.00,
     *   "maxPrice": 2000.00,
     *   "categories": ["15"],
     *   "page": 0,
     *   "size": 20,
     *   "sortBy": "price_asc"
     * }
     */
    @PostMapping("/products/search")
    public ResponseEntity<ProductSearchResponse> searchProducts(
            @RequestBody ProductSearchRequest request) {

        log.info("Search request: query='{}', filters={}, page={}",
                request.getQuery(), request.getFilters(), request.getPage());

        ProductSearchResponse response = searchService.searchProducts(request);

        log.info("Search completed: {} results in {}ms",
                response.getTotalElements(), response.getSearchTime());

        return ResponseEntity.ok(response);
    }

    /**
     * Get available parameters and their options for a category
     * Returns IDs needed for filtering
     *
     * GET /api/categories/{categoryId}/parameters?language=bg
     *
     * Response:
     * {
     *   "5:RAM": [
     *     { "id": 12, "value": "8GB", "count": 45, "selected": false },
     *     { "id": 13, "value": "16GB", "count": 23, "selected": false }
     *   ],
     *   "7:Цвят": [
     *     { "id": 25, "value": "Черен", "count": 67, "selected": false }
     *   ]
     * }
     */
    @GetMapping("/categories/{categoryId}/parameters")
    public ResponseEntity<Map<String, List<FacetValue>>> getCategoryParameters(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "bg") String language) {

        log.info("Fetching parameters for category {} in language {}", categoryId, language);

        Map<String, List<FacetValue>> parameters =
                searchService.getAvailableParametersWithCountsForCategory(categoryId, language);

        return ResponseEntity.ok(parameters);
    }

    /**
     * Get autocomplete suggestions
     *
     * GET /api/products/suggestions?q=lap&language=bg&max=10
     */
    @GetMapping("/products/suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "bg") String language,
            @RequestParam(defaultValue = "10") int max) {

        List<String> suggestions = searchService.getSearchSuggestions(query, language, max);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Helper endpoint: Search by category
     *
     * GET /api/categories/15/products?language=bg&page=0&size=20
     */
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

    /**
     * Helper endpoint: Get featured products
     *
     * GET /api/products/featured?language=bg&page=0&size=20
     */
    @GetMapping("/products/featured")
    public ResponseEntity<ProductSearchResponse> getFeaturedProducts(
            @RequestParam(defaultValue = "bg") String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchResponse response =
                searchService.searchFeaturedProducts(language, page, size);

        return ResponseEntity.ok(response);
    }

    /**
     * Helper endpoint: Get products on sale
     *
     * GET /api/products/sale?language=bg&page=0&size=20
     */
    @GetMapping("/products/sale")
    public ResponseEntity<ProductSearchResponse> getProductsOnSale(
            @RequestParam(defaultValue = "bg") String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchResponse response =
                searchService.searchProductsOnSale(language, page, size);

        return ResponseEntity.ok(response);
    }
}