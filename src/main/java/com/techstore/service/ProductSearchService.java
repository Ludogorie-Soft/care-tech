package com.techstore.service;

import com.techstore.dto.request.ProductSearchRequest;
import com.techstore.dto.response.FacetValue;
import com.techstore.dto.response.ProductSearchResponse;
import com.techstore.dto.response.ProductSearchResult;
import com.techstore.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductSearchService {

    private final ProductSearchRepository searchRepository;

    /**
     * Main search method with parameter filtering support (ID-based)
     */
    public ProductSearchResponse searchProducts(ProductSearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Searching products with query: '{}', language: {}, filters: {}",
                    request.getQuery(), request.getLanguage(), request.getFilters());

            // Validate and sanitize input
            validateSearchRequest(request);

            // Perform search with parameter filtering
            ProductSearchResponse response = searchRepository.searchProducts(request);

            // Set actual search time
            long searchTime = System.currentTimeMillis() - startTime;
            response.setSearchTime(searchTime);

            log.debug("Search completed in {}ms, found {} products, {} facets",
                    searchTime, response.getTotalElements(),
                    response.getFacets() != null ? response.getFacets().size() : 0);

            return response;

        } catch (Exception e) {
            log.error("Search failed for query: '{}', filters: {}",
                    request.getQuery(), request.getFilters(), e);
            throw new RuntimeException("Search failed", e);
        }
    }

    /**
     * Get autocomplete suggestions
     */
    public List<String> getSearchSuggestions(String query, String language, int maxSuggestions) {
        if (!StringUtils.hasText(query) || query.length() < 2) {
            return Collections.emptyList();
        }

        try {
            String sanitizedQuery = sanitizeQuery(query);
            if (sanitizedQuery.length() < 2) {
                return Collections.emptyList();
            }

            return searchRepository.getSearchSuggestions(sanitizedQuery, language, maxSuggestions);

        } catch (Exception e) {
            log.error("Failed to get search suggestions for query: '{}'", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get available parameters and options for a category
     * This is used to build the filter UI dynamically
     *
     * @param categoryId Category ID
     * @param language Language code (bg or en)
     * @return Map with keys "paramId:paramName" and values "optionId:optionName"
     */
    @Cacheable(value = "categoryParameters", key = "#categoryId + '_' + #language")
    public Map<String, List<String>> getAvailableParametersForCategory(Long categoryId, String language) {
        try {
            log.debug("Fetching parameters for category: {}, language: {}", categoryId, language);

            Map<String, List<String>> parameters =
                    searchRepository.getAvailableParametersForCategory(categoryId, language);

            log.debug("Found {} parameters for category {}", parameters.size(), categoryId);
            return parameters;

        } catch (Exception e) {
            log.error("Failed to get parameters for category: {}", categoryId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get available parameters with product counts for a category
     * More detailed version that includes counts for each option
     *
     * @param categoryId Category ID
     * @param language Language code (bg or en)
     * @return Map with keys "paramId:paramName" and values List<FacetValue> (with IDs and counts)
     */
    @Cacheable(value = "categoryParametersWithCounts", key = "#categoryId + '_' + #language")
    public Map<String, List<FacetValue>> getAvailableParametersWithCountsForCategory(
            Long categoryId, String language) {
        try {
            log.debug("Fetching parameters with counts for category: {}, language: {}", categoryId, language);

            Map<String, List<FacetValue>> parameters =
                    searchRepository.getAvailableParametersWithCountsForCategory(categoryId, language);

            log.debug("Found {} parameters with counts for category {}", parameters.size(), categoryId);
            return parameters;

        } catch (Exception e) {
            log.error("Failed to get parameters with counts for category: {}", categoryId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Validate and sanitize search request
     * SIMPLIFIED: ID-based filters need much less validation!
     */
    private void validateSearchRequest(ProductSearchRequest request) {
        // Validate page size
        if (request.getSize() > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
        if (request.getSize() < 1) {
            throw new IllegalArgumentException("Page size must be at least 1");
        }

        // Validate page number
        if (request.getPage() < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        // Validate query length
        if (request.getQuery() != null && request.getQuery().length() > 200) {
            throw new IllegalArgumentException("Search query too long (max 200 characters)");
        }

        // Validate price range
        if (request.getMinPrice() != null && request.getMaxPrice() != null &&
                request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }

        // ============================================================
        // SIMPLIFIED FILTER VALIDATION - ID-BASED
        // Much simpler than text-based validation!
        // ============================================================
        if (request.getFilters() != null) {
            for (Map.Entry<Long, List<Long>> filter : request.getFilters().entrySet()) {
                Long parameterId = filter.getKey();
                List<Long> optionIds = filter.getValue();

                // Basic validation
                if (parameterId == null || parameterId <= 0) {
                    throw new IllegalArgumentException("Invalid parameter ID: " + parameterId);
                }

                if (optionIds != null && optionIds.size() > 50) {
                    throw new IllegalArgumentException(
                            "Too many filter options for parameter " + parameterId + " (max 50)");
                }

                // Validate each option ID
                if (optionIds != null) {
                    for (Long optionId : optionIds) {
                        if (optionId == null || optionId <= 0) {
                            throw new IllegalArgumentException(
                                    "Invalid option ID: " + optionId + " for parameter " + parameterId);
                        }
                    }
                }
            }
        }

        // Sanitize query (only the query needs sanitization now!)
        if (StringUtils.hasText(request.getQuery())) {
            request.setQuery(sanitizeQuery(request.getQuery()));
        }
    }

    /**
     * Sanitize query string to prevent SQL injection and other issues
     */
    private String sanitizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }

        // Remove potentially dangerous characters for SQL
        String sanitized = query.replaceAll("[';\"\\\\]", " ");

        // Remove extra whitespace
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        // Limit length
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }

        return sanitized;
    }

    /**
     * Helper method to search products by category
     */
    public ProductSearchResponse searchByCategory(String categoryName, String language, int page, int size) {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categories(List.of(categoryName))
                .language(language)
                .page(page)
                .size(size)
                .sortBy("relevance")
                .build();

        return searchProducts(request);
    }

    /**
     * Helper method to search featured products
     */
    public ProductSearchResponse searchFeaturedProducts(String language, int page, int size) {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .featured(true)
                .language(language)
                .page(page)
                .size(size)
                .sortBy("featured")
                .build();

        return searchProducts(request);
    }

    /**
     * Helper method to search products on sale
     */
    public ProductSearchResponse searchProductsOnSale(String language, int page, int size) {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .onSale(true)
                .language(language)
                .page(page)
                .size(size)
                .sortBy("price_desc")
                .build();

        return searchProducts(request);
    }
}