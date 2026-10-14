package com.techstore.service.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.service.filter.parser.FilterValueParser;
import com.techstore.service.filter.parser.ParsedValue;
import com.techstore.util.SlugUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rebuilds the canonical filter index (V38) from the raw supplier layer.
 * <p>
 * The syncs own {@code parameters}, {@code parameter_options} and {@code product_parameters} and
 * rewrite them every night, which is why every earlier attempt to de-duplicate filters by editing
 * those tables was undone. This service reads them and writes only the derived {@code filter_*}
 * tables, {@code product_filter_values} and the AUTO rows of {@code category_filters}. The shop-owned
 * rules and MANUAL rows are never modified here, so a rebuild is deterministic: the same raw data and
 * rules always produce the same index.
 * <p>
 * Everything runs in one transaction behind an advisory lock. Readers keep seeing the previous index
 * until the commit, and a failed rebuild leaves it untouched. A dry run executes every step and rolls
 * back, which makes it the way to preview the effect of new rules.
 */
@Slf4j
@Service
public class FilterIndexService {

    /**
     * An automatic group whose values average more than this many characters is a specification
     * written as prose ("Input Voltage: 100-240V; Input Current: ..."), not a list of options.
     */
    private static final int MAX_AUTO_LABEL_LENGTH = 40;

    private static final String VISIBLE_PRODUCT = """
            active AND show_flag AND status = 'AVAILABLE' AND NOT deleted
            AND image_url IS NOT NULL AND image_url <> ''""";

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final TransactionTemplate tx;
    private final TransactionTemplate txNew;
    private final Map<String, FilterValueParser> parsers;
    private final ObjectMapper objectMapper;
    private final CategoryFilterService categoryFilterService;

    @Value("${app.filters.auto.min-coverage:0.30}")
    private double minCoverage;

    @Value("${app.filters.auto.max-values:40}")
    private int maxAutoValues;

    @Value("${app.filters.auto.max-groups:12}")
    private int maxGroups;

    /** A category with fewer visible products than this gets {@link #maxGroupsSmall} automatic groups. */
    @Value("${app.filters.auto.small-category-products:10}")
    private int smallCategoryProducts;

    @Value("${app.filters.auto.max-groups-small:5}")
    private int maxGroupsSmall;

    /**
     * Uses the auto-configured {@link JdbcTemplate}: the search template carries a 15 s query timeout
     * that a full rebuild must not be cut off by.
     */
    public FilterIndexService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                              List<FilterValueParser> parsers, ObjectMapper objectMapper,
                              CategoryFilterService categoryFilterService) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
        this.tx = new TransactionTemplate(transactionManager);
        this.txNew = new TransactionTemplate(transactionManager);
        this.txNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.parsers = parsers.stream().collect(Collectors.toMap(FilterValueParser::code, Function.identity()));
        this.objectMapper = objectMapper;
        this.categoryFilterService = categoryFilterService;
    }

    public FilterRebuildResult rebuild(String trigger, boolean dryRun) {
        LocalDateTime started = LocalDateTime.now();
        long start = System.currentTimeMillis();
        try {
            FilterRebuildResult result = tx.execute(status -> {
                Boolean locked = jdbc.queryForObject(
                        "SELECT pg_try_advisory_xact_lock(hashtext('filter_index_rebuild'))", Boolean.class);
                if (!Boolean.TRUE.equals(locked)) {
                    log.warn("Filter index rebuild skipped ({}): another rebuild is running", trigger);
                    return FilterRebuildResult.skipped(trigger, dryRun);
                }
                jdbc.execute("SET LOCAL statement_timeout = '5min'");
                jdbc.execute("SET LOCAL lock_timeout = '10s'");

                Map<String, Object> stats = runSteps();
                long duration = System.currentTimeMillis() - start;
                stats.put("durationMs", duration);

                if (dryRun) {
                    status.setRollbackOnly();
                    return new FilterRebuildResult("DRY_RUN", trigger, true, duration, stats);
                }
                recordRun(started, "SUCCESS", trigger, false, stats, null);
                return new FilterRebuildResult("SUCCESS", trigger, false, duration, stats);
            });
            if (result != null) {
                if ("SUCCESS".equals(result.status())) {
                    categoryFilterService.evictAll();
                }
                log.info("Filter index rebuild {} ({}) in {} ms: {}", result.status(), trigger,
                        result.durationMs(), result.stats());
            }
            return result;
        } catch (RuntimeException e) {
            txNew.executeWithoutResult(s -> recordRun(started, "FAILED", trigger, dryRun, null, e.getMessage()));
            throw e;
        }
    }

    private Map<String, Object> runSteps() {
        Map<String, Object> stats = new LinkedHashMap<>();

        createBaseTables();
        resolveParameterMap();
        List<String> autoCreated = createAutoAttributes();
        if (!autoCreated.isEmpty()) {
            resolveParameterMap();
        }
        stats.put("autoAttributesCreated", autoCreated);

        stats.put("optionValues", resolveOptionValues());
        stats.putAll(syncProductFilterValues());
        stats.putAll(selectCategoryFilters());
        publishDerivedTables();
        stats.putAll(summary());
        return stats;
    }

    // ── Step 1: the raw rows the index is built from ────────────────────────────────────────────────

    private void createBaseTables() {
        jdbc.execute("""
                CREATE TEMP TABLE tmp_vis ON COMMIT DROP AS
                SELECT id, category_id FROM products
                WHERE %s AND category_id IS NOT NULL""".formatted(VISIBLE_PRODUCT));
        jdbc.execute("CREATE INDEX ON tmp_vis (id)");
        jdbc.execute("ANALYZE tmp_vis");

        // Every product is indexed, not only visible ones, so a product coming back in stock needs no
        // rebuild to be filterable. The supplier is part of the key because suppliers adopt each
        // other's parameter rows: parameters.platform does not say where a value came from.
        jdbc.execute("""
                CREATE TEMP TABLE tmp_combo ON COMMIT DROP AS
                SELECT DISTINCT pp.parameter_id, pr.category_id, COALESCE(pr.platform, '') AS platform
                FROM product_parameters pp
                JOIN products pr ON pr.id = pp.product_id
                WHERE NOT pr.deleted AND pr.category_id IS NOT NULL""");
        jdbc.execute("ANALYZE tmp_combo");
    }

    // ── Step 2: which rule applies to each raw parameter ────────────────────────────────────────────

    /**
     * Picks one rule per (parameter, category, supplier). Most specific wins: curated over automatic,
     * then parameter id over name, then category-scoped, then supplier-scoped, then Bulgarian name over
     * English name; ties go to the older rule.
     */
    private void resolveParameterMap() {
        jdbc.execute("DROP TABLE IF EXISTS tmp_param_map");
        jdbc.execute("""
                CREATE TEMP TABLE tmp_param_map ON COMMIT DROP AS
                SELECT DISTINCT ON (c.parameter_id, c.category_id, c.platform)
                       c.parameter_id, c.category_id, c.platform, s.action, s.attribute_id
                FROM tmp_combo c
                JOIN parameters p ON p.id = c.parameter_id
                JOIN LATERAL (
                    SELECT fs.id, fs.action, fs.attribute_id, fs.platform, fs.category_id, fs.origin, 16 AS w
                    FROM filter_attribute_sources fs WHERE fs.parameter_id = c.parameter_id
                    UNION ALL
                    SELECT fs.id, fs.action, fs.attribute_id, fs.platform, fs.category_id, fs.origin, 2
                    FROM filter_attribute_sources fs WHERE fs.name_norm = filter_norm(p.name_bg)
                    UNION ALL
                    SELECT fs.id, fs.action, fs.attribute_id, fs.platform, fs.category_id, fs.origin, 1
                    FROM filter_attribute_sources fs WHERE fs.name_norm = filter_norm(p.name_en)
                ) s ON (s.platform IS NULL OR s.platform = c.platform)
                   AND (s.category_id IS NULL OR s.category_id = c.category_id)
                ORDER BY c.parameter_id, c.category_id, c.platform,
                         (CASE WHEN s.origin = 'MANUAL' THEN 32 ELSE 0 END) + s.w
                           + (CASE WHEN s.category_id IS NOT NULL THEN 8 ELSE 0 END)
                           + (CASE WHEN s.platform IS NOT NULL THEN 4 ELSE 0 END) DESC,
                         s.id""");
        jdbc.execute("CREATE INDEX ON tmp_param_map (parameter_id, category_id, platform)");
        jdbc.execute("ANALYZE tmp_param_map");
    }

    // ── Step 3: day-one baseline — one attribute per parameter name that is worth filtering ─────────

    /**
     * Creates an AUTO attribute for every unmapped parameter name that makes a usable filter in at
     * least one category: at least {@code minCoverage} of its visible products carry it, with 2 to
     * {@code maxAutoValues} distinct, short values. The name rule it gets maps the name for every supplier,
     * which is what merges "Цвят" from VALI, ASBIS, MOST and TEKRA into one group. Names covered by a
     * rule — including the IGNORE/HIDE junk rules — are never auto-created.
     */
    private List<String> createAutoAttributes() {
        List<String> names = named.queryForList("""
                WITH unmapped AS (
                    SELECT c.parameter_id, c.category_id, c.platform, filter_norm(p.name_bg) AS name
                    FROM tmp_combo c
                    JOIN parameters p ON p.id = c.parameter_id
                    WHERE filter_norm(p.name_bg) IS NOT NULL
                      AND NOT EXISTS (SELECT 1 FROM tmp_param_map m
                                      WHERE m.parameter_id = c.parameter_id AND m.category_id = c.category_id
                                        AND m.platform = c.platform)
                ),
                category_size AS (SELECT category_id, count(*) AS n FROM tmp_vis GROUP BY category_id),
                per_category AS (
                    SELECT u.name, v.category_id,
                           count(DISTINCT v.id) AS products,
                           count(DISTINCT filter_norm(po.name_bg)) AS vals,
                           avg(length(po.name_bg)) AS avg_label_length
                    FROM unmapped u
                    JOIN product_parameters pp ON pp.parameter_id = u.parameter_id
                    JOIN tmp_vis v ON v.id = pp.product_id AND v.category_id = u.category_id
                    JOIN products pr ON pr.id = v.id AND COALESCE(pr.platform, '') = u.platform
                    JOIN parameter_options po ON po.id = pp.parameter_option_id
                    GROUP BY u.name, v.category_id
                )
                SELECT pc.name
                FROM per_category pc
                JOIN category_size cs ON cs.category_id = pc.category_id
                WHERE pc.vals BETWEEN 2 AND :maxValues
                  AND pc.avg_label_length <= :maxLabelLength
                  AND pc.products >= 2
                  AND pc.products >= :minCoverage * cs.n
                GROUP BY pc.name
                ORDER BY pc.name""",
                new MapSqlParameterSource()
                        .addValue("maxValues", maxAutoValues)
                        .addValue("maxLabelLength", MAX_AUTO_LABEL_LENGTH)
                        .addValue("minCoverage", minCoverage),
                String.class);
        if (names.isEmpty()) {
            return List.of();
        }

        // Display name: the spelling most parameter rows use ("Цвят" rather than "цвят"), without the
        // trailing punctuation some suppliers add ("Тип слушалки.").
        Map<String, Map<String, Object>> display = named.queryForList("""
                SELECT filter_norm(name_bg) AS name,
                       mode() WITHIN GROUP (ORDER BY regexp_replace(btrim(name_bg), '[[:space:].,:;]+$', '')) AS name_bg,
                       mode() WITHIN GROUP (ORDER BY regexp_replace(btrim(name_en), '[[:space:].,:;]+$', '')) AS name_en
                FROM parameters
                WHERE filter_norm(name_bg) IN (:names)
                GROUP BY filter_norm(name_bg)""",
                new MapSqlParameterSource("names", names)).stream()
                .collect(Collectors.toMap(r -> (String) r.get("name"), r -> r));

        Set<String> slugs = new HashSet<>(jdbc.queryForList("SELECT slug FROM filter_attributes", String.class));
        List<String> created = new ArrayList<>();
        for (String name : names) {
            Map<String, Object> row = display.get(name);
            String nameBg = row != null ? (String) row.get("name_bg") : name;
            String nameEn = row != null ? (String) row.get("name_en") : null;
            String slug = uniqueSlug(nameBg, slugs);
            int inserted = jdbc.update("""
                    INSERT INTO filter_attributes (slug, name_bg, name_en, origin, auto_key)
                    VALUES (?, ?, ?, 'AUTO', ?)
                    ON CONFLICT (auto_key) DO NOTHING""", slug, nameBg, nameEn, name);
            if (inserted > 0) {
                slugs.add(slug);
                created.add(nameBg);
            }
        }

        // An AUTO attribute always has its name rule, also one created by an earlier run whose rule
        // was removed by hand.
        jdbc.update("""
                INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, origin, note)
                SELECT 'MAP', a.id, a.auto_key, 'AUTO', 'auto'
                FROM filter_attributes a
                WHERE a.origin = 'AUTO' AND a.auto_key IS NOT NULL
                  AND NOT EXISTS (SELECT 1 FROM filter_attribute_sources s
                                  WHERE s.name_norm = a.auto_key AND s.platform IS NULL AND s.category_id IS NULL)""");
        return created;
    }

    /**
     * "auto-" keeps generated slugs out of the curated namespace: a curated attribute can be called
     * "chipset" even when a supplier parameter named "Chipset" produced an AUTO attribute first.
     */
    private static String uniqueSlug(String name, Set<String> taken) {
        String base = SlugUtils.generateSlug(name);
        if (base.isEmpty()) {
            base = "attribute";
        }
        if (base.length() > 100) {
            base = base.substring(0, 100);
        }
        base = "auto-" + base;
        String slug = base;
        for (int i = 2; taken.contains(slug); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    // ── Step 4: raw option -> canonical value(s) ────────────────────────────────────────────────────

    /**
     * Resolution order per raw option: an EXACT rule on the whole text, then per split part an EXACT
     * rule, then REGEX rules (every match counts, so "black with blue" can be both colours), then an
     * automatic value if the attribute allows them. NUMERIC attributes go through their Java parser.
     * Whatever is left is reported, never silently dropped.
     */
    private int resolveOptionValues() {
        jdbc.execute("""
                CREATE TEMP TABLE tmp_whole ON COMMIT DROP AS
                SELECT DISTINCT o.option_id, o.attribute_id, a.value_type, a.split_pattern, a.auto_values,
                       po.name_bg AS raw_bg, po.name_en AS raw_en, filter_norm(po.name_bg) AS whole_norm
                FROM (
                    SELECT DISTINCT pp.parameter_option_id AS option_id, m.attribute_id
                    FROM product_parameters pp
                    JOIN products pr ON pr.id = pp.product_id
                    JOIN tmp_param_map m ON m.parameter_id = pp.parameter_id AND m.category_id = pr.category_id
                                        AND m.platform = COALESCE(pr.platform, '') AND m.action = 'MAP'
                    WHERE NOT pr.deleted
                ) o
                JOIN filter_attributes a ON a.id = o.attribute_id
                JOIN parameter_options po ON po.id = o.option_id""");
        jdbc.execute("ANALYZE tmp_whole");

        // value_id NULL = a rule that drops the text; the option still counts as handled.
        jdbc.execute("CREATE TEMP TABLE tmp_optval (option_id BIGINT, attribute_id BIGINT, value_id BIGINT) ON COMMIT DROP");
        jdbc.execute("CREATE TEMP TABLE tmp_unmapped (attribute_id BIGINT, raw_norm TEXT, raw TEXT, option_id BIGINT) ON COMMIT DROP");

        jdbc.update("""
                INSERT INTO tmp_optval (option_id, attribute_id, value_id)
                SELECT w.option_id, w.attribute_id, r.value_id
                FROM tmp_whole w
                JOIN filter_value_rules r ON r.attribute_id = w.attribute_id AND r.raw_norm = w.whole_norm""");

        resolveEnumParts();
        resolveNumericValues();

        jdbc.update("DELETE FROM filter_option_map");
        return jdbc.update("""
                INSERT INTO filter_option_map (parameter_option_id, attribute_id, value_id)
                SELECT DISTINCT option_id, attribute_id, value_id FROM tmp_optval WHERE value_id IS NOT NULL""");
    }

    private void resolveEnumParts() {
        jdbc.execute("""
                CREATE TEMP TABLE tmp_part ON COMMIT DROP AS
                SELECT w.option_id, w.attribute_id, w.auto_values,
                       left(btrim(regexp_replace(x.part, '[\\u200b-\\u200f\\ufeff]', '', 'g')), 500) AS part_raw,
                       filter_norm(x.part) AS part_norm, filter_value_key(x.part) AS part_key, FALSE AS matched
                FROM tmp_whole w
                CROSS JOIN LATERAL unnest(CASE WHEN w.split_pattern IS NULL THEN ARRAY[w.raw_bg]
                                               ELSE regexp_split_to_array(w.raw_bg, w.split_pattern) END) AS x(part)
                WHERE w.value_type = 'ENUM'
                  AND NOT EXISTS (SELECT 1 FROM tmp_optval t
                                  WHERE t.option_id = w.option_id AND t.attribute_id = w.attribute_id)""");
        jdbc.update("DELETE FROM tmp_part WHERE part_norm IS NULL OR part_key IS NULL OR length(part_norm) > 500");
        jdbc.execute("ANALYZE tmp_part");

        jdbc.update("""
                INSERT INTO tmp_optval (option_id, attribute_id, value_id)
                SELECT p.option_id, p.attribute_id, r.value_id
                FROM tmp_part p
                JOIN filter_value_rules r ON r.attribute_id = p.attribute_id AND r.raw_norm = p.part_norm""");
        jdbc.update("""
                UPDATE tmp_part p SET matched = TRUE
                WHERE EXISTS (SELECT 1 FROM filter_value_rules r
                              WHERE r.attribute_id = p.attribute_id AND r.raw_norm = p.part_norm)""");

        jdbc.update("""
                INSERT INTO tmp_optval (option_id, attribute_id, value_id)
                SELECT p.option_id, p.attribute_id, r.value_id
                FROM tmp_part p
                JOIN filter_value_rules r ON r.attribute_id = p.attribute_id AND r.pattern IS NOT NULL
                                         AND p.part_norm ~* r.pattern
                WHERE NOT p.matched""");
        jdbc.update("""
                UPDATE tmp_part p SET matched = TRUE
                WHERE NOT p.matched
                  AND EXISTS (SELECT 1 FROM filter_value_rules r
                              WHERE r.attribute_id = p.attribute_id AND r.pattern IS NOT NULL
                                AND p.part_norm ~* r.pattern)""");

        // Automatic values are keyed by filter_value_key, so spelling variants of one value ("16 GB",
        // "16GB") become one checkbox labelled with the most common spelling. The key is UNIQUE per
        // attribute, which keeps value ids — and filter URLs — stable across runs.
        jdbc.update("""
                INSERT INTO filter_values (attribute_id, value_bg, norm_key, origin)
                SELECT attribute_id, mode() WITHIN GROUP (ORDER BY part_raw), part_key, 'AUTO'
                FROM tmp_part
                WHERE NOT matched AND auto_values
                GROUP BY attribute_id, part_key
                ON CONFLICT (attribute_id, norm_key) DO NOTHING""");
        jdbc.update("""
                INSERT INTO tmp_optval (option_id, attribute_id, value_id)
                SELECT p.option_id, p.attribute_id, v.id
                FROM tmp_part p
                JOIN filter_values v ON v.attribute_id = p.attribute_id AND v.norm_key = p.part_key
                WHERE NOT p.matched AND p.auto_values""");

        jdbc.update("""
                INSERT INTO tmp_unmapped (attribute_id, raw_norm, raw, option_id)
                SELECT attribute_id, part_norm, part_raw, option_id FROM tmp_part
                WHERE NOT matched AND NOT auto_values""");
    }

    private void resolveNumericValues() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT w.option_id, w.attribute_id, a.parser, w.raw_bg, w.raw_en
                FROM tmp_whole w
                JOIN filter_attributes a ON a.id = w.attribute_id
                WHERE w.value_type = 'NUMERIC'
                  AND NOT EXISTS (SELECT 1 FROM tmp_optval t
                                  WHERE t.option_id = w.option_id AND t.attribute_id = w.attribute_id)""");
        if (rows.isEmpty()) {
            return;
        }

        List<Object[]> values = new ArrayList<>();
        List<Object[]> mapped = new ArrayList<>();
        List<Object[]> unmapped = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long optionId = ((Number) row.get("option_id")).longValue();
            long attributeId = ((Number) row.get("attribute_id")).longValue();
            String rawBg = (String) row.get("raw_bg");
            FilterValueParser parser = parsers.get((String) row.get("parser"));
            Optional<ParsedValue> parsed = parser == null ? Optional.empty()
                    : parser.parse(rawBg, (String) row.get("raw_en"));
            if (parsed.isPresent()) {
                ParsedValue v = parsed.get();
                values.add(new Object[]{attributeId, v.labelBg(), v.labelEn(), v.normKey(), v.number()});
                mapped.add(new Object[]{optionId, attributeId, v.normKey()});
            } else {
                unmapped.add(new Object[]{attributeId, rawBg, optionId});
            }
        }

        jdbc.batchUpdate("""
                INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, numeric_value, origin)
                VALUES (?, ?, ?, ?, ?, 'AUTO')
                ON CONFLICT (attribute_id, norm_key) DO NOTHING""", values);
        jdbc.execute("CREATE TEMP TABLE tmp_numeric (option_id BIGINT, attribute_id BIGINT, norm_key TEXT) ON COMMIT DROP");
        jdbc.batchUpdate("INSERT INTO tmp_numeric (option_id, attribute_id, norm_key) VALUES (?, ?, ?)", mapped);
        jdbc.update("""
                INSERT INTO tmp_optval (option_id, attribute_id, value_id)
                SELECT n.option_id, n.attribute_id, v.id
                FROM tmp_numeric n
                JOIN filter_values v ON v.attribute_id = n.attribute_id AND v.norm_key = n.norm_key""");
        jdbc.batchUpdate("""
                INSERT INTO tmp_unmapped (attribute_id, raw_norm, raw, option_id)
                VALUES (?, filter_norm(?), ?, ?)""",
                unmapped.stream().map(u -> new Object[]{u[0], u[1], u[1], u[2]}).toList());
    }

    // ── Step 5: product -> canonical values, applied as a diff ──────────────────────────────────────

    private Map<String, Object> syncProductFilterValues() {
        jdbc.execute("""
                CREATE TEMP TABLE tmp_pfv ON COMMIT DROP AS
                SELECT DISTINCT pp.product_id, fom.attribute_id, fom.value_id
                FROM product_parameters pp
                JOIN products pr ON pr.id = pp.product_id AND NOT pr.deleted
                JOIN tmp_param_map m ON m.parameter_id = pp.parameter_id AND m.category_id = pr.category_id
                                    AND m.platform = COALESCE(pr.platform, '') AND m.action = 'MAP'
                JOIN filter_option_map fom ON fom.parameter_option_id = pp.parameter_option_id
                                          AND fom.attribute_id = m.attribute_id""");
        jdbc.execute("CREATE INDEX ON tmp_pfv (product_id, attribute_id, value_id)");
        int nameDerived = addNameDerivedValues();
        jdbc.execute("ANALYZE tmp_pfv");

        int deleted = jdbc.update("""
                DELETE FROM product_filter_values f
                WHERE NOT EXISTS (SELECT 1 FROM tmp_pfv t
                                  WHERE t.product_id = f.product_id AND t.attribute_id = f.attribute_id
                                    AND t.value_id = f.value_id)""");
        int inserted = jdbc.update("""
                INSERT INTO product_filter_values (product_id, attribute_id, value_id)
                SELECT product_id, attribute_id, value_id FROM tmp_pfv
                ON CONFLICT DO NOTHING""");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("productValuesInserted", inserted);
        stats.put("productValuesDeleted", deleted);
        stats.put("nameDerivedValues", nameDerived);
        return stats;
    }

    /**
     * Values from the product name (V40) for products none of whose parameters gave the attribute a
     * value — a supplier that sends the socket always wins over the name. Regex rules run in SQL;
     * parser rules run the attribute's NUMERIC parser over the name.
     */
    private int addNameDerivedValues() {
        if (Boolean.FALSE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM filter_name_rules)", Boolean.class))) {
            return 0;
        }
        jdbc.execute("""
                CREATE TEMP TABLE tmp_param_attr ON COMMIT DROP AS
                SELECT DISTINCT product_id, attribute_id FROM tmp_pfv""");
        jdbc.execute("CREATE INDEX ON tmp_param_attr (product_id, attribute_id)");
        jdbc.execute("""
                CREATE TEMP TABLE tmp_names ON COMMIT DROP AS
                SELECT id AS product_id, category_id, name_bg, filter_norm(name_bg) AS name_norm
                FROM products
                WHERE NOT deleted AND category_id IS NOT NULL AND name_bg IS NOT NULL""");
        jdbc.execute("ANALYZE tmp_names");

        int added = jdbc.update("""
                INSERT INTO tmp_pfv (product_id, attribute_id, value_id)
                SELECT DISTINCT n.product_id, r.attribute_id, r.value_id
                FROM filter_name_rules r
                JOIN tmp_names n ON r.category_id IS NULL OR n.category_id = r.category_id
                WHERE NOT r.use_parser
                  AND n.name_norm ~* r.pattern
                  AND NOT EXISTS (SELECT 1 FROM tmp_param_attr t
                                  WHERE t.product_id = n.product_id AND t.attribute_id = r.attribute_id)""");

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT DISTINCT n.product_id, r.attribute_id, a.parser, n.name_bg
                FROM filter_name_rules r
                JOIN filter_attributes a ON a.id = r.attribute_id AND a.value_type = 'NUMERIC'
                JOIN tmp_names n ON r.category_id IS NULL OR n.category_id = r.category_id
                WHERE r.use_parser
                  AND NOT EXISTS (SELECT 1 FROM tmp_param_attr t
                                  WHERE t.product_id = n.product_id AND t.attribute_id = r.attribute_id)""");
        if (rows.isEmpty()) {
            return added;
        }
        List<Object[]> values = new ArrayList<>();
        List<Object[]> mapped = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            FilterValueParser parser = parsers.get((String) row.get("parser"));
            if (parser == null) {
                continue;
            }
            long attributeId = ((Number) row.get("attribute_id")).longValue();
            parser.parse((String) row.get("name_bg"), null).ifPresent(v -> {
                values.add(new Object[]{attributeId, v.labelBg(), v.labelEn(), v.normKey(), v.number()});
                mapped.add(new Object[]{((Number) row.get("product_id")).longValue(), attributeId, v.normKey()});
            });
        }
        jdbc.batchUpdate("""
                INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, numeric_value, origin)
                VALUES (?, ?, ?, ?, ?, 'AUTO')
                ON CONFLICT (attribute_id, norm_key) DO NOTHING""", values);
        jdbc.execute("CREATE TEMP TABLE tmp_name_numeric (product_id BIGINT, attribute_id BIGINT, norm_key TEXT) ON COMMIT DROP");
        jdbc.batchUpdate("INSERT INTO tmp_name_numeric (product_id, attribute_id, norm_key) VALUES (?, ?, ?)", mapped);
        return added + jdbc.update("""
                INSERT INTO tmp_pfv (product_id, attribute_id, value_id)
                SELECT n.product_id, n.attribute_id, v.id
                FROM tmp_name_numeric n
                JOIN filter_values v ON v.attribute_id = n.attribute_id AND v.norm_key = n.norm_key""");
    }

    // ── Step 6: AUTO filter groups per category ─────────────────────────────────────────────────────

    /**
     * Refills the AUTO rows of every category in AUTO mode with its best-covered attributes, up to the
     * category's free slots. MANUAL rows are never touched and win every conflict: an attribute with a
     * MANUAL row, or with the same display name as a visible MANUAL attribute, is not added again. An
     * AUTO group keeps its place while it stays above 80% of the threshold, so the panel does not
     * flicker from one night to the next.
     */
    private Map<String, Object> selectCategoryFilters() {
        jdbc.execute("""
                CREATE TEMP TABLE tmp_coverage ON COMMIT DROP AS
                SELECT v.category_id, f.attribute_id,
                       count(DISTINCT f.product_id) AS products, count(DISTINCT f.value_id) AS vals,
                       avg(length(fv.value_bg)) AS avg_label_length
                FROM tmp_vis v
                JOIN product_filter_values f ON f.product_id = v.id
                JOIN filter_values fv ON fv.id = f.value_id
                GROUP BY v.category_id, f.attribute_id""");
        jdbc.execute("""
                CREATE TEMP TABLE tmp_prev_auto ON COMMIT DROP AS
                SELECT category_id, attribute_id FROM category_filters WHERE origin = 'AUTO'""");
        jdbc.update("DELETE FROM category_filters WHERE origin = 'AUTO'");

        int inserted = named.update("""
                INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin, coverage)
                WITH candidates AS (
                    SELECT t.category_id, t.attribute_id, t.products, a.origin, a.sort_order, filter_norm(a.name_bg) AS name,
                           round(t.products::numeric / cs.n, 4) AS coverage,
                           COALESCE(st.max_groups,
                                    CASE WHEN cs.n < :smallCategoryProducts THEN :maxGroupsSmall ELSE :maxGroups END)
                             - (SELECT count(*) FROM category_filters m
                                WHERE m.category_id = t.category_id AND m.origin = 'MANUAL' AND m.visible) AS slots
                    FROM tmp_coverage t
                    JOIN (SELECT category_id, count(*) AS n FROM tmp_vis GROUP BY category_id) cs
                         ON cs.category_id = t.category_id
                    JOIN filter_attributes a ON a.id = t.attribute_id
                    JOIN categories c ON c.id = t.category_id AND c.alias_of_id IS NULL
                    LEFT JOIN category_filter_settings st ON st.category_id = t.category_id
                    WHERE COALESCE(st.mode, 'AUTO') = 'AUTO'
                      AND t.vals >= 2 AND t.products >= 2
                      AND (a.origin = 'MANUAL' OR (t.vals <= :maxValues AND t.avg_label_length <= :maxLabelLength))
                      AND (t.products >= COALESCE(st.min_coverage, :minCoverage) * cs.n
                           OR (t.products >= 0.8 * COALESCE(st.min_coverage, :minCoverage) * cs.n
                               AND EXISTS (SELECT 1 FROM tmp_prev_auto p
                                           WHERE p.category_id = t.category_id AND p.attribute_id = t.attribute_id)))
                      AND NOT EXISTS (SELECT 1 FROM category_filters m
                                      WHERE m.category_id = t.category_id AND m.attribute_id = t.attribute_id)
                      AND NOT EXISTS (SELECT 1 FROM category_filters m
                                      JOIN filter_attributes ma ON ma.id = m.attribute_id
                                      WHERE m.category_id = t.category_id AND m.visible
                                        AND filter_norm(ma.name_bg) = filter_norm(a.name_bg))
                ),
                -- Two attributes may share a display name (a curated "Диагонал" and an AUTO one created
                -- from a parameter called "Диагонал"); a category shows one of them.
                one_per_name AS (
                    SELECT DISTINCT ON (category_id, name) *
                    FROM candidates
                    ORDER BY category_id, name, (origin = 'MANUAL') DESC, products DESC, attribute_id
                ),
                ranked AS (
                    SELECT o.*, row_number() OVER (PARTITION BY o.category_id
                                                   ORDER BY o.products DESC, o.sort_order, o.attribute_id) AS rn
                    FROM one_per_name o
                )
                SELECT category_id, attribute_id, 1000 + rn, TRUE, 'AUTO', coverage
                FROM ranked
                WHERE rn <= slots""",
                new MapSqlParameterSource()
                        .addValue("maxGroups", maxGroups)
                        .addValue("smallCategoryProducts", smallCategoryProducts)
                        .addValue("maxGroupsSmall", maxGroupsSmall)
                        .addValue("maxValues", maxAutoValues)
                        .addValue("maxLabelLength", MAX_AUTO_LABEL_LENGTH)
                        .addValue("minCoverage", minCoverage));
        return Map.of("autoCategoryFilters", inserted);
    }

    // ── Step 7: tables the serving path and the admin report read ───────────────────────────────────

    private void publishDerivedTables() {
        jdbc.update("DELETE FROM filter_param_map");
        jdbc.update("""
                INSERT INTO filter_param_map (parameter_id, category_id, platform, action, attribute_id)
                SELECT parameter_id, category_id, platform, action, attribute_id FROM tmp_param_map""");

        jdbc.update("DELETE FROM filter_unmapped_values");
        jdbc.update("""
                INSERT INTO filter_unmapped_values (attribute_id, raw_norm, sample_text, option_count)
                SELECT attribute_id, raw_norm, min(raw), count(DISTINCT option_id)
                FROM tmp_unmapped
                WHERE raw_norm IS NOT NULL
                GROUP BY attribute_id, raw_norm""");
    }

    private Map<String, Object> summary() {
        Map<String, Object> stats = new LinkedHashMap<>(jdbc.queryForMap("""
                SELECT (SELECT count(*) FROM filter_attributes)                          AS attributes,
                       (SELECT count(*) FROM filter_attributes WHERE origin = 'MANUAL')   AS "manualAttributes",
                       (SELECT count(*) FROM filter_values)                              AS "values",
                       (SELECT count(*) FROM product_filter_values)                      AS "productValues",
                       (SELECT count(*) FROM filter_unmapped_values)                     AS "unmappedValues",
                       (SELECT count(*) FROM tmp_param_map WHERE action = 'MAP')          AS "mappedParameters",
                       (SELECT count(*) FROM tmp_combo)                                  AS "parameterCombos"
                """));

        // What a shopper sees: groups per category with visible products.
        stats.putAll(jdbc.queryForMap("""
                SELECT count(*)                         AS "categoriesWithFilters",
                       round(avg(groups), 1)             AS "avgGroupsPerCategory",
                       COALESCE(max(groups), 0)          AS "maxGroupsPerCategory"
                FROM (SELECT cf.category_id, count(*) AS groups
                      FROM category_filters cf
                      WHERE cf.visible AND cf.category_id IN (SELECT category_id FROM tmp_vis)
                      GROUP BY cf.category_id) g"""));
        stats.put("duplicateGroupNames", jdbc.queryForObject("""
                SELECT count(*) FROM (
                    SELECT cf.category_id
                    FROM category_filters cf JOIN filter_attributes a ON a.id = cf.attribute_id
                    WHERE cf.visible
                    GROUP BY cf.category_id, filter_norm(a.name_bg)
                    HAVING count(*) > 1) d""", Long.class));
        return stats;
    }

    private void recordRun(LocalDateTime started, String status, String trigger, boolean dryRun,
                           Map<String, Object> stats, String error) {
        jdbc.update("""
                INSERT INTO filter_rebuild_runs (started_at, finished_at, status, trigger, dry_run, stats, error)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?)""",
                Timestamp.valueOf(started), Timestamp.valueOf(LocalDateTime.now()), status, trigger, dryRun,
                toJson(stats), error);
    }

    private String toJson(Map<String, Object> stats) {
        if (stats == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
