package com.techstore.service.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.techstore.dto.filter.CategoryFiltersRequest;
import com.techstore.dto.filter.CategoryFiltersResponse;
import com.techstore.service.CategoryAliasResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serves the filter panel of a category from the canonical filter layer.
 * <p>
 * Groups are {@code category_filters} rows and values are {@code filter_values}, so one attribute is
 * one group whatever supplier the products come from. The category's products and their values are
 * loaded with a single query and cached; counts are computed in memory. Each group is counted against
 * the selection in all the other groups but not its own, which is what lets a shopper tick a second
 * value in a group they already filtered on.
 */
@Slf4j
@Service
public class CategoryFilterService {

    private static final String VISIBLE_PRODUCT = """
            p.active AND p.show_flag AND p.status = 'AVAILABLE' AND NOT p.deleted
            AND p.image_url IS NOT NULL AND p.image_url <> ''""";

    private final NamedParameterJdbcTemplate named;
    private final CategoryAliasResolver categoryAliasResolver;

    /** Keyed by the resolved (non-alias) category id. Cleared after every rebuild. */
    private final Cache<Long, CategoryData> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public CategoryFilterService(JdbcTemplate jdbc, CategoryAliasResolver categoryAliasResolver) {
        this.named = new NamedParameterJdbcTemplate(jdbc);
        this.categoryAliasResolver = categoryAliasResolver;
    }

    public void evictAll() {
        cache.invalidateAll();
    }

    /**
     * @param includeHidden also return groups switched off for the category (admins toggle them back)
     */
    @Transactional(readOnly = true)
    public CategoryFiltersResponse getFilters(Long categoryId, CategoryFiltersRequest request, boolean includeHidden) {
        Long resolvedId = resolveAlias(categoryId);
        CategoryData data = cache.get(resolvedId, this::load);
        boolean english = request != null && "en".equalsIgnoreCase(request.language());
        return count(categoryId, data, Selection.of(request, data), english, includeHidden);
    }

    /**
     * Shows or hides a filter group in a category. The row becomes MANUAL, so the nightly rebuild keeps
     * the decision; a hidden row also keeps the rebuild from adding the attribute back.
     */
    @Transactional
    public void setVisibility(Long categoryId, Long attributeId, boolean visible) {
        Long resolvedId = resolveAlias(categoryId);
        named.update("""
                INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
                VALUES (:categoryId, :attributeId, 500, :visible, 'MANUAL')
                ON CONFLICT (category_id, attribute_id)
                DO UPDATE SET visible = EXCLUDED.visible, origin = 'MANUAL'""",
                new MapSqlParameterSource()
                        .addValue("categoryId", resolvedId)
                        .addValue("attributeId", attributeId)
                        .addValue("visible", visible));
        cache.invalidate(resolvedId);
    }

    /**
     * Turns a pre-V38 filter link ({@code ?param_5821=150582,108657}) into the canonical selection for
     * the category. Options the index does not map, or attributes the category does not show, are
     * dropped — the link then filters on less rather than failing.
     */
    @Transactional(readOnly = true)
    public Map<Long, List<Long>> translateLegacy(Long categoryId, Map<Long, List<Long>> legacyFilters) {
        Set<Long> optionIds = legacyFilters == null ? Set.of() : legacyFilters.values().stream()
                .filter(v -> v != null)
                .flatMap(Collection::stream)
                .limit(500)
                .collect(Collectors.toSet());
        if (optionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        named.query("""
                SELECT DISTINCT fom.attribute_id, fom.value_id
                FROM filter_option_map fom
                JOIN category_filters cf ON cf.attribute_id = fom.attribute_id
                                        AND cf.category_id = :categoryId AND cf.visible
                WHERE fom.parameter_option_id IN (:optionIds)
                ORDER BY fom.attribute_id, fom.value_id""",
                new MapSqlParameterSource()
                        .addValue("categoryId", resolveAlias(categoryId))
                        .addValue("optionIds", optionIds),
                rs -> {
                    result.computeIfAbsent(rs.getLong("attribute_id"), k -> new ArrayList<>())
                            .add(rs.getLong("value_id"));
                });
        return result;
    }

    private Long resolveAlias(Long categoryId) {
        return categoryAliasResolver.resolve(categoryId);
    }

    // ── Loading ────────────────────────────────────────────────────────────────────────────────────

    private CategoryData load(Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource("categoryId", categoryId);

        List<GroupDef> groups = named.query("""
                SELECT a.id, a.slug, a.name_bg, a.name_en, a.unit, a.value_type, cf.visible
                FROM category_filters cf
                JOIN filter_attributes a ON a.id = cf.attribute_id
                WHERE cf.category_id = :categoryId
                ORDER BY cf.sort_order, a.name_bg""", params,
                (rs, i) -> new GroupDef(rs.getLong("id"), rs.getString("slug"), rs.getString("name_bg"),
                        rs.getString("name_en"), rs.getString("unit"), rs.getString("value_type"),
                        rs.getBoolean("visible")));

        Map<Long, ProductRow> products = new LinkedHashMap<>();
        Map<Long, String> manufacturerNames = new HashMap<>();
        named.query("""
                SELECT p.id, p.manufacturer_id, m.name AS manufacturer_name, p.final_price,
                       f.attribute_id, f.value_id
                FROM products p
                LEFT JOIN manufacturers m ON m.id = p.manufacturer_id
                LEFT JOIN product_filter_values f
                       ON f.product_id = p.id
                      AND f.attribute_id IN (SELECT attribute_id FROM category_filters
                                             WHERE category_id = :categoryId)
                WHERE p.category_id = :categoryId AND %s""".formatted(VISIBLE_PRODUCT), params,
                rs -> {
                    long productId = rs.getLong("id");
                    Long manufacturerId = (Long) rs.getObject("manufacturer_id");
                    BigDecimal price = rs.getBigDecimal("final_price");
                    ProductRow row = products.computeIfAbsent(productId,
                            id -> new ProductRow(manufacturerId, price, new HashMap<>()));
                    if (manufacturerId != null) {
                        manufacturerNames.putIfAbsent(manufacturerId, rs.getString("manufacturer_name"));
                    }
                    Long attributeId = (Long) rs.getObject("attribute_id");
                    if (attributeId != null) {
                        row.values().computeIfAbsent(attributeId, k -> new HashSet<>()).add(rs.getLong("value_id"));
                    }
                });

        Set<Long> valueIds = products.values().stream()
                .flatMap(p -> p.values().values().stream())
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        Map<Long, ValueDef> values = valueIds.isEmpty() ? Map.of() : named.query("""
                SELECT id, attribute_id, value_bg, value_en, numeric_value, sort_order
                FROM filter_values WHERE id IN (:ids)""",
                new MapSqlParameterSource("ids", valueIds),
                (rs, i) -> new ValueDef(rs.getLong("id"), rs.getString("value_bg"), rs.getString("value_en"),
                        rs.getBigDecimal("numeric_value"), (Integer) rs.getObject("sort_order")))
                .stream().collect(Collectors.toMap(ValueDef::id, v -> v));

        return new CategoryData(groups, List.copyOf(products.values()), values, manufacturerNames);
    }

    // ── Counting ───────────────────────────────────────────────────────────────────────────────────

    private CategoryFiltersResponse count(Long categoryId, CategoryData data, Selection selection, boolean english,
                                          boolean includeHidden) {
        long total = 0;
        Map<Long, Map<Long, Long>> valueCounts = new HashMap<>();
        Map<Long, Map<Long, Long>> baseCounts = new HashMap<>();
        Map<Long, Long> manufacturerCounts = new HashMap<>();
        Map<Long, Long> manufacturerBase = new HashMap<>();

        for (ProductRow p : data.products()) {
            boolean priceOk = selection.priceMatches(p.price());
            boolean manufacturerOk = selection.manufacturers().isEmpty()
                    || (p.manufacturerId() != null && selection.manufacturers().contains(p.manufacturerId()));
            Set<Long> failed = selection.failedAttributes(p);

            if (priceOk && manufacturerOk && failed.isEmpty()) {
                total++;
            }
            if (p.manufacturerId() != null) {
                manufacturerBase.merge(p.manufacturerId(), 1L, Long::sum);
                if (priceOk && failed.isEmpty()) {
                    manufacturerCounts.merge(p.manufacturerId(), 1L, Long::sum);
                }
            }
            for (GroupDef g : data.groups()) {
                Set<Long> values = p.values().get(g.id());
                if (values == null) {
                    continue;
                }
                Map<Long, Long> base = baseCounts.computeIfAbsent(g.id(), k -> new HashMap<>());
                values.forEach(v -> base.merge(v, 1L, Long::sum));
                // Counted against every other group's selection, not its own.
                boolean otherGroupsOk = failed.isEmpty() || (failed.size() == 1 && failed.contains(g.id()));
                if (priceOk && manufacturerOk && otherGroupsOk) {
                    Map<Long, Long> counts = valueCounts.computeIfAbsent(g.id(), k -> new HashMap<>());
                    values.forEach(v -> counts.merge(v, 1L, Long::sum));
                }
            }
        }

        List<CategoryFiltersResponse.Group> groups = new ArrayList<>();
        for (GroupDef g : data.groups()) {
            if (!g.visible() && !includeHidden) {
                continue;
            }
            Map<Long, Long> base = baseCounts.getOrDefault(g.id(), Map.of());
            Map<Long, Long> counts = valueCounts.getOrDefault(g.id(), Map.of());
            Set<Long> selected = selection.attributes().getOrDefault(g.id(), Set.of());
            List<CategoryFiltersResponse.Option> options = base.keySet().stream()
                    .map(data.values()::get)
                    .filter(v -> v != null)
                    .sorted(valueOrder(g, base))
                    .map(v -> new CategoryFiltersResponse.Option(v.id(), v.label(english),
                            counts.getOrDefault(v.id(), 0L), selected.contains(v.id())))
                    .toList();
            if (!g.visible() || options.size() >= 2 || options.stream().anyMatch(CategoryFiltersResponse.Option::selected)) {
                groups.add(new CategoryFiltersResponse.Group(g.id(), g.slug(), g.label(english), g.unit(),
                        g.valueType(), g.visible(), options));
            }
        }

        List<CategoryFiltersResponse.Option> manufacturers = manufacturerBase.keySet().stream()
                .sorted(Comparator.comparing((Long id) -> String.valueOf(data.manufacturerNames().get(id)),
                        String.CASE_INSENSITIVE_ORDER))
                .map(id -> new CategoryFiltersResponse.Option(id, data.manufacturerNames().get(id),
                        manufacturerCounts.getOrDefault(id, 0L), selection.manufacturers().contains(id)))
                .toList();

        return new CategoryFiltersResponse(categoryId, total, groups, manufacturers);
    }

    /**
     * Numbers ascending for numeric attributes; otherwise curated order first, then the values most
     * products in the category have. Ordered by the unfiltered counts so the list does not reshuffle
     * while the shopper clicks.
     */
    private static Comparator<ValueDef> valueOrder(GroupDef group, Map<Long, Long> baseCounts) {
        if ("NUMERIC".equals(group.valueType())) {
            return Comparator.comparing(ValueDef::number, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(ValueDef::labelBg);
        }
        return Comparator.comparing(ValueDef::sortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(v -> -baseCounts.getOrDefault(v.id(), 0L))
                .thenComparing(ValueDef::labelBg, String.CASE_INSENSITIVE_ORDER);
    }

    // ── Model ──────────────────────────────────────────────────────────────────────────────────────

    private record CategoryData(List<GroupDef> groups, List<ProductRow> products,
                                Map<Long, ValueDef> values, Map<Long, String> manufacturerNames) {
    }

    private record GroupDef(Long id, String slug, String nameBg, String nameEn, String unit, String valueType,
                            boolean visible) {
        String label(boolean english) {
            return english && nameEn != null ? nameEn : nameBg;
        }
    }

    private record ValueDef(Long id, String labelBg, String labelEn, BigDecimal number, Integer sortOrder) {
        String label(boolean english) {
            return english && labelEn != null ? labelEn : labelBg;
        }
    }

    private record ProductRow(Long manufacturerId, BigDecimal price, Map<Long, Set<Long>> values) {
    }

    /** The request reduced to what the category can actually filter on. */
    private record Selection(Map<Long, Set<Long>> attributes, Set<Long> manufacturers,
                             BigDecimal minPrice, BigDecimal maxPrice) {

        static Selection of(CategoryFiltersRequest request, CategoryData data) {
            Map<Long, Set<Long>> attributes = new HashMap<>();
            Set<Long> known = data.groups().stream().filter(GroupDef::visible).map(GroupDef::id)
                    .collect(Collectors.toSet());
            if (request != null && request.attributeFilters() != null) {
                request.attributeFilters().forEach((attributeId, values) -> {
                    if (attributeId != null && known.contains(attributeId) && values != null && !values.isEmpty()) {
                        attributes.put(attributeId, new HashSet<>(values));
                    }
                });
            }
            Set<Long> manufacturers = request == null || request.manufacturers() == null
                    ? Set.of() : new HashSet<>(request.manufacturers());
            return new Selection(attributes, manufacturers,
                    request == null ? null : request.minPrice(), request == null ? null : request.maxPrice());
        }

        boolean priceMatches(BigDecimal price) {
            if (price == null) {
                return minPrice == null && maxPrice == null;
            }
            return (minPrice == null || price.compareTo(minPrice) >= 0)
                    && (maxPrice == null || price.compareTo(maxPrice) <= 0);
        }

        /** Selected attributes the product does not satisfy. */
        Set<Long> failedAttributes(ProductRow product) {
            if (attributes.isEmpty()) {
                return Set.of();
            }
            Set<Long> failed = new HashSet<>();
            attributes.forEach((attributeId, selected) -> {
                Set<Long> values = product.values().get(attributeId);
                if (values == null || values.stream().noneMatch(selected::contains)) {
                    failed.add(attributeId);
                }
            });
            return failed;
        }
    }
}
