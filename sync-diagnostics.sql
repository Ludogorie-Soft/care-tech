-- =============================================================
-- SYNC DIAGNOSTICS — Tech Store
-- Пускай тези заявки ПРЕДИ и СЛЕД sync за да сравниш резултата.
-- =============================================================


-- -------------------------------------------------------------
-- 1. БАЗОВА ЛИНИЯ — продукти без характеристики (по платформа)
--    Очаквано след fix: значително намаляване на броя
-- -------------------------------------------------------------
SELECT p.platform,
       COUNT(*) AS products_with_zero_specs
FROM products p
WHERE NOT EXISTS (
    SELECT 1 FROM product_parameters pp WHERE pp.product_id = p.id
)
GROUP BY p.platform
ORDER BY p.platform;


-- -------------------------------------------------------------
-- 2. СРЕДНО ХАРАКТЕРИСТИКИ НА ПРОДУКТ (по платформа)
--    Очаквано след fix: avg_specs да нарасне към 8–15 за MOST/ASBIS
-- -------------------------------------------------------------
SELECT platform,
       ROUND(AVG(spec_count), 1) AS avg_specs,
       MIN(spec_count)           AS min_specs,
       MAX(spec_count)           AS max_specs,
       COUNT(*)                  AS total_products
FROM (
    SELECT p.id, p.platform, COUNT(pp.id) AS spec_count
    FROM products p
    LEFT JOIN product_parameters pp ON pp.product_id = p.id
    GROUP BY p.id, p.platform
) t
GROUP BY platform
ORDER BY platform;


-- -------------------------------------------------------------
-- 3. IS_FILTER СТАТУС — колко параметри са включени като филтри
--    Очаквано след fix: NULL записите да изчезнат, true да нараснат
-- -------------------------------------------------------------
SELECT is_filter, COUNT(*) AS cnt
FROM category_parameters
GROUP BY is_filter
ORDER BY is_filter NULLS LAST;


-- -------------------------------------------------------------
-- 4. ТОП 10 КАТЕГОРИИ с най-много характеристики (след sync)
--    Проверка дали правилните категории са обогатени
-- -------------------------------------------------------------
SELECT c.name_bg                 AS category,
       COUNT(DISTINCT cp.parameter_id) AS filterable_params,
       COUNT(DISTINCT pp.id)     AS total_product_specs
FROM categories c
JOIN category_parameters cp ON cp.category_id = c.id AND cp.is_filter = true
LEFT JOIN product_parameters pp ON pp.parameter_id = cp.parameter_id
LEFT JOIN products pr ON pr.id = pp.product_id AND pr.category_id = c.id
GROUP BY c.id, c.name_bg
ORDER BY filterable_params DESC
LIMIT 10;


-- -------------------------------------------------------------
-- 5. КОНКРЕТЕН ПРОДУКТ — характеристики по SKU
--    Замени 'ТВОЯТ-SKU' с реален SKU от MOST или ASBIS
-- -------------------------------------------------------------
SELECT param.name_bg  AS parameter,
       opt.name_bg    AS value,
       p.platform
FROM product_parameters pp
JOIN parameters        param ON param.id = pp.parameter_id
JOIN parameter_options opt   ON opt.id   = pp.parameter_option_id
JOIN products          p     ON p.id     = pp.product_id
WHERE p.sku = 'ТВОЯТ-SKU'
ORDER BY param.name_bg;


-- -------------------------------------------------------------
-- 6. ПРОВЕРКА НА JUNK MOST ПАРАМЕТРИ (трябва да са 0 след V33)
--    Ако върне редове — migration не е изпълнена
-- -------------------------------------------------------------
SELECT id, name_bg, most_key
FROM parameters
WHERE most_key IN (
    'name', 'price', 'category', 'partnumber', 'instock', 'currency',
    'manufacturer', 'code', 'partnum', 'in_stock', 'general_description',
    'gallery', 'main_picture_url', 'web_link'
);


-- -------------------------------------------------------------
-- 7. ПАРАМЕТРИ БЕЗ НИТО ЕДИН ПРОДУКТ (orphans)
--    Тези параметри съществуват но нищо не ги ползва
-- -------------------------------------------------------------
SELECT COUNT(*) AS orphan_parameters
FROM parameters param
WHERE NOT EXISTS (
    SELECT 1 FROM product_parameters pp WHERE pp.parameter_id = param.id
);


-- -------------------------------------------------------------
-- 8. MOST ПАРАМЕТРИ — колко опции имат (за оценка на качеството)
--    Параметри с 1 опция са безполезни; с >50 са вероятно free-text
-- -------------------------------------------------------------
SELECT
    CASE
        WHEN cnt = 0      THEN '0 (празни)'
        WHEN cnt = 1      THEN '1 (единична стойност)'
        WHEN cnt <= 5     THEN '2–5'
        WHEN cnt <= 20    THEN '6–20'
        WHEN cnt <= 50    THEN '21–50 (идеални за филтър)'
        ELSE                   '>50 (вероятно free-text)'
    END AS option_range,
    COUNT(*) AS parameter_count
FROM (
    SELECT po.parameter_id, COUNT(*) AS cnt
    FROM parameter_options po
    JOIN parameters p ON p.id = po.parameter_id AND p.most_key IS NOT NULL
    GROUP BY po.parameter_id
) t
GROUP BY option_range
ORDER BY MIN(cnt);
