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

    private void validateSearchRequest(ProductSearchRequest request) {
        if (request.getSize() > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
        if (request.getPage() < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (request.getQuery() != null && request.getQuery().length() > 200) {
            throw new IllegalArgumentException("Search query too long");
        }
        if (request.getMinPrice() != null && request.getMaxPrice() != null &&
                request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }

        if (StringUtils.hasText(request.getQuery())) {
            request.setQuery(sanitizeQuery(request.getQuery()));
        }
    }

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