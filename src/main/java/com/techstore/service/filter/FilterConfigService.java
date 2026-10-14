package com.techstore.service.filter;

import com.techstore.dto.filter.FilterConfigDto.AttributeDetail;
import com.techstore.dto.filter.FilterConfigDto.AttributeRequest;
import com.techstore.dto.filter.FilterConfigDto.AttributeSummary;
import com.techstore.dto.filter.FilterConfigDto.CandidateAttribute;
import com.techstore.dto.filter.FilterConfigDto.CategoryConfig;
import com.techstore.dto.filter.FilterConfigDto.CategoryGroup;
import com.techstore.dto.filter.FilterConfigDto.CategoryRef;
import com.techstore.dto.filter.FilterConfigDto.CategorySummary;
import com.techstore.dto.filter.FilterConfigDto.NameRule;
import com.techstore.dto.filter.FilterConfigDto.NameRuleRequest;
import com.techstore.dto.filter.FilterConfigDto.PreviewRow;
import com.techstore.dto.filter.FilterConfigDto.RawParameter;
import com.techstore.dto.filter.FilterConfigDto.Source;
import com.techstore.dto.filter.FilterConfigDto.SourceRequest;
import com.techstore.dto.filter.FilterConfigDto.UnmappedValue;
import com.techstore.dto.filter.FilterConfigDto.Value;
import com.techstore.dto.filter.FilterConfigDto.ValueRequest;
import com.techstore.dto.filter.FilterConfigDto.ValueRule;
import com.techstore.dto.filter.FilterConfigDto.ValueRuleRequest;
import com.techstore.exception.ResourceNotFoundException;
import com.techstore.exception.ValidationException;
import com.techstore.service.CategoryAliasResolver;
import com.techstore.service.filter.parser.FilterValueParser;
import com.techstore.util.CyrillicTransliterator;
import com.techstore.util.SecurityHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Edits the canonical filter layer for the admin: what the 55_* scripts do, without SQL.
 * <p>
 * Attributes, values and rules take effect with the next rebuild (nightly, or the rebuild button);
 * the groups of a category are served straight from {@code category_filters} and change at once.
 * Everything the admin writes is MANUAL, so neither the rebuild nor the supplier syncs undo it, and
 * rules carry "admin &lt;email&gt;, &lt;date&gt;" in their note so their origin stays visible next to the
 * rules of the curation scripts.
 */
@Slf4j
@Service
public class FilterConfigService {

    private static final String VISIBLE = """
            p.active AND p.show_flag AND p.status = 'AVAILABLE' AND NOT p.deleted
            AND p.image_url IS NOT NULL AND p.image_url <> ''""";
    private static final Set<String> PLATFORMS = Set.of("VALI", "TEKRA", "ASBIS", "MOST");
    private static final int MAX_PATTERN_LENGTH = 500;

    private final NamedParameterJdbcTemplate named;
    private final JdbcTemplate jdbc;
    private final CategoryFilterService categoryFilterService;
    private final CategoryAliasResolver categoryAliasResolver;
    private final SecurityHelper securityHelper;
    private final Set<String> parserCodes;

    public FilterConfigService(JdbcTemplate jdbc, CategoryFilterService categoryFilterService,
                               CategoryAliasResolver categoryAliasResolver, SecurityHelper securityHelper,
                               List<FilterValueParser> parsers) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
        this.categoryFilterService = categoryFilterService;
        this.categoryAliasResolver = categoryAliasResolver;
        this.securityHelper = securityHelper;
        this.parserCodes = parsers.stream().map(FilterValueParser::code).collect(Collectors.toSet());
    }

    // ── Attributes ──────────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AttributeSummary> listAttributes(String query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = "";
        if (query != null && !query.isBlank()) {
            where = "WHERE a.name_bg ILIKE :q OR a.name_en ILIKE :q OR a.slug ILIKE :q";
            params.addValue("q", "%" + query.trim() + "%");
        }
        return named.query("""
                SELECT a.*,
                       (SELECT count(*) FROM category_filters cf WHERE cf.attribute_id = a.id AND cf.visible) AS categories,
                       (SELECT count(*) FROM filter_values v WHERE v.attribute_id = a.id) AS vals,
                       (SELECT count(DISTINCT f.product_id) FROM product_filter_values f
                        WHERE f.attribute_id = a.id) AS products
                FROM filter_attributes a
                %s
                ORDER BY (a.origin = 'MANUAL') DESC, categories DESC, a.name_bg
                LIMIT 300""".formatted(where), params, (rs, i) -> attributeSummary(rs));
    }

    @Transactional(readOnly = true)
    public AttributeDetail getAttribute(Long id) {
        AttributeSummary attribute = attributeSummary(id);
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);

        List<Source> sources = named.query("""
                SELECT s.*, c.name_bg AS category_name, a.name_bg AS attribute_name
                FROM filter_attribute_sources s
                LEFT JOIN categories c ON c.id = s.category_id
                LEFT JOIN filter_attributes a ON a.id = s.attribute_id
                WHERE s.attribute_id = :id
                ORDER BY s.origin DESC, s.name_norm, s.category_id NULLS FIRST""", params, (rs, i) -> source(rs));

        List<Value> values = named.query("""
                SELECT v.*, (SELECT count(*) FROM product_filter_values f WHERE f.value_id = v.id) AS products
                FROM filter_values v
                WHERE v.attribute_id = :id
                ORDER BY v.sort_order NULLS LAST, v.numeric_value NULLS LAST, v.value_bg""", params,
                (rs, i) -> new Value(rs.getLong("id"), rs.getString("value_bg"), rs.getString("value_en"),
                        (Integer) rs.getObject("sort_order"), rs.getString("origin"),
                        rs.getBigDecimal("numeric_value"), rs.getLong("products")));

        List<ValueRule> valueRules = named.query("""
                SELECT r.*, v.value_bg
                FROM filter_value_rules r
                LEFT JOIN filter_values v ON v.id = r.value_id
                WHERE r.attribute_id = :id
                ORDER BY v.sort_order NULLS LAST, v.value_bg, r.id""", params,
                (rs, i) -> new ValueRule(rs.getLong("id"), rs.getString("pattern"), rs.getString("raw_norm"),
                        (Long) rs.getObject("value_id"), rs.getString("value_bg"), rs.getString("note")));

        List<NameRule> nameRules = named.query("""
                SELECT r.*, c.name_bg AS category_name, v.value_bg
                FROM filter_name_rules r
                LEFT JOIN categories c ON c.id = r.category_id
                LEFT JOIN filter_values v ON v.id = r.value_id
                WHERE r.attribute_id = :id
                ORDER BY c.name_bg NULLS FIRST, v.value_bg, r.id""", params,
                (rs, i) -> new NameRule(rs.getLong("id"), (Long) rs.getObject("category_id"),
                        rs.getString("category_name"), rs.getString("pattern"), (Long) rs.getObject("value_id"),
                        rs.getString("value_bg"), rs.getBoolean("use_parser"), rs.getString("note")));

        List<CategoryRef> categories = named.query("""
                SELECT c.id, c.name_bg, cf.visible, cf.origin
                FROM category_filters cf JOIN categories c ON c.id = cf.category_id
                WHERE cf.attribute_id = :id
                ORDER BY cf.visible DESC, c.name_bg""", params,
                (rs, i) -> new CategoryRef(rs.getLong("id"), rs.getString("name_bg"), rs.getBoolean("visible"),
                        rs.getString("origin")));

        return new AttributeDetail(attribute, sources, values, valueRules, nameRules, categories);
    }

    @Transactional
    public AttributeSummary createAttribute(AttributeRequest request) {
        String nameBg = required(request.nameBg(), "Името на свойството е задължително.");
        String valueType = request.valueType() == null ? "ENUM" : request.valueType().trim().toUpperCase(Locale.ROOT);
        if (!valueType.equals("ENUM") && !valueType.equals("NUMERIC")) {
            throw new ValidationException("Видът трябва да е ENUM (избор от стойности) или NUMERIC (число).");
        }
        String parser = blankToNull(request.parser());
        if (valueType.equals("NUMERIC") && (parser == null || !parserCodes.contains(parser))) {
            throw new ValidationException("Числово свойство изисква парсер. Налични: " + String.join(", ", parserCodes));
        }
        if (parser != null && !parserCodes.contains(parser)) {
            throw new ValidationException("Няма парсер " + parser + ".");
        }
        String slug = request.slug() == null || request.slug().isBlank() ? freeSlug(nameBg) : request.slug().trim();
        if (!slug.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            throw new ValidationException("Slug може да съдържа само малки латински букви, цифри и тирета.");
        }
        try {
            Long id = named.queryForObject("""
                    INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values,
                                                   sort_order, origin)
                    VALUES (:slug, :nameBg, :nameEn, :valueType, :unit, :parser, :autoValues, :sortOrder, 'MANUAL')
                    RETURNING id""",
                    new MapSqlParameterSource()
                            .addValue("slug", slug)
                            .addValue("nameBg", nameBg)
                            .addValue("nameEn", blankToNull(request.nameEn()))
                            .addValue("valueType", valueType)
                            .addValue("unit", blankToNull(request.unit()))
                            .addValue("parser", parser)
                            .addValue("autoValues", Boolean.TRUE.equals(request.autoValues()))
                            .addValue("sortOrder", request.sortOrder() == null ? 100 : request.sortOrder()),
                    Long.class);
            log.info("Filter attribute {} ({}) created by {}", id, slug, actor());
            return attributeSummary(id);
        } catch (DuplicateKeyException e) {
            throw new ValidationException("Slug „" + slug + "“ вече е зает.");
        }
    }

    /**
     * Renames an attribute or changes its unit, order and automatic values. An automatic attribute
     * becomes curated: the rebuild never renames it back.
     */
    @Transactional
    public AttributeSummary updateAttribute(Long id, AttributeRequest request) {
        attributeSummary(id);
        named.update("""
                UPDATE filter_attributes
                SET name_bg = COALESCE(:nameBg, name_bg),
                    name_en = COALESCE(:nameEn, name_en),
                    unit = CASE WHEN :unitSet THEN CAST(:unit AS varchar) ELSE unit END,
                    auto_values = COALESCE(:autoValues, auto_values),
                    sort_order = COALESCE(:sortOrder, sort_order),
                    origin = 'MANUAL',
                    updated_at = now()
                WHERE id = :id""",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("nameBg", blankToNull(request.nameBg()))
                        .addValue("nameEn", blankToNull(request.nameEn()))
                        .addValue("unitSet", request.unit() != null)
                        .addValue("unit", blankToNull(request.unit()))
                        .addValue("autoValues", request.autoValues())
                        .addValue("sortOrder", request.sortOrder()));
        categoryFilterService.evictAll();
        return attributeSummary(id);
    }

    // ── Values ──────────────────────────────────────────────────────────────────────────────────────

    @Transactional
    public Value createValue(Long attributeId, ValueRequest request) {
        AttributeSummary attribute = attributeSummary(attributeId);
        if ("NUMERIC".equals(attribute.valueType())) {
            throw new ValidationException("Стойностите на числово свойство идват от парсера „" + attribute.parser()
                    + "“ — не се добавят на ръка.");
        }
        String valueBg = required(request.valueBg(), "Стойността е задължителна.");
        try {
            Long id = named.queryForObject("""
                    INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
                    VALUES (:attributeId, :valueBg, :valueEn, filter_norm(:valueBg), :sortOrder, 'MANUAL')
                    RETURNING id""",
                    new MapSqlParameterSource()
                            .addValue("attributeId", attributeId)
                            .addValue("valueBg", valueBg)
                            .addValue("valueEn", blankToNull(request.valueEn()))
                            .addValue("sortOrder", request.sortOrder()),
                    Long.class);
            return value(id);
        } catch (DuplicateKeyException e) {
            throw new ValidationException("Свойството вече има стойност „" + valueBg + "“.");
        }
    }

    /** Renames a value; its key (and so every filter link to it) stays the same. */
    @Transactional
    public Value updateValue(Long valueId, ValueRequest request) {
        value(valueId);
        named.update("""
                UPDATE filter_values
                SET value_bg = COALESCE(:valueBg, value_bg),
                    value_en = COALESCE(:valueEn, value_en),
                    sort_order = COALESCE(:sortOrder, sort_order),
                    origin = 'MANUAL'
                WHERE id = :id""",
                new MapSqlParameterSource()
                        .addValue("id", valueId)
                        .addValue("valueBg", blankToNull(request.valueBg()))
                        .addValue("valueEn", blankToNull(request.valueEn()))
                        .addValue("sortOrder", request.sortOrder()));
        categoryFilterService.evictAll();
        return value(valueId);
    }

    /** Deletes a value with its rules; products keep it until the next rebuild. */
    @Transactional
    public void deleteValue(Long valueId) {
        value(valueId);
        named.update("DELETE FROM filter_values WHERE id = :id", new MapSqlParameterSource("id", valueId));
        categoryFilterService.evictAll();
    }

    // ── Rules ───────────────────────────────────────────────────────────────────────────────────────

    /**
     * Maps supplier values onto a value: a regular expression over the normalized text, or one exact
     * supplier value. An exact rule without a value drops that supplier value from the filter.
     */
    @Transactional
    public ValueRule createValueRule(Long attributeId, ValueRuleRequest request) {
        attributeSummary(attributeId);
        String pattern = blankToNull(request.pattern());
        String raw = blankToNull(request.rawNorm());
        if ((pattern == null) == (raw == null)) {
            throw new ValidationException("Посочи или регулярен израз, или точна стойност на доставчика.");
        }
        if (pattern != null) {
            validatePattern(pattern);
            if (request.valueId() == null) {
                throw new ValidationException("Регулярният израз трябва да сочи към стойност.");
            }
        }
        if (request.valueId() != null) {
            requireValueOf(attributeId, request.valueId());
        }
        try {
            Long id = named.queryForObject("""
                    INSERT INTO filter_value_rules (attribute_id, pattern, raw_norm, value_id, note)
                    VALUES (:attributeId, :pattern, filter_norm(CAST(:raw AS text)), :valueId, :note)
                    RETURNING id""",
                    new MapSqlParameterSource()
                            .addValue("attributeId", attributeId)
                            .addValue("pattern", pattern)
                            .addValue("raw", raw)
                            .addValue("valueId", request.valueId())
                            .addValue("note", note()),
                    Long.class);
            return named.queryForObject("""
                    SELECT r.*, v.value_bg FROM filter_value_rules r LEFT JOIN filter_values v ON v.id = r.value_id
                    WHERE r.id = :id""", new MapSqlParameterSource("id", id),
                    (rs, i) -> new ValueRule(rs.getLong("id"), rs.getString("pattern"), rs.getString("raw_norm"),
                            (Long) rs.getObject("value_id"), rs.getString("value_bg"), rs.getString("note")));
        } catch (DuplicateKeyException e) {
            throw new ValidationException("Такова правило вече има.");
        }
    }

    @Transactional
    public void deleteValueRule(Long id) {
        deleteById("filter_value_rules", id, "Няма такова правило за стойност.");
    }

    /** Takes a value from the product name when the supplier parameters give the attribute nothing. */
    @Transactional
    public NameRule createNameRule(Long attributeId, NameRuleRequest request) {
        AttributeSummary attribute = attributeSummary(attributeId);
        Long categoryId = request.categoryId() == null ? null : requireCategory(request.categoryId());
        String pattern = blankToNull(request.pattern());
        if (request.useParser()) {
            if (attribute.parser() == null) {
                throw new ValidationException("Свойството няма парсер — използвай регулярен израз.");
            }
            pattern = null;
        } else {
            if (pattern == null || request.valueId() == null) {
                throw new ValidationException("Посочи регулярен израз и стойност.");
            }
            validatePattern(pattern);
            requireValueOf(attributeId, request.valueId());
        }
        try {
            Long id = named.queryForObject("""
                    INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, use_parser, note)
                    VALUES (:attributeId, :categoryId, :pattern, :valueId, :useParser, :note)
                    RETURNING id""",
                    new MapSqlParameterSource()
                            .addValue("attributeId", attributeId)
                            .addValue("categoryId", categoryId)
                            .addValue("pattern", pattern)
                            .addValue("valueId", request.useParser() ? null : request.valueId())
                            .addValue("useParser", request.useParser())
                            .addValue("note", note()),
                    Long.class);
            return named.queryForObject("""
                    SELECT r.*, c.name_bg AS category_name, v.value_bg
                    FROM filter_name_rules r
                    LEFT JOIN categories c ON c.id = r.category_id
                    LEFT JOIN filter_values v ON v.id = r.value_id
                    WHERE r.id = :id""", new MapSqlParameterSource("id", id),
                    (rs, i) -> new NameRule(rs.getLong("id"), (Long) rs.getObject("category_id"),
                            rs.getString("category_name"), rs.getString("pattern"), (Long) rs.getObject("value_id"),
                            rs.getString("value_bg"), rs.getBoolean("use_parser"), rs.getString("note")));
        } catch (DuplicateKeyException e) {
            throw new ValidationException("Такова правило вече има.");
        }
    }

    @Transactional
    public void deleteNameRule(Long id) {
        deleteById("filter_name_rules", id, "Няма такова правило за име.");
    }

    // ── Supplier parameters ─────────────────────────────────────────────────────────────────────────

    /**
     * Says what the index does with a supplier parameter name: map it onto an attribute, keep it for the
     * specifications only (IGNORE) or hide it everywhere (HIDE). A category or a supplier narrows the rule
     * and wins over a wider one.
     */
    @Transactional
    public Source createSource(SourceRequest request) {
        String action = request.action() == null ? "" : request.action().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("MAP", "IGNORE", "HIDE").contains(action)) {
            throw new ValidationException("Действието трябва да е MAP, IGNORE или HIDE.");
        }
        if (action.equals("MAP")) {
            if (request.attributeId() == null) {
                throw new ValidationException("Посочи свойството, към което отива параметърът.");
            }
            attributeSummary(request.attributeId());
        }
        String name = required(request.name(), "Посочи името на параметъра на доставчика.");
        Long categoryId = request.categoryId() == null ? null : requireCategory(request.categoryId());
        String platform = blankToNull(request.platform());
        if (platform != null) {
            platform = platform.toUpperCase(Locale.ROOT);
            if (!PLATFORMS.contains(platform)) {
                throw new ValidationException("Доставчикът трябва да е един от " + String.join(", ", PLATFORMS) + ".");
            }
        }
        try {
            Long id = named.queryForObject("""
                    INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, platform, origin, note)
                    VALUES (:action, :attributeId, filter_norm(:name), :categoryId, :platform, 'MANUAL', :note)
                    RETURNING id""",
                    new MapSqlParameterSource()
                            .addValue("action", action)
                            .addValue("attributeId", action.equals("MAP") ? request.attributeId() : null)
                            .addValue("name", name)
                            .addValue("categoryId", categoryId)
                            .addValue("platform", platform)
                            .addValue("note", note()),
                    Long.class);
            return source(id);
        } catch (DuplicateKeyException e) {
            throw new ValidationException("За този параметър вече има правило със същия обхват.");
        }
    }

    @Transactional(readOnly = true)
    public List<Source> sourcesFor(String name) {
        String required = required(name, "Посочи името на параметъра.");
        return named.query("""
                SELECT s.*, c.name_bg AS category_name, a.name_bg AS attribute_name
                FROM filter_attribute_sources s
                LEFT JOIN categories c ON c.id = s.category_id
                LEFT JOIN filter_attributes a ON a.id = s.attribute_id
                WHERE s.name_norm = filter_norm(:name)
                ORDER BY s.origin DESC, s.category_id NULLS FIRST, s.platform NULLS FIRST""",
                new MapSqlParameterSource("name", required), (rs, i) -> source(rs));
    }

    /** Only curated rules are deleted; an automatic one comes back with the next rebuild. */
    @Transactional
    public void deleteSource(Long id) {
        Source source = source(id);
        if (!"MANUAL".equals(source.origin())) {
            throw new ValidationException("Автоматично правило се създава наново при rebuild — добави IGNORE вместо да го триеш.");
        }
        deleteById("filter_attribute_sources", id, "Няма такова правило.");
    }

    /**
     * Supplier values of the attribute (as of the last rebuild) that a pattern would match, and the
     * values they get today — shown before a rule is saved.
     */
    @Transactional(readOnly = true)
    public List<PreviewRow> preview(Long attributeId, String pattern) {
        attributeSummary(attributeId);
        validatePattern(required(pattern, "Посочи регулярен израз."));
        return named.query("""
                WITH opts AS (
                    SELECT DISTINCT po.id, filter_norm(po.name_bg) AS norm, btrim(po.name_bg) AS text
                    FROM filter_param_map m
                    JOIN parameter_options po ON po.parameter_id = m.parameter_id
                    WHERE m.attribute_id = :attributeId AND m.action = 'MAP'
                ),
                grouped AS (
                    SELECT norm, min(text) AS text, array_agg(id) AS ids
                    FROM opts WHERE norm ~* :pattern GROUP BY norm
                )
                SELECT g.text,
                       (SELECT count(DISTINCT pp.product_id) FROM product_parameters pp
                        JOIN products p ON p.id = pp.product_id AND %s
                        WHERE pp.parameter_option_id = ANY (g.ids)) AS products,
                       (SELECT string_agg(DISTINCT fv.value_bg, ', ') FROM filter_option_map fom
                        JOIN filter_values fv ON fv.id = fom.value_id
                        WHERE fom.parameter_option_id = ANY (g.ids) AND fom.attribute_id = :attributeId) AS current_values
                FROM grouped g
                ORDER BY products DESC, g.text
                LIMIT 100""".formatted(VISIBLE),
                new MapSqlParameterSource().addValue("attributeId", attributeId).addValue("pattern", pattern),
                (rs, i) -> new PreviewRow(rs.getString("text"), rs.getLong("products"), rs.getString("current_values")));
    }

    // ── Work queues ─────────────────────────────────────────────────────────────────────────────────

    /** Supplier values that reached an attribute but match none of its values (as of the last rebuild). */
    @Transactional(readOnly = true)
    public List<UnmappedValue> unmapped(Long attributeId, Long categoryId, String query) {
        StringBuilder where = new StringBuilder("WHERE TRUE");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (attributeId != null) {
            where.append(" AND u.attribute_id = :attributeId");
            params.addValue("attributeId", attributeId);
        }
        if (categoryId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM category_filters cf WHERE cf.attribute_id = u.attribute_id"
                    + " AND cf.category_id = :categoryId AND cf.visible)");
            params.addValue("categoryId", categoryAliasResolver.resolve(categoryId));
        }
        if (query != null && !query.isBlank()) {
            where.append(" AND (u.raw_norm ILIKE :q OR a.name_bg ILIKE :q)");
            params.addValue("q", "%" + query.trim() + "%");
        }
        return named.query("""
                SELECT u.attribute_id, a.name_bg, u.raw_norm, u.sample_text, u.option_count
                FROM filter_unmapped_values u
                JOIN filter_attributes a ON a.id = u.attribute_id
                %s
                ORDER BY (a.origin = 'MANUAL') DESC, u.option_count DESC, u.raw_norm
                LIMIT 300""".formatted(where), params,
                (rs, i) -> new UnmappedValue(rs.getLong("attribute_id"), rs.getString("name_bg"),
                        rs.getString("raw_norm"), rs.getString("sample_text"), rs.getInt("option_count")));
    }

    /** Supplier parameter names on the visible products of a category, busiest first, with what they feed. */
    @Transactional(readOnly = true)
    public List<RawParameter> rawParameters(Long categoryId, String query) {
        Long resolved = requireCategory(categoryId);
        MapSqlParameterSource params = new MapSqlParameterSource("categoryId", resolved);
        String nameFilter = "";
        if (query != null && !query.isBlank()) {
            nameFilter = "AND filter_norm(pa.name_bg) ILIKE :q";
            params.addValue("q", "%" + query.trim() + "%");
        }
        return named.query("""
                WITH raw AS (
                    SELECT filter_norm(pa.name_bg) AS name, p.platform, pp.product_id, pp.parameter_id,
                           btrim(po.name_bg) AS val
                    FROM products p
                    JOIN product_parameters pp ON pp.product_id = p.id
                    JOIN parameters pa ON pa.id = pp.parameter_id
                    JOIN parameter_options po ON po.id = pp.parameter_option_id
                    WHERE p.category_id = :categoryId AND %s AND filter_norm(pa.name_bg) IS NOT NULL %s
                ),
                grouped AS (
                    SELECT name, string_agg(DISTINCT platform, ', ') AS platforms,
                           count(DISTINCT product_id) AS products,
                           left(string_agg(DISTINCT val, ' | '), 300) AS samples,
                           array_agg(DISTINCT parameter_id) AS parameter_ids
                    FROM raw GROUP BY name
                )
                SELECT g.*, m.action, m.attribute_id, a.name_bg AS attribute_name
                FROM grouped g
                LEFT JOIN LATERAL (
                    SELECT fm.action, fm.attribute_id FROM filter_param_map fm
                    WHERE fm.category_id = :categoryId AND fm.parameter_id = ANY (g.parameter_ids)
                    ORDER BY (fm.action = 'MAP') DESC, fm.attribute_id
                    LIMIT 1
                ) m ON TRUE
                LEFT JOIN filter_attributes a ON a.id = m.attribute_id
                ORDER BY g.products DESC, g.name
                LIMIT 300""".formatted(VISIBLE, nameFilter), params,
                (rs, i) -> new RawParameter(rs.getString("name"), rs.getString("platforms"), rs.getLong("products"),
                        rs.getString("samples"), rs.getString("action"), (Long) rs.getObject("attribute_id"),
                        rs.getString("attribute_name")));
    }

    // ── Categories ──────────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategorySummary> categories(String query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String nameFilter = "";
        if (query != null && !query.isBlank()) {
            nameFilter = "AND c.name_bg ILIKE :q";
            params.addValue("q", "%" + query.trim() + "%");
        }
        return named.query("""
                WITH vis AS (SELECT p.category_id, count(*) AS n FROM products p WHERE %s GROUP BY p.category_id)
                SELECT c.id, c.name_bg, COALESCE(v.n, 0) AS products, COALESCE(s.mode, 'AUTO') AS mode,
                       (SELECT count(*) FROM category_filters cf WHERE cf.category_id = c.id AND cf.visible) AS groups
                FROM categories c
                LEFT JOIN vis v ON v.category_id = c.id
                LEFT JOIN category_filter_settings s ON s.category_id = c.id
                WHERE c.alias_of_id IS NULL AND (v.n > 0 OR s.mode IS NOT NULL) %s
                ORDER BY products DESC, c.name_bg
                LIMIT 500""".formatted(VISIBLE, nameFilter), params,
                (rs, i) -> new CategorySummary(rs.getLong("id"), rs.getString("name_bg"), rs.getLong("products"),
                        rs.getString("mode"), rs.getInt("groups")));
    }

    @Transactional(readOnly = true)
    public CategoryConfig category(Long categoryId) {
        Long id = requireCategory(categoryId);
        MapSqlParameterSource params = new MapSqlParameterSource("categoryId", id);
        String coverage = """
                WITH vis AS (SELECT p.id FROM products p WHERE p.category_id = :categoryId AND %s),
                cov AS (SELECT f.attribute_id, count(DISTINCT f.product_id) AS products, count(DISTINCT f.value_id) AS vals
                        FROM product_filter_values f JOIN vis ON vis.id = f.product_id
                        GROUP BY f.attribute_id)
                """.formatted(VISIBLE);

        record Head(String name, String mode, long products) {
        }
        Head head = named.queryForObject("""
                SELECT c.name_bg, COALESCE(s.mode, 'AUTO') AS mode,
                       (SELECT count(*) FROM products p WHERE p.category_id = c.id AND %s) AS products
                FROM categories c LEFT JOIN category_filter_settings s ON s.category_id = c.id
                WHERE c.id = :categoryId""".formatted(VISIBLE), params,
                (rs, i) -> new Head(rs.getString("name_bg"), rs.getString("mode"), rs.getLong("products")));

        List<CategoryGroup> groups = named.query(coverage + """
                SELECT cf.attribute_id, a.name_bg, a.slug, cf.origin, cf.visible, cf.sort_order, cf.coverage,
                       COALESCE(cov.products, 0) AS products, COALESCE(cov.vals, 0) AS vals
                FROM category_filters cf
                JOIN filter_attributes a ON a.id = cf.attribute_id
                LEFT JOIN cov ON cov.attribute_id = cf.attribute_id
                WHERE cf.category_id = :categoryId
                ORDER BY cf.sort_order, a.name_bg""", params,
                (rs, i) -> new CategoryGroup(rs.getLong("attribute_id"), rs.getString("name_bg"), rs.getString("slug"),
                        rs.getString("origin"), rs.getBoolean("visible"), rs.getInt("sort_order"),
                        rs.getBigDecimal("coverage"), rs.getLong("products"), rs.getInt("vals")));

        List<CandidateAttribute> candidates = named.query(coverage + """
                SELECT cov.attribute_id, a.name_bg, a.origin, cov.products, cov.vals
                FROM cov JOIN filter_attributes a ON a.id = cov.attribute_id
                WHERE NOT EXISTS (SELECT 1 FROM category_filters cf
                                  WHERE cf.category_id = :categoryId AND cf.attribute_id = cov.attribute_id)
                ORDER BY cov.products DESC, a.name_bg
                LIMIT 60""", params,
                (rs, i) -> new CandidateAttribute(rs.getLong("attribute_id"), rs.getString("name_bg"),
                        rs.getString("origin"), rs.getLong("products"), rs.getInt("vals")));

        return new CategoryConfig(id, head.name(), head.mode(), head.products(), groups, candidates);
    }

    /**
     * MANUAL keeps the groups the category shows today and stops the rebuild from adding any; AUTO lets
     * the rebuild fill the free slots again next to the curated groups.
     */
    @Transactional
    public CategoryConfig setMode(Long categoryId, String mode) {
        Long id = requireCategory(categoryId);
        String value = mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT);
        if (!value.equals("AUTO") && !value.equals("MANUAL")) {
            throw new ValidationException("Режимът трябва да е AUTO или MANUAL.");
        }
        MapSqlParameterSource params = new MapSqlParameterSource("categoryId", id).addValue("mode", value);
        if (value.equals("MANUAL")) {
            named.update("UPDATE category_filters SET origin = 'MANUAL' WHERE category_id = :categoryId AND origin = 'AUTO'",
                    params);
        }
        named.update("""
                INSERT INTO category_filter_settings (category_id, mode) VALUES (:categoryId, :mode)
                ON CONFLICT (category_id) DO UPDATE SET mode = EXCLUDED.mode""", params);
        categoryFilterService.evict(id);
        return category(id);
    }

    @Transactional
    public CategoryConfig addGroup(Long categoryId, Long attributeId) {
        Long id = requireCategory(categoryId);
        attributeSummary(attributeId);
        named.update("""
                INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
                VALUES (:categoryId, :attributeId,
                        COALESCE((SELECT max(sort_order) FROM category_filters
                                  WHERE category_id = :categoryId AND sort_order < 1000), 0) + 10,
                        TRUE, 'MANUAL')
                ON CONFLICT (category_id, attribute_id) DO UPDATE SET visible = TRUE, origin = 'MANUAL'""",
                new MapSqlParameterSource("categoryId", id).addValue("attributeId", attributeId));
        categoryFilterService.evict(id);
        return category(id);
    }

    /**
     * Removes a group. In AUTO mode the group is switched off instead, or the rebuild would add it back.
     */
    @Transactional
    public CategoryConfig removeGroup(Long categoryId, Long attributeId) {
        Long id = requireCategory(categoryId);
        MapSqlParameterSource params = new MapSqlParameterSource("categoryId", id).addValue("attributeId", attributeId);
        String mode = named.queryForObject("""
                SELECT COALESCE((SELECT mode FROM category_filter_settings WHERE category_id = :categoryId), 'AUTO')""",
                params, String.class);
        if ("MANUAL".equals(mode)) {
            named.update("DELETE FROM category_filters WHERE category_id = :categoryId AND attribute_id = :attributeId",
                    params);
        } else {
            named.update("""
                    UPDATE category_filters SET visible = FALSE, origin = 'MANUAL'
                    WHERE category_id = :categoryId AND attribute_id = :attributeId""", params);
        }
        categoryFilterService.evict(id);
        return category(id);
    }

    /** Orders the listed groups 10, 20, 30…; a reordered automatic group becomes curated. */
    @Transactional
    public CategoryConfig reorder(Long categoryId, List<Long> attributeIds) {
        Long id = requireCategory(categoryId);
        if (attributeIds == null || attributeIds.isEmpty()) {
            throw new ValidationException("Посочи подредбата на групите.");
        }
        int order = 10;
        for (Long attributeId : attributeIds) {
            named.update("""
                    UPDATE category_filters SET sort_order = :order, origin = 'MANUAL'
                    WHERE category_id = :categoryId AND attribute_id = :attributeId""",
                    new MapSqlParameterSource("categoryId", id).addValue("attributeId", attributeId)
                            .addValue("order", order));
            order += 10;
        }
        categoryFilterService.evict(id);
        return category(id);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * Compiles the pattern with the engine the rebuild uses: one invalid expression would fail every
     * rebuild, the nightly one included.
     */
    void validatePattern(String pattern) {
        if (pattern.length() > MAX_PATTERN_LENGTH) {
            throw new ValidationException("Регулярният израз е по-дълъг от " + MAX_PATTERN_LENGTH + " знака.");
        }
        try {
            jdbc.queryForObject("SELECT '' ~* ?", Boolean.class, pattern);
        } catch (DataAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            String message = cause.getMessage() == null ? "" : cause.getMessage().replaceFirst("(?s)^ERROR:\\s*", "")
                    .replaceFirst("(?s)\\n.*", "");
            throw new ValidationException("Невалиден регулярен израз: " + message);
        }
    }

    private AttributeSummary attributeSummary(Long id) {
        List<AttributeSummary> found = named.query("""
                SELECT a.*,
                       (SELECT count(*) FROM category_filters cf WHERE cf.attribute_id = a.id AND cf.visible) AS categories,
                       (SELECT count(*) FROM filter_values v WHERE v.attribute_id = a.id) AS vals,
                       (SELECT count(DISTINCT f.product_id) FROM product_filter_values f
                        WHERE f.attribute_id = a.id) AS products
                FROM filter_attributes a WHERE a.id = :id""", new MapSqlParameterSource("id", id),
                (rs, i) -> attributeSummary(rs));
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Няма свойство " + id + ".");
        }
        return found.get(0);
    }

    private static AttributeSummary attributeSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AttributeSummary(rs.getLong("id"), rs.getString("slug"), rs.getString("name_bg"),
                rs.getString("name_en"), rs.getString("value_type"), rs.getString("unit"), rs.getString("parser"),
                rs.getString("origin"), rs.getBoolean("auto_values"), rs.getInt("categories"), rs.getInt("vals"),
                rs.getLong("products"));
    }

    private Value value(Long id) {
        List<Value> found = named.query("""
                SELECT v.*, (SELECT count(*) FROM product_filter_values f WHERE f.value_id = v.id) AS products
                FROM filter_values v WHERE v.id = :id""", new MapSqlParameterSource("id", id),
                (rs, i) -> new Value(rs.getLong("id"), rs.getString("value_bg"), rs.getString("value_en"),
                        (Integer) rs.getObject("sort_order"), rs.getString("origin"),
                        rs.getBigDecimal("numeric_value"), rs.getLong("products")));
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Няма стойност " + id + ".");
        }
        return found.get(0);
    }

    private Source source(Long id) {
        List<Source> found = named.query("""
                SELECT s.*, c.name_bg AS category_name, a.name_bg AS attribute_name
                FROM filter_attribute_sources s
                LEFT JOIN categories c ON c.id = s.category_id
                LEFT JOIN filter_attributes a ON a.id = s.attribute_id
                WHERE s.id = :id""", new MapSqlParameterSource("id", id), (rs, i) -> source(rs));
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Няма правило " + id + ".");
        }
        return found.get(0);
    }

    private static Source source(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Source(rs.getLong("id"), rs.getString("action"), rs.getString("name_norm"),
                (Long) rs.getObject("parameter_id"), rs.getString("platform"), (Long) rs.getObject("category_id"),
                rs.getString("category_name"), (Long) rs.getObject("attribute_id"), rs.getString("attribute_name"),
                rs.getString("origin"), rs.getString("note"));
    }

    private void requireValueOf(Long attributeId, Long valueId) {
        Integer n = named.queryForObject("SELECT count(*) FROM filter_values WHERE id = :valueId AND attribute_id = :attributeId",
                new MapSqlParameterSource("valueId", valueId).addValue("attributeId", attributeId), Integer.class);
        if (n == null || n == 0) {
            throw new ValidationException("Стойността не е на това свойство.");
        }
    }

    /** @return the category id to store: an alias resolves to the category it stands for */
    private Long requireCategory(Long categoryId) {
        Long resolved = categoryAliasResolver.resolve(categoryId);
        Integer n = named.queryForObject("SELECT count(*) FROM categories WHERE id = :id",
                new MapSqlParameterSource("id", resolved), Integer.class);
        if (n == null || n == 0) {
            throw new ResourceNotFoundException("Няма категория " + categoryId + ".");
        }
        return resolved;
    }

    private void deleteById(String table, Long id, String missing) {
        int deleted = named.update("DELETE FROM " + table + " WHERE id = :id", new MapSqlParameterSource("id", id));
        if (deleted == 0) {
            throw new ResourceNotFoundException(missing);
        }
    }

    private String freeSlug(String nameBg) {
        String base = CyrillicTransliterator.transliterate(nameBg).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (base.isEmpty()) {
            base = "attribute";
        }
        String slug = base;
        for (int n = 2; exists("SELECT count(*) FROM filter_attributes WHERE slug = :slug", slug); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }

    private boolean exists(String sql, String slug) {
        Integer n = named.queryForObject(sql, new MapSqlParameterSource("slug", slug), Integer.class);
        return n != null && n > 0;
    }

    private String note() {
        String note = "admin " + actor() + ", " + LocalDate.now();
        return note.length() > 255 ? note.substring(0, 255) : note;
    }

    private String actor() {
        try {
            String email = securityHelper.getCurrentUserEmail();
            return email == null ? "unknown" : email;
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    private static String required(String value, String message) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            throw new ValidationException(message);
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
