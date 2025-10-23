package com.techstore.controller;

import com.techstore.dto.request.ProductSearchRequest;
import com.techstore.dto.response.FacetValue;
import com.techstore.dto.response.ProductSearchResponse;
import com.techstore.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Product Search with advanced filtering
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Search", description = "Product search and filtering APIs")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    /**
     * Main product search endpoint with parameter filtering support
     *
     * Example request:
     * POST /api/v1/products/search
     * {
     *   "query": "laptop",
     *   "language": "bg",
     *   "filters": {
     *     "RAM памет": ["8GB", "16GB"],
     *     "Цвят": ["Черен"]
     *   },
     *   "categories": ["Laptops"],
     *   "minPrice": 1000,
     *   "maxPrice": 3000,
     *   "sortBy": "price_asc",
     *   "page": 0,
     *   "size": 20
     * }
     */
    @PostMapping("/search")
    @Operation(
            summary = "Search products with filters",
            description = "Advanced product search with full-text search, parameter filtering, price range, categories, etc."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(schema = @Schema(implementation = ProductSearchResponse.class))
    )
    public ResponseEntity<ProductSearchResponse> searchProducts(
            @Valid @RequestBody ProductSearchRequest request) {

        log.info("Product search request: query='{}', filters={}, page={}",
                request.getQuery(), request.getFilters(), request.getPage());

        ProductSearchResponse response = productSearchService.searchProducts(request);

        log.info("Search completed: {} results in {}ms",
                response.getTotalElements(), response.getSearchTime());

        return ResponseEntity.ok(response);
    }

    /**
     * Get search autocomplete suggestions
     */
    @GetMapping("/suggestions")
    @Operation(
            summary = "Get search suggestions",
            description = "Returns autocomplete suggestions based on partial query"
    )
    public ResponseEntity<List<String>> getSearchSuggestions(
            @Parameter(description = "Search query (minimum 2 characters)")
            @RequestParam String query,

            @Parameter(description = "Language code (bg or en)")
            @RequestParam(defaultValue = "bg") String language,

            @Parameter(description = "Maximum number of suggestions")
            @RequestParam(defaultValue = "10") int maxSuggestions) {

        List<String> suggestions = productSearchService
                .getSearchSuggestions(query, language, maxSuggestions);

        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get available parameters and options for a specific category
     * Useful for building dynamic filter UI
     */
    @GetMapping("/categories/{categoryId}/parameters")
    @Operation(
            summary = "Get available parameters for category",
            description = "Returns all parameters and their options for a specific category. Used to build filter UI."
    )
    public ResponseEntity<Map<String, List<String>>> getAvailableParameters(
            @Parameter(description = "Category ID")
            @PathVariable Long categoryId,

            @Parameter(description = "Language code (bg or en)")
            @RequestParam(defaultValue = "bg") String language) {

        Map<String, List<String>> parameters = productSearchService
                .getAvailableParametersForCategory(categoryId, language);

        return ResponseEntity.ok(parameters);
    }

    /**
     * Get available parameters with product counts for a specific category
     * More detailed version that includes how many products have each option
     */
    @GetMapping("/categories/{categoryId}/parameters/with-counts")
    @Operation(
            summary = "Get available parameters with counts for category",
            description = "Returns all parameters with product counts for each option. Used to build filter UI with counts."
    )
    public ResponseEntity<Map<String, List<FacetValue>>> getAvailableParametersWithCounts(
            @Parameter(description = "Category ID")
            @PathVariable Long categoryId,

            @Parameter(description = "Language code (bg or en)")
            @RequestParam(defaultValue = "bg") String language) {

        Map<String, List<FacetValue>> parameters = productSearchService
                .getAvailableParametersWithCountsForCategory(categoryId, language);

        return ResponseEntity.ok(parameters);
    }

    /**
     * Quick search - simplified endpoint without POST body
     */
    @GetMapping("/quick-search")
    @Operation(
            summary = "Quick search",
            description = "Simplified search with GET parameters"
    )
    public ResponseEntity<ProductSearchResponse> quickSearch(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "bg") String language,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchRequest request = ProductSearchRequest.builder()
                .query(query)
                .language(language)
                .categories(category != null ? List.of(category) : null)
                .manufacturers(manufacturer != null ? List.of(manufacturer) : null)
                .page(page)
                .size(size)
                .build();

        ProductSearchResponse response = productSearchService.searchProducts(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Search by category with filters
     */
    @PostMapping("/categories/{categoryId}/search")
    @Operation(
            summary = "Search within category",
            description = "Search products within a specific category with filters"
    )
    public ResponseEntity<ProductSearchResponse> searchInCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody ProductSearchRequest request) {

        // Add category filter if not already present
        if (request.getCategories() == null || request.getCategories().isEmpty()) {
            // You would need to fetch category name by ID
            // For now, this is a simplified example
        }

        ProductSearchResponse response = productSearchService.searchProducts(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get featured products
     */
    @GetMapping("/featured")
    @Operation(
            summary = "Get featured products",
            description = "Returns list of featured products"
    )
    public ResponseEntity<ProductSearchResponse> getFeaturedProducts(
            @RequestParam(defaultValue = "bg") String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchRequest request = ProductSearchRequest.builder()
                .language(language)
                .featured(true)
                .page(page)
                .size(size)
                .sortBy("featured")
                .build();

        ProductSearchResponse response = productSearchService.searchProducts(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get products on sale
     */
    @GetMapping("/on-sale")
    @Operation(
            summary = "Get products on sale",
            description = "Returns list of products with active discounts"
    )
    public ResponseEntity<ProductSearchResponse> getProductsOnSale(
            @RequestParam(defaultValue = "bg") String language,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchRequest request = ProductSearchRequest.builder()
                .language(language)
                .categories(category != null ? List.of(category) : null)
                .onSale(true)
                .page(page)
                .size(size)
                .sortBy("price_desc")
                .build();

        ProductSearchResponse response = productSearchService.searchProducts(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(IllegalArgumentException e) {
        log.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }

    /**
     * Exception handler for search errors
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleSearchException(RuntimeException e) {
        log.error("Search error", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("SEARCH_ERROR", "Search failed. Please try again."));
    }

    /**
     * Error response DTO
     */
    record ErrorResponse(String code, String message) {}
}