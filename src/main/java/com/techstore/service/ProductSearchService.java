package com.techstore.service;

import com.techstore.dto.request.ProductSearchRequest;
import com.techstore.dto.response.FacetValue;
import com.techstore.dto.response.ProductSearchResponse;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductSearchService {

    private final ProductSearchRepository searchRepository;
    private final CategoryRepository categoryRepository;

    public ProductSearchResponse searchProducts(ProductSearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Searching products with query: '{}', language: {}, filters: {}",
                    request.getQuery(), request.getLanguage(), request.getFilters());

            // Validate and sanitize input
            validateSearchRequest(request);

            // Resolve alias category IDs before querying
            if (request.getCategories() != null && !request.getCategories().isEmpty()) {
                request.setCategories(resolveAliasCategories(request.getCategories()));
            }

            // Основно търсене — FTS + ILIKE (бързо, ползва индекси)
            ProductSearchResponse response = searchRepository.searchProducts(request);

            // Fuzzy fallback — само ако няма резултати и заявката е >= 4 символа
            if (response.getTotalElements() == 0
                    && StringUtils.hasText(request.getQuery())
                    && request.getQuery().length() >= 4) {
                log.debug("No results for '{}', attempting fuzzy search", request.getQuery());
                response = searchRepository.searchProductsFuzzy(request);
            }

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
        // @Builder.Default values are NOT applied when Jackson deserializes via @NoArgsConstructor.
        // Guard against null/zero defaults that would cause NPE or LIMIT 0 in the SQL.
        if (request.getLanguage() == null) request.setLanguage("bg");
        if (request.getSortBy() == null) request.setSortBy("price_asc");
        if (request.getSize() <= 0) request.setSize(20);
        if (request.getPage() < 0) request.setPage(0);

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
        // Note: apostrophes and quotes are safe to keep since queries are fully parameterized
        String sanitized = query.replaceAll("[;\\\\]", " ");

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

            Long resolvedId = resolveAliasId(categoryId);
            Map<String, List<FacetValue>> parameters =
                    searchRepository.getAvailableParametersWithCountsForCategory(resolvedId, language);

            log.debug("Found {} parameters with counts for category {}", parameters.size(), categoryId);
            return parameters;

        } catch (Exception e) {
            log.error("Failed to get parameters with counts for category: {}", categoryId, e);
            return Collections.emptyMap();
        }
    }

    public Map<String, List<FacetValue>> getFilteredFacets(
            Long categoryId, ProductSearchRequest request, String language) {
        try {
            if (request.getLanguage() == null) {
                request.setLanguage(language);
            }
            Long resolvedId = resolveAliasId(categoryId);
            return searchRepository.getFilteredFacets(resolvedId, request, language);
        } catch (Exception e) {
            log.error("Failed to get filtered facets for category: {}", categoryId, e);
            return Collections.emptyMap();
        }
    }

    public ProductSearchResponse searchByCategory(String categoryId, String language, int page, int size, String sortBy) {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categories(List.of(categoryId))
                .language(language)
                .page(page)
                .size(size)
                .sortBy(sortBy != null ? sortBy : "price_asc")
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

    public ProductSearchResponse searchProductsOnSale(String language, int page, int size, String sortBy) {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .onSale(true)
                .language(language)
                .page(page)
                .size(size)
                .sortBy(sortBy != null ? sortBy : "price_asc")
                .build();

        return searchProducts(request);
    }

    /** If the category is an alias, returns the target category's ID; otherwise returns the same ID. */
    private Long resolveAliasId(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .map(c -> c.getAliasOf() != null ? c.getAliasOf().getId() : c.getId())
                .orElse(categoryId);
    }

    private List<String> resolveAliasCategories(List<String> categories) {
        return categories.stream()
                .map(id -> {
                    try {
                        Long resolved = resolveAliasId(Long.parseLong(id));
                        return resolved != null ? String.valueOf(resolved) : id;
                    } catch (NumberFormatException e) {
                        return id;
                    }
                })
                .toList();
    }
}