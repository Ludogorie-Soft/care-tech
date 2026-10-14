package com.techstore.service.filter;

import com.techstore.dto.response.DisplaySpecificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds the specification table shown to shoppers from the raw supplier values and the filter index.
 * <p>
 * Parameters mapped to one filter attribute are one row under the attribute's name, so a product that
 * carries "Цвят" from two supplier parameters shows a single "Цвят". Unmapped parameters are grouped by
 * normalized name. HIDE rules (packaging, EAN, URLs) remove a row. Values are de-duplicated by
 * filter_value_key, so "16 GB" and "16GB" show once. A value that is a list joined with {@code <br/>}
 * (ASBIS sends feature lists that way) becomes one line per item, and formatting tags such as
 * {@code <b>} are dropped: the page shows values as text. The raw {@code specifications} field is left
 * untouched because the admin product form edits parameters through it.
 */
@Service
@RequiredArgsConstructor
public class DisplaySpecificationService {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Map<Long, List<DisplaySpecificationDto>> forProducts(Collection<Long> productIds, String language) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        boolean english = "en".equalsIgnoreCase(language);

        // productId -> groupKey -> (name, values)
        Map<Long, Map<String, Row>> byProduct = new LinkedHashMap<>();
        namedParameterJdbcTemplate.query("""
                SELECT pp.product_id,
                       CASE WHEN m.action = 'MAP' THEN 'a:' || m.attribute_id
                            ELSE 'n:' || filter_norm(p.name_bg) END                               AS group_key,
                       CASE WHEN m.action = 'MAP' THEN a.name_bg
                            ELSE regexp_replace(btrim(p.name_bg), '[[:space:].,:;]+$', '') END     AS name_bg,
                       CASE WHEN m.action = 'MAP' THEN a.name_en ELSE p.name_en END               AS name_en,
                       btrim(po.name_bg)                                                         AS value_bg,
                       btrim(po.name_en)                                                         AS value_en,
                       filter_value_key(po.name_bg)                                              AS value_key
                FROM product_parameters pp
                JOIN products pr         ON pr.id = pp.product_id
                JOIN parameters p        ON p.id = pp.parameter_id
                JOIN parameter_options po ON po.id = pp.parameter_option_id
                LEFT JOIN filter_param_map m ON m.parameter_id = pp.parameter_id
                                            AND m.category_id = pr.category_id
                                            AND m.platform = COALESCE(pr.platform, '')
                LEFT JOIN filter_attributes a ON a.id = m.attribute_id
                WHERE pp.product_id IN (:ids)
                  AND COALESCE(m.action, '') <> 'HIDE'
                  AND filter_norm(p.name_bg) IS NOT NULL
                  AND filter_value_key(po.name_bg) IS NOT NULL
                ORDER BY pp.product_id,
                         CASE WHEN m.action = 'MAP' THEN 0 ELSE 1 END,
                         COALESCE(a.sort_order, 1000), COALESCE(p.sort_order, 1000), 3, 5""",
                new MapSqlParameterSource("ids", productIds),
                rs -> {
                    Map<String, Row> groups = byProduct.computeIfAbsent(rs.getLong("product_id"),
                            k -> new LinkedHashMap<>());
                    String nameEn = rs.getString("name_en");
                    String name = english && nameEn != null ? nameEn : rs.getString("name_bg");
                    Row row = groups.computeIfAbsent(rs.getString("group_key"), k -> new Row(name));
                    if (row.keys.add(rs.getString("value_key"))) {
                        String valueEn = rs.getString("value_en");
                        for (String line : displayLines(english && valueEn != null ? valueEn : rs.getString("value_bg"))) {
                            if (row.lines.add(line.toLowerCase(Locale.ROOT))) {
                                row.values.add(line);
                            }
                        }
                    }
                });

        Map<Long, List<DisplaySpecificationDto>> result = new HashMap<>();
        byProduct.forEach((productId, groups) -> result.put(productId, groups.values().stream()
                .map(r -> new DisplaySpecificationDto(r.name, List.copyOf(r.values)))
                .toList()));
        return result;
    }

    // "<br/>", "<br>" and the malformed "</br>" all separate lines in ASBIS values.
    private static final Pattern LINE_BREAK = Pattern.compile("(?i)</?br\\s*/?>");
    // Only formatting tags: a bare "<[^>]+>" would also eat text such as "<5 ms и >1 W".
    private static final Pattern FORMATTING_TAG =
            Pattern.compile("(?i)</?(b|strong|i|em|u|p|span|div|ul|ol|li|sup|sub|font|a|img)(\\s[^>]*)?/?>");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    /** One supplier value as the lines shown: split at {@code <br/>}, formatting tags removed. */
    static List<String> displayLines(String value) {
        if (value == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String part : LINE_BREAK.split(value)) {
            String line = SPACES.matcher(FORMATTING_TAG.matcher(part).replaceAll(" ")).replaceAll(" ").trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static final class Row {
        private final String name;
        private final Set<String> keys = new HashSet<>();
        private final Set<String> lines = new HashSet<>();
        private final List<String> values = new ArrayList<>();

        private Row(String name) {
            this.name = name;
        }
    }
}
