package com.techstore.service;

import com.techstore.dto.request.ProductSearchRequest;
import com.techstore.dto.response.DisplaySpecificationDto;
import com.techstore.dto.response.ProductSearchResponse;
import com.techstore.dto.response.ProductSearchResult;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductSearchRepository;
import com.techstore.service.filter.DisplaySpecificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductSearchService {

    private final ProductSearchRepository searchRepository;
    private final CategoryRepository categoryRepository;
    private final DisplaySpecificationService displaySpecificationService;
    private final CategoryAliasResolver categoryAliasResolver;

    public ProductSearchResponse searchProducts(ProductSearchRequest request) {
        long startTime = System.currentTimeMillis();

        log.debug("Searching products with query: '{}', language: {}, attributeFilters: {}",
                request.getQuery(), request.getLanguage(), request.getAttributeFilters());

        // Validation runs outside the catch below on purpose. Wrapping it meant a bad
        // page size or a backwards price range came back as a 500 "Search failed" instead
        // of a 400, hiding a client mistake behind a server error.
        validateSearchRequest(request);

        try {
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

            addDisplaySpecifications(response, request.getLanguage());

            // Set actual search time
            long searchTime = System.currentTimeMillis() - startTime;
            response.setSearchTime(searchTime);

            log.debug("Search completed in {}ms, found {} products", searchTime, response.getTotalElements());

            return response;

        } catch (Exception e) {
            log.error("Search failed for query: '{}', attributeFilters: {}",
                    request.getQuery(), request.getAttributeFilters(), e);
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

    public ProductSearchResponse searchByCategory(String categoryId, String language, int page, int size, String sortBy) {
        List<String> allIds = getAllDescendantIds(categoryId);

        ProductSearchRequest request = ProductSearchRequest.builder()
                .categories(allIds)
                .language(language)
                .page(page)
                .size(size)
                .sortBy(sortBy != null ? sortBy : "price_asc")
                .build();

        return searchProducts(request);
    }

    /** Returns the given category ID plus all descendant IDs (recursive). */
    private List<String> getAllDescendantIds(String categoryId) {
        List<String> result = new java.util.ArrayList<>();
        result.add(categoryId);
        try {
            Long id = Long.parseLong(categoryId);
            collectDescendants(id, result);
        } catch (NumberFormatException ignored) {
        }
        return result;
    }

    private void collectDescendants(Long parentId, List<String> accumulator) {
        categoryRepository.findByParentId(parentId).forEach(child -> {
            accumulator.add(String.valueOf(child.getId()));
            collectDescendants(child.getId(), accumulator);
        });
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

    private void addDisplaySpecifications(ProductSearchResponse response, String language) {
        if (response.getProducts() == null || response.getProducts().isEmpty()) {
            return;
        }
        Map<Long, List<DisplaySpecificationDto>> specs = displaySpecificationService.forProducts(
                response.getProducts().stream().map(ProductSearchResult::getId).toList(), language);
        response.getProducts().forEach(p -> p.setDisplaySpecifications(specs.getOrDefault(p.getId(), List.of())));
    }

    private Long resolveAliasId(Long categoryId) {
        return categoryAliasResolver.resolve(categoryId);
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