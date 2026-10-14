package com.techstore.service.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the filter index still needs from a human: raw parameters no rule covers, values a curated
 * attribute could not place, rules pointing at parameters that no longer exist, and how many groups
 * each category shows. Read-only.
 */
@Service
@RequiredArgsConstructor
public class FilterReportService {

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public Map<String, Object> report() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("lastRuns", jdbc.queryForList("""
                SELECT id, started_at, finished_at, status, trigger, dry_run, stats::text AS stats, error
                FROM filter_rebuild_runs ORDER BY id DESC LIMIT 5"""));
        report.put("unmappedParameters", unmappedParameters());
        report.put("unmappedValues", jdbc.queryForList("""
                SELECT a.id AS attribute_id, a.name_bg AS attribute, u.raw_norm, u.sample_text, u.option_count
                FROM filter_unmapped_values u JOIN filter_attributes a ON a.id = u.attribute_id
                ORDER BY u.option_count DESC, a.name_bg LIMIT 200"""));
        report.put("danglingRules", jdbc.queryForList("""
                SELECT s.id, s.action, s.parameter_id, s.platform, s.category_id, s.note
                FROM filter_attribute_sources s
                WHERE s.parameter_id IS NOT NULL
                  AND NOT EXISTS (SELECT 1 FROM parameters p WHERE p.id = s.parameter_id)"""));
        report.put("categories", jdbc.queryForList("""
                SELECT c.id AS category_id, c.name_bg AS category, COALESCE(st.mode, 'AUTO') AS mode,
                       count(*) FILTER (WHERE cf.visible) AS groups,
                       string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order, a.name_bg) FILTER (WHERE cf.visible) AS filters
                FROM category_filters cf
                JOIN categories c ON c.id = cf.category_id
                JOIN filter_attributes a ON a.id = cf.attribute_id
                LEFT JOIN category_filter_settings st ON st.category_id = c.id
                GROUP BY c.id, c.name_bg, st.mode
                ORDER BY groups DESC, c.name_bg"""));
        return report;
    }

    /** Raw parameter names on visible products that no rule maps, ignores or hides, by reach. */
    private List<Map<String, Object>> unmappedParameters() {
        return jdbc.queryForList("""
                SELECT filter_norm(p.name_bg) AS name,
                       string_agg(DISTINCT pr.platform, ',') AS platforms,
                       count(DISTINCT pr.id) AS products,
                       count(DISTINCT pr.category_id) AS categories,
                       count(DISTINCT filter_norm(po.name_bg)) AS distinct_values
                FROM product_parameters pp
                JOIN products pr ON pr.id = pp.product_id
                JOIN parameters p ON p.id = pp.parameter_id
                JOIN parameter_options po ON po.id = pp.parameter_option_id
                WHERE pr.active AND pr.show_flag AND pr.status = 'AVAILABLE' AND NOT pr.deleted
                  AND pr.image_url IS NOT NULL AND pr.image_url <> ''
                  AND NOT EXISTS (SELECT 1 FROM filter_param_map m
                                  WHERE m.parameter_id = pp.parameter_id AND m.category_id = pr.category_id
                                    AND m.platform = COALESCE(pr.platform, ''))
                GROUP BY filter_norm(p.name_bg)
                ORDER BY products DESC
                LIMIT 100""");
    }
}
