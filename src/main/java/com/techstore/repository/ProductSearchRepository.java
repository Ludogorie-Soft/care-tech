package com.techstore.repository;

import com.techstore.dto.request.ProductSearchRequest;
import com.techstore.dto.response.FacetValue;
import com.techstore.dto.response.ProductSearchResponse;
import com.techstore.dto.response.ProductSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductSearchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public ProductSearchResponse searchProducts(ProductSearchRequest request) {
        String language = "simple";
        String nameField = request.getLanguage().equals("en") ? "name_en" : "name_bg";
        String descriptionField = request.getLanguage().equals("en") ? "description_en" : "description_bg";

        StringBuilder sql = new StringBuilder();
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        sql.append("SELECT p.*, m.name as manufacturer_name, c.")
                .append(nameField)
                .append(" as category_name ");

        if (StringUtils.hasText(request.getQuery())) {
            sql.append(", ts_rank(")
                    .append("to_tsvector('").append(language).append("', ")
                    .append("coalesce(p.").append(nameField).append(", '') || ' ' || ")
                    .append("coalesce(p.").append(descriptionField).append(", '') || ' ' || ")
                    .append("coalesce(p.model, '') || ' ' || ")
                    .append("coalesce(p.reference_number, '')), ")
                    .append("plainto_tsquery('").append(language).append("', :query)) as search_rank ");
            params.put("query", request.getQuery());
        } else {
            sql.append(", 1.0 as search_rank ");
        }

        sql.append("FROM products p ")
                .append("LEFT JOIN manufacturers m ON p.manufacturer_id = m.id ")
                .append("LEFT JOIN categories c ON p.category_id = c.id ");

        countSql.append("SELECT COUNT(*) FROM products p ")
                .append("LEFT JOIN manufacturers m ON p.manufacturer_id = m.id ")
                .append("LEFT JOIN categories c ON p.category_id = c.id ");

        StringBuilder whereClause = new StringBuilder("WHERE p.active = true AND p.show_flag = true ");

        if (StringUtils.hasText(request.getQuery())) {
            whereClause.append("AND (")
                    .append("to_tsvector('").append(language).append("', ")
                    .append("coalesce(p.").append(nameField).append(", '') || ' ' || ")
                    .append("coalesce(p.").append(descriptionField).append(", '') || ' ' || ")
                    .append("coalesce(p.model, '') || ' ' || ")
                    .append("coalesce(p.reference_number, '')) ")
                    .append("@@ plainto_tsquery('").append(language).append("', :query) OR ")

                    .append("LOWER(p.model) LIKE LOWER(:likeQuery) OR ")
                    .append("LOWER(p.reference_number) LIKE LOWER(:likeQuery) OR ")
                    .append("p.barcode = :exactQuery OR ")
                    .append("LOWER(m.name) LIKE LOWER(:likeQuery)) ");

            params.put("likeQuery", "%" + request.getQuery() + "%");
            params.put("exactQuery", request.getQuery());
        }

        if (request.getMinPrice() != null) {
            whereClause.append("AND p.final_price >= :minPrice ");
            params.put("minPrice", request.getMinPrice());
        }
        if (request.getMaxPrice() != null) {
            whereClause.append("AND p.final_price <= :maxPrice ");
            params.put("maxPrice", request.getMaxPrice());
        }

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            whereClause.append("AND p.category_id IN (:categoryIds) ");
            List<Long> categoryIds = request.getCategories().stream()
                    .map(Long::valueOf)
                    .toList();
            params.put("categoryIds", categoryIds);
        }

        if (request.getManufacturers() != null && !request.getManufacturers().isEmpty()) {
            whereClause.append("AND p.manufacturer_id IN (:manufacturerIds) ");
            List<Long> manufacturerIds = request.getManufacturers().stream()
                    .map(Long::valueOf)
                    .toList();
            params.put("manufacturerIds", manufacturerIds);
        }

        if (request.getFeatured() != null) {
            whereClause.append("AND p.featured = :featured ");
            params.put("featured", request.getFeatured());
        }

        if (request.getOnSale() != null && request.getOnSale()) {
            whereClause.append("AND p.discount > 0 ");
        }

        // ============================================================
        // IMPROVED PARAMETER FILTERING - BY ID (MUCH FASTER!)
        // ============================================================
        // Benefits:
        // 1. No language dependency (no name_en vs name_bg)
        // 2. Much faster (indexed ID comparison vs text LOWER())
        // 3. Stable (IDs don't change, names can be edited)
        // 4. No encoding issues (16GB vs 16 GB vs 16gb)
        // 5. Cleaner API
        //
        // Logic:
        // - AND between different parameters (parameterId1 AND parameterId2)
        // - OR within same parameter (optionId1 OR optionId2)
        // ============================================================
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            int filterIndex = 0;
            for (Map.Entry<Long, List<Long>> filter : request.getFilters().entrySet()) {
                Long parameterId = filter.getKey();
                List<Long> optionIds = filter.getValue();

                // Skip empty filters
                if (optionIds == null || optionIds.isEmpty()) {
                    continue;
                }

                // EXISTS subquery: product must have at least one of the specified options
                // for this parameter
                whereClause.append("AND EXISTS (")
                        .append("SELECT 1 FROM product_parameters pp ")
                        .append("WHERE pp.product_id = p.id ")
                        .append("AND pp.parameter_id = :parameterId").append(filterIndex).append(" ")
                        .append("AND pp.parameter_option_id IN (:optionIds").append(filterIndex).append(")) ");

                // Add parameters - simple ID comparison, no LOWER() needed!
                params.put("parameterId" + filterIndex, parameterId);
                params.put("optionIds" + filterIndex, optionIds);

                filterIndex++;
            }
        }

        sql.append(whereClause);
        countSql.append(whereClause);

        sql.append("ORDER BY ");
        switch (request.getSortBy().toLowerCase()) {
            case "price_asc":
                sql.append("p.final_price ASC");
                break;
            case "price_desc":
                sql.append("p.final_price DESC");
                break;
            case "name":
                sql.append("p.").append(nameField).append(" ASC");
                break;
            case "newest":
                sql.append("p.created_at DESC");
                break;
            case "featured":
                sql.append("p.featured DESC, search_rank DESC");
                break;
            default:
                sql.append("search_rank DESC, p.final_price ASC");
        }

        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", request.getSize());
        params.put("offset", request.getPage() * request.getSize());

        try {
            List<ProductSearchResult> products = namedJdbcTemplate.query(
                    sql.toString(), params, (rs, rowNum) -> mapRowToProduct(rs, rowNum, request.getLanguage()));

            Long totalCount = namedJdbcTemplate.queryForObject(
                    countSql.toString(), params, Long.class);

            long totalElements = totalCount != null ? totalCount : 0;
            int totalPages = (int) Math.ceil((double) totalElements / request.getSize());

            log.info("Search executed successfully. Query: '{}', Filters: {}, Results: {}, Time: {}ms",
                    request.getQuery(), request.getFilters(), totalElements, 0);

            return ProductSearchResponse.builder()
                    .products(products)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .currentPage(request.getPage())
                    .facets(buildFacets(request, params, whereClause.toString()))
                    .searchTime(0L)
                    .build();

        } catch (Exception e) {
            log.error("PostgreSQL search failed for query: '{}'. Error: {}", request.getQuery(), e.getMessage(), e);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build facets (aggregations) for filtering UI
     * Returns available filter options based on current search results
     *
     * IMPROVED: Now returns parameter and option IDs along with names
     */
    private Map<String, List<FacetValue>> buildFacets(
            ProductSearchRequest request,
            Map<String, Object> params,
            String whereClause) {

        Map<String, List<FacetValue>> facets = new HashMap<>();

        try {
            String language = request.getLanguage();
            String paramNameField = language.equals("en") ? "param.name_en" : "param.name_bg";
            String optionNameField = language.equals("en") ? "po.name_en" : "po.name_bg";

            // Get parameter facets with IDs from current search results
            String facetSql = "SELECT " +
                    "param.id as param_id, " +
                    paramNameField + " as param_name, " +
                    "po.id as option_id, " +
                    optionNameField + " as option_name, " +
                    "COUNT(DISTINCT p.id) as count " +
                    "FROM products p " +
                    "LEFT JOIN manufacturers m ON p.manufacturer_id = m.id " +
                    "LEFT JOIN categories c ON p.category_id = c.id " +
                    "JOIN product_parameters pp ON pp.product_id = p.id " +
                    "JOIN parameters param ON pp.parameter_id = param.id " +
                    "JOIN parameter_options po ON pp.parameter_option_id = po.id " +
                    whereClause +
                    "GROUP BY param.id, param_name, po.id, option_name " +
                    "ORDER BY param_name, count DESC";

            namedJdbcTemplate.query(facetSql, params, rs -> {
                Long paramId = rs.getLong("param_id");
                String paramName = rs.getString("param_name");
                Long optionId = rs.getLong("option_id");
                String optionName = rs.getString("option_name");
                Long count = rs.getLong("count");

                // Check if this option is currently selected
                boolean isSelected = request.getFilters() != null &&
                        request.getFilters().containsKey(paramId) &&
                        request.getFilters().get(paramId) != null &&
                        request.getFilters().get(paramId).contains(optionId);

                FacetValue facetValue = FacetValue.builder()
                        .id(optionId)  // NOW WE HAVE THE ID!
                        .value(optionName)
                        .count(count)
                        .selected(isSelected)
                        .build();

                // Use "param_id:param_name" as key to keep both ID and name
                String facetKey = paramId + ":" + paramName;
                facets.computeIfAbsent(facetKey, k -> new ArrayList<>())
                        .add(facetValue);
            });

        } catch (Exception e) {
            log.warn("Failed to build facets: {}", e.getMessage());
        }

        return facets;
    }

    private ProductSearchResult mapRowToProduct(ResultSet rs, int rowNum, String language) throws SQLException {
        return ProductSearchResult.builder()
                .id(rs.getLong("id"))
                .name(rs.getString(language.equals("en") ? "name_en" : "name_bg"))
                .description(rs.getString(language.equals("en") ? "description_en" : "description_bg"))
                .model(rs.getString("model"))
                .referenceNumber(rs.getString("reference_number"))
                .finalPrice(rs.getBigDecimal("final_price"))
                .discount(rs.getBigDecimal("discount"))
                .primaryImageUrl("/api/images/product/" + rs.getLong("id") + "/primary")
                .manufacturerName(rs.getString("manufacturer_name"))
                .categoryName(rs.getString("category_name"))
                .featured(rs.getBoolean("featured"))
                .onSale(rs.getBigDecimal("discount") != null && rs.getBigDecimal("discount").compareTo(BigDecimal.ZERO) > 0)
                .score(rs.getFloat("search_rank"))
                .build();
    }

    public List<String> getSearchSuggestions(String query, String language, int maxSuggestions) {
        if (!StringUtils.hasText(query) || query.length() < 2) {
            return Collections.emptyList();
        }

        try {
            String nameField = language.equals("en") ? "name_en" : "name_bg";

            String sql = "SELECT DISTINCT " + nameField + ", " +
                    "similarity(" + nameField + ", :query) as sim " +
                    "FROM products " +
                    "WHERE " + nameField + " % :query " +
                    "AND active = true AND show_flag = true " +
                    "AND " + nameField + " IS NOT NULL " +
                    "ORDER BY sim DESC, " + nameField + " " +
                    "LIMIT :limit";

            Map<String, Object> params = new HashMap<>();
            params.put("query", query);
            params.put("limit", maxSuggestions);

            return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString(nameField))
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.warn("Trigram suggestions failed, falling back to LIKE: {}", e.getMessage());

            try {
                String nameField = language.equals("en") ? "name_en" : "name_bg";
                String sql = "SELECT DISTINCT " + nameField + " " +
                        "FROM products " +
                        "WHERE LOWER(" + nameField + ") LIKE LOWER(:query) " +
                        "AND active = true AND show_flag = true " +
                        "AND " + nameField + " IS NOT NULL " +
                        "ORDER BY " + nameField + " " +
                        "LIMIT :limit";

                Map<String, Object> params = new HashMap<>();
                params.put("query", query + "%");
                params.put("limit", maxSuggestions);

                return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString(nameField))
                        .stream()
                        .filter(Objects::nonNull)
                        .toList();

            } catch (Exception fallbackError) {
                log.error("Both trigram and LIKE suggestions failed: {}", fallbackError.getMessage());
                return Collections.emptyList();
            }
        }
    }

    /**
     * Get available parameters and their options for a category
     * Useful for building filter UI dynamically
     *
     * IMPROVED: Now returns IDs along with names
     */
    public Map<String, List<String>> getAvailableParametersForCategory(Long categoryId, String language) {
        Map<String, List<String>> parameters = new HashMap<>();

        try {
            String paramNameField = language.equals("en") ? "param.name_en" : "param.name_bg";
            String optionNameField = language.equals("en") ? "po.name_en" : "po.name_bg";

            String sql = "SELECT DISTINCT " +
                    "param.id as param_id, " +
                    paramNameField + " as param_name, " +
                    "po.id as option_id, " +
                    optionNameField + " as option_name " +
                    "FROM parameters param " +
                    "JOIN parameter_options po ON po.parameter_id = param.id " +
                    "WHERE param.category_id = :categoryId " +
                    "ORDER BY param.sort_order, param_name, po.sort_order, option_name";

            Map<String, Object> params = new HashMap<>();
            params.put("categoryId", categoryId);

            namedJdbcTemplate.query(sql, params, rs -> {
                Long paramId = rs.getLong("param_id");
                String paramName = rs.getString("param_name");
                Long optionId = rs.getLong("option_id");
                String optionName = rs.getString("option_name");

                // Use "param_id:param_name" as key
                String key = paramId + ":" + paramName;
                // Store "option_id:option_name" as value
                String value = optionId + ":" + optionName;

                parameters.computeIfAbsent(key, k -> new java.util.ArrayList<>())
                        .add(value);
            });

        } catch (Exception e) {
            log.error("Failed to get parameters for category {}: {}", categoryId, e.getMessage());
        }

        return parameters;
    }

    /**
     * Get available parameters with counts for a category (includes product counts)
     * More detailed version that returns FacetValue objects with IDs
     */
    public Map<String, List<FacetValue>> getAvailableParametersWithCountsForCategory(
            Long categoryId, String language) {

        Map<String, List<FacetValue>> parameters = new HashMap<>();

        try {
            String paramNameField = language.equals("en") ? "param.name_en" : "param.name_bg";
            String optionNameField = language.equals("en") ? "po.name_en" : "po.name_bg";

            String sql = "SELECT " +
                    "param.id as param_id, " +
                    paramNameField + " as param_name, " +
                    "po.id as option_id, " +
                    optionNameField + " as option_name, " +
                    "COUNT(DISTINCT pp.product_id) as product_count " +
                    "FROM parameters param " +
                    "JOIN parameter_options po ON po.parameter_id = param.id " +
                    "LEFT JOIN product_parameters pp ON pp.parameter_option_id = po.id " +
                    "LEFT JOIN products p ON pp.product_id = p.id AND p.active = true AND p.show_flag = true " +
                    "WHERE param.category_id = :categoryId " +
                    "GROUP BY param.id, param_name, po.id, option_name, param.sort_order, po.sort_order " +
                    "ORDER BY param.sort_order, param_name, po.sort_order, option_name";

            Map<String, Object> params = new HashMap<>();
            params.put("categoryId", categoryId);

            namedJdbcTemplate.query(sql, params, rs -> {
                Long paramId = rs.getLong("param_id");
                String paramName = rs.getString("param_name");
                Long optionId = rs.getLong("option_id");
                String optionName = rs.getString("option_name");
                Long productCount = rs.getLong("product_count");

                FacetValue facetValue = FacetValue.builder()
                        .id(optionId)  // ID IS HERE!
                        .value(optionName)
                        .count(productCount)
                        .selected(false)
                        .build();

                // Use "param_id:param_name" as key
                String key = paramId + ":" + paramName;
                parameters.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(facetValue);
            });

        } catch (Exception e) {
            log.error("Failed to get parameters with counts for category {}: {}", categoryId, e.getMessage());
        }

        return parameters;
    }
}