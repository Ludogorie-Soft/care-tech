package com.techstore.repository;

import com.techstore.dto.request.ProductSearchRequest;
import com.techstore.dto.response.FacetValue;
import com.techstore.dto.response.ProductSearchResponse;
import com.techstore.dto.response.ProductSearchResult;
import com.techstore.dto.response.ProductParameterResponseDto;
import com.techstore.dto.response.ParameterOptionResponseDto;
import com.techstore.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductSearchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    /** Guards against a pathological query turning into dozens of ANDed ILIKEs. */
    private static final int MAX_QUERY_WORDS = 6;

    /** The text FTS indexes — matches the expression in V5__update_fts_combined_index.sql. */
    private static final String FTS_VECTOR =
            "to_tsvector('simple', coalesce(p.name_bg, '') || ' ' || coalesce(p.name_en, '') || ' ' || " +
            "coalesce(p.description_bg, '') || ' ' || coalesce(p.description_en, '') || ' ' || " +
            "coalesce(p.model, '') || ' ' || coalesce(p.reference_number, ''))";

    /**
     * Everything a word may match against. Category and manufacturer are included on
     * purpose: "лаптоп lenovo" found one product without them and 50 with, because the
     * word "лаптоп" lives in the category name, not in "Lenovo IdeaPad Slim 3".
     */
    private static final String SEARCH_BLOB =
            "(coalesce(p.name_bg, '') || ' ' || coalesce(p.name_en, '') || ' ' || coalesce(p.model, '') || ' ' || " +
            "coalesce(p.reference_number, '') || ' ' || coalesce(p.sku, '') || ' ' || " +
            "coalesce(m.name, '') || ' ' || coalesce(c.name_bg, '') || ' ' || coalesce(c.name_en, ''))";

    /** Name with spaces stripped, so "rtx 5080" can match a product named "RTX5080". */
    private static final String NAME_NO_SPACES =
            "replace(lower(coalesce(p.name_bg, '') || ' ' || coalesce(p.name_en, '') || ' ' || coalesce(p.model, '')), ' ', '')";

    private static final String CATEGORY_NAME =
            "lower(coalesce(c.name_bg, '') || ' ' || coalesce(c.name_en, ''))";

    public ProductSearchResponse searchProducts(ProductSearchRequest request) {
        String language = "simple";
        String nameField = request.getLanguage().equals("en") ? "name_en" : "name_bg";
        String descriptionField = request.getLanguage().equals("en") ? "description_en" : "description_bg";

        StringBuilder sql = new StringBuilder();
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        sql.append("SELECT p.id, p.name_bg, p.name_en, p.description_bg, p.description_en, ")
                .append("p.model, p.reference_number, p.final_price, p.discount, p.featured, p.status, ")
                .append("p.slug, p.platform, ")
                .append("m.name as manufacturer_name, c.")
                .append(nameField)
                .append(" as category_name ")
                .append(", p.image_url AS primary_image_url ");


        if (StringUtils.hasText(request.getQuery())) {
            // Relevance. ts_rank alone was not enough: it is 0 whenever a row matched
            // through ILIKE rather than full text, which left whole result sets tied at
            // zero and falling back to price — "лаптопи" opened with a CMOS battery and
            // three cabinets, "iphone 15" with DVR recorders.
            sql.append(", (")
                    .append("ts_rank(").append(FTS_VECTOR)
                    .append(", plainto_tsquery('").append(language).append("', :query))")
                    // Name match is the strongest signal, prefix stronger than substring.
                    .append(" + CASE WHEN ").append(NAME_NO_SPACES).append(" LIKE :qPrefix ESCAPE '\\' THEN 3.0")
                    .append(" WHEN ").append(NAME_NO_SPACES).append(" LIKE :qNameLike ESCAPE '\\' THEN 1.0 ELSE 0 END")
                    // A category whose own name matches is what the user is after when they
                    // type "лаптопи" or "камери". Accessory categories in this catalogue are
                    // consistently phrased "X за Y" ("Чанти за лаптопи", "Стойки и основи за
                    // камери") while the real ones are not ("Лаптопи", "IP камери"), so that
                    // "за" is what separates a laptop from a laptop bag. It is a heuristic
                    // over this data, not a rule — revisit it if category naming changes.
                    .append(" + CASE WHEN ").append(CATEGORY_NAME).append(" LIKE :qLike ESCAPE '\\'")
                    .append(" AND ").append(CATEGORY_NAME).append(" NOT LIKE '% за %' THEN 3.0")
                    .append(" WHEN ").append(CATEGORY_NAME).append(" LIKE :qLike ESCAPE '\\' THEN 0.5 ELSE 0 END")
                    .append(" + CASE WHEN lower(coalesce(m.name, '')) LIKE :qLike ESCAPE '\\' THEN 0.5 ELSE 0 END")
                    .append(") as search_rank ");
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

        StringBuilder whereClause = new StringBuilder(
                "WHERE p.active = true AND p.show_flag = true AND p.status = 'AVAILABLE' " +
                // Soft-deleted products already get active=false from ProductService, but a
                // sync that re-activates one by external id would otherwise resurrect it.
                "AND p.deleted = false " +
                "AND (p.image_url IS NOT NULL AND p.image_url <> '') "
        );

        if (StringUtils.hasText(request.getQuery())) {
            // Two independent ways to match, OR-ed together.
            //
            // The old ILIKE branch searched for the whole query as one contiguous
            // substring, so every multi-word query fell back to full text alone:
            // "asus rog backpack", "asus монитор" and "samsung ssd 1tb" each matched
            // nothing through ILIKE. And because the FTS config is 'simple' there is no
            // stemming, so "лаптопи" did not find "лаптоп" — 5 hits against 153.
            //
            // Matching word by word instead, with every word required, fixes both:
            // "лаптопи" now returns 334 and "лаптоп lenovo" 50. The words are matched
            // against the category and manufacturer too, which is where the plural
            // actually lives ("Лаптопи", "Монитори").
            List<String> words = splitQueryWords(request.getQuery());

            whereClause.append("AND (")
                    // Full text — whole words, uses the GIN index, covers descriptions
                    .append(FTS_VECTOR)
                    .append(" @@ plainto_tsquery('").append(language).append("', :query)")
                    .append(" OR p.barcode = :exactQuery");

            if (!words.isEmpty()) {
                whereClause.append(" OR (");
                for (int i = 0; i < words.size(); i++) {
                    if (i > 0) {
                        whereClause.append(" AND ");
                    }
                    whereClause.append(SEARCH_BLOB).append(" ILIKE :word").append(i).append(" ESCAPE '\\'");
                    params.put("word" + i, "%" + escapeLikeWildcards(words.get(i)) + "%");
                }
                whereClause.append(")");
            }
            whereClause.append(") ");

            params.put("exactQuery", request.getQuery());

            String normalized = escapeLikeWildcards(request.getQuery().toLowerCase().replace(" ", ""));
            params.put("qPrefix", normalized + "%");
            params.put("qNameLike", "%" + normalized + "%");
            params.put("qLike", "%" + escapeLikeWildcards(request.getQuery().toLowerCase()) + "%");
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

        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            int filterIndex = 0;
            for (Map.Entry<Long, List<Long>> filter : request.getFilters().entrySet()) {
                Long parameterId = filter.getKey();
                List<Long> optionIds = filter.getValue();

                if (optionIds == null || optionIds.isEmpty()) {
                    continue;
                }

                whereClause.append("AND EXISTS (")
                        .append("SELECT 1 FROM product_parameters pp ")
                        .append("WHERE pp.product_id = p.id ")
                        .append("AND pp.parameter_id = :parameterId").append(filterIndex).append(" ")
                        .append("AND pp.parameter_option_id IN (:optionIds").append(filterIndex).append(")) ");

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
        // Every sort key above has duplicates — 1108 price values are shared by two or
        // more of the 7276 visible products, the largest group being 56. PostgreSQL does
        // not promise a stable order for ties, so with LIMIT/OFFSET a product could show
        // up twice across pages while another never appeared at all. The id breaks every
        // tie and makes paging deterministic.
        sql.append(", p.id ASC");

        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", request.getSize());
        params.put("offset", request.getPage() * request.getSize());

        try {
            List<ProductSearchResult> products = namedJdbcTemplate.query(
                    sql.toString(), params, (rs, rowNum) -> mapRowToProduct(rs, rowNum, request.getLanguage()));

            // Зареждане на параметрите за всички продукти
            if (!products.isEmpty()) {
                List<Long> productIds = products.stream()
                        .map(ProductSearchResult::getId)
                        .collect(Collectors.toList());

                Map<Long, List<ProductParameterResponseDto>> parametersMap = loadProductParameters(productIds, request.getLanguage());

                // Добавяне на параметрите към всеки продукт
                products.forEach(product -> {
                    List<ProductParameterResponseDto> productParams = parametersMap.getOrDefault(product.getId(), Collections.emptyList());
                    product.setSpecifications(productParams);
                });
            }

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
                    // Facets are deliberately not computed here. Building them joined
                    // products against product_parameters, parameters and parameter_options
                    // and grouped over every match rather than the current page — measured
                    // at 677ms on production, more than the rest of the search put together.
                    // Nothing consumed the result: the value reached Redux and no component
                    // ever read it, because the category page gets its facets from
                    // /categories/{id}/filter-facets, which does proper disjunctive faceting.
                    .facets(Collections.emptyMap())
                    .searchTime(0L)
                    .build();

        } catch (Exception e) {
            log.error("PostgreSQL search failed for query: '{}'. Error: {}", request.getQuery(), e.getMessage(), e);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Splits a query into the words that must all be present, capped at
     * {@link #MAX_QUERY_WORDS} so a long paste cannot generate an unbounded
     * number of ANDed ILIKE conditions.
     */
    private List<String> splitQueryWords(String query) {
        return Arrays.stream(query.trim().split("\\s+"))
                .filter(StringUtils::hasText)
                .limit(MAX_QUERY_WORDS)
                .toList();
    }

    /**
     * Escapes the LIKE wildcards. Without this a query of "%" matched every product,
     * and "_" silently matched any single character. Queries are parameterised, so this
     * is about matching what the user typed, not about injection. Backslash is escaped
     * first so it cannot double-escape the wildcards added afterwards; note that
     * ProductSearchService.sanitizeQuery already strips backslashes, this is defence in
     * depth for any other caller.
     */
    private String escapeLikeWildcards(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private Map<Long, List<ProductParameterResponseDto>> loadProductParameters(List<Long> productIds, String language) {
        // Both parameter names are selected and picked apart in Java below, so unlike the
        // other methods here there is no paramNameField to build into the SQL.
        String optionNameField = language.equals("en") ? "po.name_en" : "po.name_bg";

        String sql = "SELECT pp.product_id, " +
                "param.id as parameter_id, " +
                "param.name_en as parameter_name_en, " +
                "param.name_bg as parameter_name_bg, " +
                "po.id as option_id, " +
                "po.external_id as option_external_id, " +
                optionNameField + " as option_name, " +
                "po.sort_order as option_order, " +
                "po.created_at as option_created_at, " +
                "po.updated_at as option_updated_at " +
                "FROM product_parameters pp " +
                "JOIN parameters param ON pp.parameter_id = param.id " +
                "JOIN parameter_options po ON pp.parameter_option_id = po.id " +
                "WHERE pp.product_id IN (:productIds) " +
                "ORDER BY pp.product_id, param.sort_order, param.id, po.sort_order";

        Map<String, Object> params = new HashMap<>();
        params.put("productIds", productIds);

        // Временна структура: Map<ProductId, Map<ParameterId, ProductParameterResponseDto>>
        Map<Long, Map<Long, ProductParameterResponseDto>> tempMap = new HashMap<>();

        namedJdbcTemplate.query(sql, params, rs -> {
            Long productId = rs.getLong("product_id");
            Long parameterId = rs.getLong("parameter_id");
            String parameterNameEn = rs.getString("parameter_name_en");
            String parameterNameBg = rs.getString("parameter_name_bg");

            // Създаване на ParameterOptionResponseDto
            ParameterOptionResponseDto option = new ParameterOptionResponseDto();
            option.setId(rs.getLong("option_id"));
            option.setExternalId(rs.getObject("option_external_id") != null ? rs.getLong("option_external_id") : null);
            option.setName(rs.getString("option_name"));
            option.setParameterId(parameterId);
            // The parameter's name, not the option's — this used to repeat option_name, so
            // an option came back as {name: "16GB", parameterName: "16GB"} instead of
            // {name: "16GB", parameterName: "RAM"}.
            option.setParameterName(language.equals("en") ? parameterNameEn : parameterNameBg);
            option.setOrder(rs.getObject("option_order") != null ? rs.getInt("option_order") : null);
            option.setCreatedAt(rs.getTimestamp("option_created_at") != null ? rs.getTimestamp("option_created_at").toLocalDateTime() : null);
            option.setUpdatedAt(rs.getTimestamp("option_updated_at") != null ? rs.getTimestamp("option_updated_at").toLocalDateTime() : null);

            // Взимане или създаване на ProductParameterResponseDto за този продукт и параметър
            tempMap.putIfAbsent(productId, new HashMap<>());
            Map<Long, ProductParameterResponseDto> productParams = tempMap.get(productId);

            if (!productParams.containsKey(parameterId)) {
                ProductParameterResponseDto paramDto = ProductParameterResponseDto.builder()
                        .parameterId(parameterId)
                        .parameterNameEn(parameterNameEn)
                        .parameterNameBg(parameterNameBg)
                        .options(new HashSet<>())
                        .build();
                productParams.put(parameterId, paramDto);
            }

            // Добавяне на опцията
            productParams.get(parameterId).getOptions().add(option);
        });

        // Конвертиране към финалния формат
        Map<Long, List<ProductParameterResponseDto>> result = new HashMap<>();
        for (Map.Entry<Long, Map<Long, ProductParameterResponseDto>> entry : tempMap.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }

        return result;
    }

    private ProductSearchResult mapRowToProduct(ResultSet rs, int rowNum, String language) throws SQLException {
        String st = rs.getString("status");
        ProductStatus status = (st != null) ? ProductStatus.valueOf(st) : ProductStatus.NOT_AVAILABLE;
        return ProductSearchResult.builder()
                .id(rs.getLong("id"))
                .name(rs.getString(language.equals("en") ? "name_en" : "name_bg"))
                .description(rs.getString(language.equals("en") ? "description_en" : "description_bg"))
                .model(rs.getString("model"))
                .referenceNumber(rs.getString("reference_number"))
                .finalPrice(rs.getBigDecimal("final_price"))
                .discount(rs.getBigDecimal("discount"))
                .status(status.getCode())
                .primaryImageUrl(rs.getString("primary_image_url") != null ? "/api/images/product/" + rs.getLong("id") + "/primary" : null)
                .manufacturerName(rs.getString("manufacturer_name"))
                .categoryName(rs.getString("category_name"))
                .featured(rs.getBoolean("featured"))
                .onSale(rs.getBigDecimal("discount") != null && rs.getBigDecimal("discount").compareTo(BigDecimal.ZERO) > 0)
                .score(rs.getFloat("search_rank"))
                .slug(rs.getString("slug"))
                .platform(rs.getString("platform"))
                .build();
    }

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
                    "JOIN category_parameters cp ON cp.parameter_id = param.id " +
                    "JOIN parameter_options po ON po.parameter_id = param.id " +
                    "INNER JOIN product_parameters pp ON pp.parameter_option_id = po.id " +
                    "INNER JOIN products p ON pp.product_id = p.id " +
                    "   AND p.active = true " +
                    "   AND p.show_flag = true " +
                    "   AND p.status = 'AVAILABLE' " +
                    "   AND p.deleted = false " +
                    "   AND (p.image_url IS NOT NULL AND p.image_url <> '') " +
                    "WHERE cp.category_id = :categoryId " +
                    // Counts are per category. Without this the join above constrained only
                    // which parameters were listed, while the products behind the counts came
                    // from the whole catalogue, so every number was inflated.
                    "  AND p.category_id = :categoryId " +
                    // IS NOT FALSE, matching getFilteredFacets: with "!= false" a NULL is_filter
                    // evaluates to NULL and the parameter silently disappears. The missing space
                    // before GROUP BY also made this "falseGROUP BY", a syntax error that the
                    // catch below swallowed — the method always returned an empty map.
                    "  AND cp.is_filter IS NOT FALSE " +
                    "GROUP BY param.id, param_name, po.id, option_name, param.sort_order, po.sort_order " +
                    "HAVING COUNT(DISTINCT pp.product_id) > 0 " +
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

    /**
     * Returns available filter options with product counts given the currently active filters.
     * Uses disjunctive (independent) faceting: each parameter's options are counted from products
     * matching all OTHER active filters, but NOT that parameter's own filter. This prevents
     * selecting one filter from collapsing the options of other filter groups.
     */
    public Map<String, List<FacetValue>> getFilteredFacets(
            Long categoryId, ProductSearchRequest request, String language) {

        Map<String, List<FacetValue>> facets = new LinkedHashMap<>();
        String paramNameField = language.equals("en") ? "param.name_en" : "param.name_bg";
        String optionNameField = language.equals("en") ? "po.name_en" : "po.name_bg";
        Map<Long, List<Long>> activeFilters = request.getFilters() != null ? request.getFilters() : Collections.emptyMap();

        // ── Step 1: load all filterable parameters for this category (ordered) ──────
        String cpSql = "SELECT param.id as param_id, " + paramNameField + " as param_name " +
                "FROM parameters param " +
                "JOIN category_parameters cp ON cp.parameter_id = param.id " +
                "WHERE cp.category_id = :categoryId AND cp.is_filter IS NOT FALSE " +
                "ORDER BY param.sort_order, param_name";

        Map<Long, String> categoryParamMap = new LinkedHashMap<>();
        try {
            namedJdbcTemplate.query(cpSql, Map.of("categoryId", categoryId), (RowCallbackHandler) rs ->
                    categoryParamMap.put(rs.getLong("param_id"), rs.getString("param_name")));
        } catch (Exception e) {
            log.error("Failed to load category parameters for category {}: {}", categoryId, e.getMessage(), e);
            return facets;
        }

        // ── Step 2: for each parameter run an independent query ───────────────────
        // Each query applies all active filters EXCEPT the current parameter's own filter.
        for (Map.Entry<Long, String> paramEntry : categoryParamMap.entrySet()) {
            Long thisParamId = paramEntry.getKey();
            String thisParamName = paramEntry.getValue();
            String facetKey = thisParamId + ":" + thisParamName;

            Map<String, Object> params = new HashMap<>();
            params.put("categoryId", categoryId);
            params.put("thisParamId", thisParamId);

            StringBuilder where = new StringBuilder(
                    "WHERE p.active = true AND p.show_flag = true " +
                    "AND p.status = 'AVAILABLE' AND p.deleted = false " +
                    "AND (p.image_url IS NOT NULL AND p.image_url <> '') " +
                    "AND p.category_id = :categoryId "
            );

            if (request.getManufacturers() != null && !request.getManufacturers().isEmpty()) {
                where.append("AND p.manufacturer_id IN (:manufacturerIds) ");
                params.put("manufacturerIds", request.getManufacturers().stream().map(Long::valueOf).toList());
            }
            if (request.getMinPrice() != null) {
                where.append("AND p.final_price >= :minPrice ");
                params.put("minPrice", request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                where.append("AND p.final_price <= :maxPrice ");
                params.put("maxPrice", request.getMaxPrice());
            }

            // Apply all active filters EXCEPT this parameter's own filter
            int idx = 0;
            for (Map.Entry<Long, List<Long>> filterEntry : activeFilters.entrySet()) {
                if (filterEntry.getKey().equals(thisParamId)) continue;
                if (filterEntry.getValue() == null || filterEntry.getValue().isEmpty()) continue;
                where.append("AND EXISTS (SELECT 1 FROM product_parameters pp2 ")
                        .append("WHERE pp2.product_id = p.id ")
                        .append("AND pp2.parameter_id = :fParamId").append(idx).append(" ")
                        .append("AND pp2.parameter_option_id IN (:fOptIds").append(idx).append(")) ");
                params.put("fParamId" + idx, filterEntry.getKey());
                params.put("fOptIds" + idx, filterEntry.getValue());
                idx++;
            }

            String optSql = "SELECT po.id as option_id, " + optionNameField + " as option_name, " +
                    "COUNT(DISTINCT p.id) as cnt " +
                    "FROM products p " +
                    "JOIN product_parameters pp ON pp.product_id = p.id AND pp.parameter_id = :thisParamId " +
                    "JOIN parameter_options po ON pp.parameter_option_id = po.id " +
                    where +
                    "GROUP BY po.id, option_name, po.sort_order " +
                    "HAVING COUNT(DISTINCT p.id) > 0 " +
                    "ORDER BY po.sort_order, option_name";

            try {
                namedJdbcTemplate.query(optSql, params, rs -> {
                    Long optionId = rs.getLong("option_id");
                    String optionName = rs.getString("option_name");
                    Long count = rs.getLong("cnt");

                    boolean isSelected = activeFilters.containsKey(thisParamId) &&
                            activeFilters.get(thisParamId) != null &&
                            activeFilters.get(thisParamId).contains(optionId);

                    FacetValue facetValue = FacetValue.builder()
                            .id(optionId)
                            .value(optionName)
                            .count(count)
                            .selected(isSelected)
                            .build();

                    facets.computeIfAbsent(facetKey, k -> new ArrayList<>()).add(facetValue);
                });
            } catch (Exception e) {
                log.error("Failed to get facets for param {} in category {}: {}", thisParamId, categoryId, e.getMessage(), e);
            }
        }

        // Manufacturer counts — apply parameter filters only (NOT manufacturer filter),
        // so the user can see which brands have products given the selected parameters.
        try {
            StringBuilder mWhere = new StringBuilder(
                    "WHERE p.active = true AND p.show_flag = true " +
                    "AND p.status = 'AVAILABLE' AND p.deleted = false " +
                    "AND (p.image_url IS NOT NULL AND p.image_url <> '') " +
                    "AND p.category_id = :categoryId "
            );
            Map<String, Object> mParams = new HashMap<>();
            mParams.put("categoryId", categoryId);

            if (request.getMinPrice() != null) {
                mWhere.append("AND p.final_price >= :minPrice ");
                mParams.put("minPrice", request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                mWhere.append("AND p.final_price <= :maxPrice ");
                mParams.put("maxPrice", request.getMaxPrice());
            }

            if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                int idx = 0;
                for (Map.Entry<Long, List<Long>> entry : request.getFilters().entrySet()) {
                    if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
                    mWhere.append("AND EXISTS (SELECT 1 FROM product_parameters pp2 ")
                            .append("WHERE pp2.product_id = p.id ")
                            .append("AND pp2.parameter_id = :mParamId").append(idx).append(" ")
                            .append("AND pp2.parameter_option_id IN (:mOptIds").append(idx).append(")) ");
                    mParams.put("mParamId" + idx, entry.getKey());
                    mParams.put("mOptIds" + idx, entry.getValue());
                    idx++;
                }
            }

            String mSql = "SELECT m.id as manufacturer_id, m.name as manufacturer_name, " +
                    "COUNT(DISTINCT p.id) as cnt " +
                    "FROM products p " +
                    "JOIN manufacturers m ON p.manufacturer_id = m.id " +
                    mWhere +
                    "GROUP BY m.id, m.name " +
                    "HAVING COUNT(DISTINCT p.id) > 0 " +
                    "ORDER BY m.name";

            List<FacetValue> manufacturerFacets = new ArrayList<>();
            namedJdbcTemplate.query(mSql, mParams, rs -> {
                manufacturerFacets.add(FacetValue.builder()
                        .id(rs.getLong("manufacturer_id"))
                        .value(rs.getString("manufacturer_name"))
                        .count(rs.getLong("cnt"))
                        .selected(false)
                        .build());
            });

            if (!manufacturerFacets.isEmpty()) {
                facets.put("manufacturers:Производител", manufacturerFacets);
            }
        } catch (Exception e) {
            log.error("Failed to get manufacturer facets for category {}: {}", categoryId, e.getMessage(), e);
        }

        return facets;
    }

    public ProductSearchResponse searchProductsFuzzy(ProductSearchRequest request) {
        String language = "simple";
        String nameField = request.getLanguage().equals("en") ? "name_en" : "name_bg";

        Map<String, Object> params = new HashMap<>();
        params.put("query", request.getQuery());
        params.put("limit", request.getSize());
        params.put("offset", request.getPage() * request.getSize());

        // The fallback used to ignore the caller's filters entirely, so a fruitless
        // search inside one category answered with near-matches from the whole shop.
        StringBuilder scope = new StringBuilder();
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            scope.append("AND p.category_id IN (:categoryIds) ");
            params.put("categoryIds", request.getCategories().stream().map(Long::valueOf).toList());
        }
        if (request.getManufacturers() != null && !request.getManufacturers().isEmpty()) {
            scope.append("AND p.manufacturer_id IN (:manufacturerIds) ");
            params.put("manufacturerIds", request.getManufacturers().stream().map(Long::valueOf).toList());
        }
        if (request.getMinPrice() != null) {
            scope.append("AND p.final_price >= :minPrice ");
            params.put("minPrice", request.getMinPrice());
        }
        if (request.getMaxPrice() != null) {
            scope.append("AND p.final_price <= :maxPrice ");
            params.put("maxPrice", request.getMaxPrice());
        }
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            int i = 0;
            for (Map.Entry<Long, List<Long>> filter : request.getFilters().entrySet()) {
                if (filter.getValue() == null || filter.getValue().isEmpty()) {
                    continue;
                }
                scope.append("AND EXISTS (SELECT 1 FROM product_parameters pp ")
                        .append("WHERE pp.product_id = p.id AND pp.parameter_id = :fzParamId").append(i)
                        .append(" AND pp.parameter_option_id IN (:fzOptIds").append(i).append(")) ");
                params.put("fzParamId" + i, filter.getKey());
                params.put("fzOptIds" + i, filter.getValue());
                i++;
            }
        }

        String sql = "SELECT p.id, p.name_bg, p.name_en, p.description_bg, p.description_en, " +
                "p.model, p.reference_number, p.final_price, p.discount, p.featured, p.status, " +
                "p.slug, p.platform, m.name as manufacturer_name, c." + nameField + " as category_name, " +
                "p.image_url AS primary_image_url, " +
                "GREATEST(" +
                "  CASE WHEN p.name_bg IS NOT NULL THEN word_similarity(:query, p.name_bg) ELSE 0 END, " +
                "  CASE WHEN p.name_en IS NOT NULL THEN word_similarity(:query, p.name_en) ELSE 0 END" +
                ") as search_rank " +
                "FROM products p " +
                "LEFT JOIN manufacturers m ON p.manufacturer_id = m.id " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE p.active = true AND p.show_flag = true " +
                "AND p.status = 'AVAILABLE' AND p.deleted = false " +
                "AND (p.image_url IS NOT NULL AND p.image_url <> '') " +
                "AND (p.name_bg IS NOT NULL AND word_similarity(:query, p.name_bg) > 0.35 " +
                "  OR p.name_en IS NOT NULL AND word_similarity(:query, p.name_en) > 0.35) " +
                scope +
                // p.id breaks ties so paging stays deterministic — see searchProducts()
                "ORDER BY search_rank DESC, p.final_price ASC, p.id ASC " +
                "LIMIT :limit OFFSET :offset";

        String countSql = "SELECT COUNT(*) FROM products p " +
                "LEFT JOIN manufacturers m ON p.manufacturer_id = m.id " +
                "WHERE p.active = true AND p.show_flag = true " +
                "AND p.status = 'AVAILABLE' AND p.deleted = false " +
                "AND (p.image_url IS NOT NULL AND p.image_url <> '') " +
                "AND (p.name_bg IS NOT NULL AND word_similarity(:query, p.name_bg) > 0.35 " +
                "  OR p.name_en IS NOT NULL AND word_similarity(:query, p.name_en) > 0.35) " +
                scope;

        try {
            List<ProductSearchResult> products = namedJdbcTemplate.query(
                    sql, params, (rs, rowNum) -> mapRowToProduct(rs, rowNum, request.getLanguage()));

            if (!products.isEmpty()) {
                List<Long> productIds = products.stream()
                        .map(ProductSearchResult::getId)
                        .collect(Collectors.toList());
                Map<Long, List<ProductParameterResponseDto>> parametersMap =
                        loadProductParameters(productIds, request.getLanguage());
                products.forEach(product -> product.setSpecifications(
                        parametersMap.getOrDefault(product.getId(), Collections.emptyList())));
            }

            Long totalCount = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
            long totalElements = totalCount != null ? totalCount : 0;
            int totalPages = (int) Math.ceil((double) totalElements / request.getSize());

            return ProductSearchResponse.builder()
                    .products(products)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .currentPage(request.getPage())
                    .facets(new HashMap<>())
                    .searchTime(0L)
                    .build();

        } catch (Exception e) {
            log.error("Fuzzy search failed for query: '{}'. Error: {}", request.getQuery(), e.getMessage(), e);
            throw new RuntimeException("Fuzzy search failed: " + e.getMessage(), e);
        }
    }
}