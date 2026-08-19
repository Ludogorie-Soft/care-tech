-- ============================================================
-- Script 28: Видеонаблюдение — Одит и диагностика
-- Дата: 2026-08-19
-- Цел: Да открие пълното дърво на категориите за Видеонаблюдение
--       и да покаже разминаванията между нашия сайт и Текра
-- САМО READ операции — нищо не се променя
-- ============================================================

-- --------------------------------------------------------
-- СТЪПКА 1: Намери parent ID за "Видеонаблюдение"
-- --------------------------------------------------------
SELECT id, name_bg, name_en, parent_id, external_id, tekra_id, show, sort_order, platform
FROM categories
WHERE name_bg ILIKE '%видеонаблюдение%'
ORDER BY id;

-- --------------------------------------------------------
-- СТЪПКА 2: Пълно дърво (2 нива) на Видеонаблюдение
-- --------------------------------------------------------
WITH videosurv AS (
    SELECT id FROM categories WHERE name_bg ILIKE '%видеонаблюдение%' AND parent_id IS NULL
)
SELECT
    c1.id        AS parent_id,
    c1.name_bg   AS parent_name,
    c1.show      AS parent_show,
    c2.id        AS child_id,
    c2.name_bg   AS child_name,
    c2.show      AS child_show,
    c2.sort_order,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c2.id AND p.status = 'AVAILABLE' AND p.show = true) AS available_products
FROM categories c1
LEFT JOIN categories c2 ON c2.parent_id = c1.id
WHERE c1.parent_id = (SELECT id FROM videosurv)
   OR c1.id = (SELECT id FROM videosurv)
ORDER BY c1.sort_order, c2.sort_order;

-- --------------------------------------------------------
-- СТЪПКА 3: Всички категории директно под Видеонаблюдение (1 ниво)
-- --------------------------------------------------------
SELECT
    c.id,
    c.name_bg,
    c.name_en,
    c.show,
    c.sort_order,
    c.external_id,
    c.tekra_id,
    c.platform,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id) AS total_products,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.status = 'AVAILABLE') AS available_products,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.platform = 'TEKRA') AS tekra_products,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.platform = 'VALI') AS vali_products,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.platform = 'MOST') AS most_products,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.platform = 'ASBIS') AS asbis_products
FROM categories c
WHERE c.parent_id = (SELECT id FROM categories WHERE name_bg ILIKE '%видеонаблюдение%' AND parent_id IS NULL)
ORDER BY c.sort_order;

-- --------------------------------------------------------
-- СТЪПКА 4: Всички категории на 2-ро ниво (подкатегории на подкатегориите)
-- --------------------------------------------------------
SELECT
    parent.id        AS l1_id,
    parent.name_bg   AS l1_name,
    child.id         AS l2_id,
    child.name_bg    AS l2_name,
    child.show       AS l2_show,
    child.sort_order AS l2_sort,
    child.external_id,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = child.id) AS total,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = child.id AND p.status = 'AVAILABLE') AS available,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = child.id AND p.platform = 'TEKRA') AS tekra,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = child.id AND p.platform = 'VALI') AS vali,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = child.id AND p.platform = 'MOST') AS most
FROM categories parent
JOIN categories child ON child.parent_id = parent.id
WHERE parent.parent_id = (SELECT id FROM categories WHERE name_bg ILIKE '%видеонаблюдение%' AND parent_id IS NULL)
ORDER BY parent.sort_order, child.sort_order;

-- --------------------------------------------------------
-- СТЪПКА 5: Проблем 1 — MOST продукти в cat Адаптери (cat ~235)
--           (220 SSD/HDD попаднали грешно в категория Адаптери)
-- --------------------------------------------------------
SELECT
    c.id            AS cat_id,
    c.name_bg       AS cat_name,
    p.id            AS product_id,
    p.name_bg       AS product_name,
    p.status,
    p.platform,
    p.sku
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.id IN (
    -- Намери категорията с name_bg ILIKE '%адаптер%' под Видеонаблюдение
    SELECT c2.id FROM categories c2
    JOIN categories c1 ON c1.id = c2.parent_id
    WHERE c1.parent_id = (SELECT id FROM categories WHERE name_bg ILIKE '%видеонаблюдение%' AND parent_id IS NULL)
      AND c2.name_bg ILIKE '%адаптер%'
)
AND p.platform = 'MOST'
ORDER BY p.name_bg;

-- --------------------------------------------------------
-- СТЪПКА 6: Проблем 2 — MOST продукти в Surveillance Монитори (~249)
--           (298 gaming/office монитора попаднали в surveillance категория)
-- --------------------------------------------------------
SELECT
    c.id       AS cat_id,
    c.name_bg  AS cat_name,
    p.id       AS product_id,
    p.name_bg  AS product_name,
    p.status,
    p.platform,
    p.sku
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.id IN (
    SELECT c2.id FROM categories c2
    JOIN categories c1 ON c1.id = c2.parent_id
    WHERE c1.parent_id = (SELECT id FROM categories WHERE name_bg ILIKE '%видеонаблюдение%' AND parent_id IS NULL)
      AND (c2.name_bg ILIKE '%монитор%' OR c1.name_bg ILIKE '%монитор%')
)
AND p.platform = 'MOST'
ORDER BY p.name_bg
LIMIT 20;

-- --------------------------------------------------------
-- СТЪПКА 7: Проблем 3 — VALI consumer камери в IP камери (cat ~100)
--           (TP-Link Tapo, HAMA, Google Nest в проф. surveillance категория)
-- --------------------------------------------------------
SELECT
    c.id       AS cat_id,
    c.name_bg  AS cat_name,
    p.id       AS product_id,
    p.name_bg  AS product_name,
    p.status,
    p.platform
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE p.platform = 'VALI'
  AND c.id IN (
      -- Всички подкатегории под IP системи
      SELECT c2.id FROM categories c2
      JOIN categories c1 ON c1.id = c2.parent_id
      WHERE c1.parent_id = (SELECT id FROM categories WHERE name_bg ILIKE '%видеонаблюдение%' AND parent_id IS NULL)
        AND c1.name_bg ILIKE '%ip систем%'
  )
ORDER BY p.name_bg;

-- --------------------------------------------------------
-- СТЪПКА 8: Проблем 4 — IP системи, кои подкатегории съществуват vs. липсват
-- --------------------------------------------------------
SELECT
    c.id,
    c.name_bg,
    c.show,
    c.sort_order,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.status = 'AVAILABLE') AS available
FROM categories c
WHERE c.parent_id = (
    SELECT id FROM categories
    WHERE name_bg ILIKE '%ip систем%'
      AND parent_id = (SELECT id FROM categories WHERE name_bg ILIKE '%видеонаблюдение%' AND parent_id IS NULL)
)
ORDER BY c.sort_order;

-- --------------------------------------------------------
-- СТЪПКА 9: Намери cat_id за cat 233 (Мрежови + Сървъри mixed)
-- --------------------------------------------------------
SELECT
    c.id, c.name_bg, c.parent_id,
    parent.name_bg AS parent_name,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id) AS total,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.platform = 'TEKRA') AS tekra
FROM categories c
LEFT JOIN categories parent ON parent.id = c.parent_id
WHERE c.id IN (233, 238, 240, 241, 242, 244, 245, 246, 247, 249, 100, 231, 235, 236, 237)
ORDER BY c.id;

-- --------------------------------------------------------
-- СТЪПКА 10: Намери cat_id за Монитори и дисплеи (за преместване на MOST monitors)
-- --------------------------------------------------------
SELECT
    c.id, c.name_bg, c.parent_id,
    parent.name_bg AS parent_name,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id) AS total
FROM categories c
LEFT JOIN categories parent ON parent.id = c.parent_id
WHERE c.name_bg ILIKE '%монитор%'
   OR c.name_bg ILIKE '%дисплей%'
   OR c.name_bg ILIKE '%display%'
ORDER BY c.id;

-- --------------------------------------------------------
-- СТЪПКА 11: Summary за cat 238 — разпределение NVR vs DVR
-- --------------------------------------------------------
SELECT
    CASE
        WHEN p.name_bg ILIKE '%dvr%' OR p.name_en ILIKE '%dvr%' THEN 'DVR'
        WHEN p.name_bg ILIKE '%nvr%' OR p.name_en ILIKE '%nvr%' THEN 'NVR'
        ELSE 'UNKNOWN'
    END AS device_type,
    COUNT(*) AS total,
    COUNT(*) FILTER (WHERE p.status = 'AVAILABLE') AS available
FROM products p
WHERE p.category_id = 238
GROUP BY 1;
