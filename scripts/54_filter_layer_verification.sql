-- ============================================================================
-- 54_filter_layer_verification.sql
--
-- САМО ЧЕТЕНЕ. Критериите за приемане на каноничния филтърен слой (V38).
-- Пуска се след деплой + rebuild и след всяка партида курация (55_*).
-- Всяка заявка казва в коментара какъв резултат се очаква.
-- ============================================================================

-- 1. Последните rebuild-ове. Очаквано: SUCCESS, durationMs под 60 000.
SELECT id, started_at, status, trigger, dry_run,
       stats ->> 'durationMs'            AS duration_ms,
       stats ->> 'avgGroupsPerCategory'  AS avg_groups,
       stats ->> 'maxGroupsPerCategory'  AS max_groups,
       stats ->> 'duplicateGroupNames'   AS duplicate_names,
       stats ->> 'unmappedValues'        AS unmapped_values,
       error
FROM filter_rebuild_runs ORDER BY id DESC LIMIT 5;

-- 2. Едно и също име два пъти в категория. Очаквано: 0 реда.
SELECT cf.category_id, filter_norm(a.name_bg) AS name, count(*)
FROM category_filters cf JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.visible
GROUP BY 1, 2 HAVING count(*) > 1;

-- 3. Групи на категория с видими продукти. Очаквано: максимум ≤ 12, средно ≤ 8.
WITH vis AS (
    SELECT id, category_id FROM products
    WHERE active AND show_flag AND status = 'AVAILABLE' AND NOT deleted
      AND image_url IS NOT NULL AND image_url <> '')
SELECT count(*) AS categories, round(avg(groups), 1) AS avg_groups, max(groups) AS max_groups
FROM (SELECT cf.category_id, count(*) AS groups
      FROM category_filters cf
      WHERE cf.visible AND cf.category_id IN (SELECT category_id FROM vis)
      GROUP BY cf.category_id) g;

-- 4. Покритие на всяка видима група (дял от видимите продукти в категорията,
--    които имат стойност). AUTO групите трябва да са ≥ 0.24 (0.30 с хистерезис 0.8);
--    ниско покритие при MANUAL група е сигнал за курацията.
WITH vis AS (
    SELECT id, category_id FROM products
    WHERE active AND show_flag AND status = 'AVAILABLE' AND NOT deleted
      AND image_url IS NOT NULL AND image_url <> '')
SELECT cf.category_id, c.name_bg AS category, a.name_bg AS filter, cf.origin,
       round(count(DISTINCT f.product_id)::numeric / NULLIF(count(DISTINCT v.id), 0), 2) AS coverage
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
JOIN vis v ON v.category_id = cf.category_id
LEFT JOIN product_filter_values f ON f.product_id = v.id AND f.attribute_id = cf.attribute_id
WHERE cf.visible
GROUP BY cf.category_id, c.name_bg, a.name_bg, cf.origin, cf.sort_order
ORDER BY coverage, cf.category_id
LIMIT 50;

-- 5. Опашката за курация: сурови параметри на видими продукти без правило, по обхват.
WITH vis AS (
    SELECT id, category_id, platform FROM products
    WHERE active AND show_flag AND status = 'AVAILABLE' AND NOT deleted
      AND image_url IS NOT NULL AND image_url <> '')
SELECT filter_norm(p.name_bg) AS name, string_agg(DISTINCT v.platform, ',') AS platforms,
       count(DISTINCT v.id) AS products, count(DISTINCT v.category_id) AS categories
FROM product_parameters pp
JOIN vis v ON v.id = pp.product_id
JOIN parameters p ON p.id = pp.parameter_id
WHERE NOT EXISTS (SELECT 1 FROM filter_param_map m
                  WHERE m.parameter_id = pp.parameter_id AND m.category_id = v.category_id
                    AND m.platform = COALESCE(v.platform, ''))
GROUP BY 1 ORDER BY products DESC LIMIT 50;

-- 6. Несъпоставени стойности на курирани свойства. Кандидати за нови правила.
SELECT a.name_bg AS attribute, u.sample_text, u.option_count
FROM filter_unmapped_values u JOIN filter_attributes a ON a.id = u.attribute_id
ORDER BY u.option_count DESC LIMIT 50;

-- 7. Правила към изчезнал параметър. Очаквано: 0 реда.
SELECT s.* FROM filter_attribute_sources s
WHERE s.parameter_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM parameters p WHERE p.id = s.parameter_id);

-- 8. VALI не трие параметри вече. Очаквано: „Stale (kept)“ в съобщението, никога „Deleted“.
SELECT created_at, status, error_message FROM sync_logs
WHERE sync_type = 'VALI_PARAMETERS' ORDER BY created_at DESC LIMIT 5;
