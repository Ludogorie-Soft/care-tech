-- ============================================================
-- Script 17: Fix all visible products in hidden categories
-- Generated: 2026-05-26
-- Strategy:
--   Group 1 — Reassign to correct visible subcategory (1:1 match)
--   Group 2 — Reassign to closest visible category (by decision)
--   Group 3 — Hide products (household/irrelevant for this platform)
--   Special  — Make "Мултимедиен хардуер" (325) visible again
-- ============================================================

BEGIN;

-- ============================================================
-- PREVIEW: How many products will be affected per group
-- ============================================================

SELECT
    'Group 1+2 - Reassign to visible category' AS action,
    COUNT(*) AS product_count
FROM products
WHERE show_flag = true
  AND category_id IN (
    413, 513, 415, 322, 392, 375, 421, 440, 348, 362, 406, 301,
    333, 342, 398, 395, 360, 436, 269, 272, 275, 282, 293, 266,
    263, 258, 261, 259, 295, 404, 220, 218, 213, 212, 219,
    402, 321, 517, 443, 380, 298, 500, 489
  )
UNION ALL
SELECT
    'Group 3 - Hide (household/irrelevant)',
    COUNT(*)
FROM products
WHERE show_flag = true
  AND category_id IN (
    454, 466, 469, 455, 459, 463, 468, 472, 480,
    470, 462, 473, 471, 475, 478, 477, 476, 497
  );

-- ============================================================
-- GROUP 1: Clear 1:1 reassignments
-- ============================================================

-- Компютърни компоненти (parent=1)
UPDATE products SET category_id = 2,   updated_at = CURRENT_TIMESTAMP WHERE category_id = 413  AND show_flag = true; -- Дънни платки
UPDATE products SET category_id = 17,  updated_at = CURRENT_TIMESTAMP WHERE category_id = 513  AND show_flag = true; -- SSD
UPDATE products SET category_id = 6,   updated_at = CURRENT_TIMESTAMP WHERE category_id = 415  AND show_flag = true; -- Памети
UPDATE products SET category_id = 3,   updated_at = CURRENT_TIMESTAMP WHERE category_id = 322  AND show_flag = true; -- Процесори
UPDATE products SET category_id = 11,  updated_at = CURRENT_TIMESTAMP WHERE category_id = 392  AND show_flag = true; -- Кутии за компютри
UPDATE products SET category_id = 8,   updated_at = CURRENT_TIMESTAMP WHERE category_id = 436  AND show_flag = true; -- Видео карти

-- Компютри (parent=31)
UPDATE products SET category_id = 32,  updated_at = CURRENT_TIMESTAMP WHERE category_id IN (375, 421) AND show_flag = true; -- Настолни компютри
UPDATE products SET category_id = 37,  updated_at = CURRENT_TIMESTAMP WHERE category_id = 440  AND show_flag = true; -- Лаптопи

-- Компютърна периферия (parent=54)
UPDATE products SET category_id = 54,  updated_at = CURRENT_TIMESTAMP WHERE category_id IN (406, 301) AND show_flag = true; -- Компютърна периферия
UPDATE products SET category_id = 66,  updated_at = CURRENT_TIMESTAMP WHERE category_id = 266  AND show_flag = true; -- Слушалки
UPDATE products SET category_id = 120, updated_at = CURRENT_TIMESTAMP WHERE category_id = 263  AND show_flag = true; -- Преносими тонколони

-- Монитори (parent=49)
UPDATE products SET category_id = 50,  updated_at = CURRENT_TIMESTAMP WHERE category_id = 362  AND show_flag = true; -- Монитори

-- Рутери и мрежово оборудване (parent=99)
UPDATE products SET category_id = 99,  updated_at = CURRENT_TIMESTAMP WHERE category_id IN (333, 342) AND show_flag = true; -- Рутери и мрежово
UPDATE products SET category_id = 110, updated_at = CURRENT_TIMESTAMP WHERE category_id = 258  AND show_flag = true; -- Суичове - управляеми
UPDATE products SET category_id = 113, updated_at = CURRENT_TIMESTAMP WHERE category_id = 261  AND show_flag = true; -- Защитни стени

-- Кабели (parent=127)
UPDATE products SET category_id = 128, updated_at = CURRENT_TIMESTAMP WHERE category_id IN (398, 395) AND show_flag = true; -- Адаптери, конвертори

-- Сторидж
UPDATE products SET category_id = 71,  updated_at = CURRENT_TIMESTAMP WHERE category_id = 360  AND show_flag = true; -- Сторидж у-ва

-- Геймърска периферия (parent=167)
UPDATE products SET category_id = 175, updated_at = CURRENT_TIMESTAMP WHERE category_id IN (269, 272) AND show_flag = true; -- Волани и педали
UPDATE products SET category_id = 176, updated_at = CURRENT_TIMESTAMP WHERE category_id = 275  AND show_flag = true; -- Аксесоари за Волани
UPDATE products SET category_id = 179, updated_at = CURRENT_TIMESTAMP WHERE category_id IN (282, 293) AND show_flag = true; -- Геймърски аксесоари

-- Видеонаблюдение
UPDATE products SET category_id = 230, updated_at = CURRENT_TIMESTAMP WHERE category_id IN (259, 295) AND show_flag = true;

-- Фото и видео (parent=144)
UPDATE products SET category_id = 152, updated_at = CURRENT_TIMESTAMP WHERE category_id = 404  AND show_flag = true; -- Други фото аксесоари

-- Смарт часовници (parent=153)
UPDATE products SET category_id = 156, updated_at = CURRENT_TIMESTAMP WHERE category_id = 220  AND show_flag = true; -- Смарт часовници

-- Инструменти и аксесоари (211)
UPDATE products SET category_id = 211, updated_at = CURRENT_TIMESTAMP WHERE category_id IN (218, 213, 212, 219) AND show_flag = true;

-- Софтуер
UPDATE products SET category_id = 192, updated_at = CURRENT_TIMESTAMP WHERE category_id = 348  AND show_flag = true;

-- ============================================================
-- GROUP 2: Reassign by decision
-- ============================================================

-- Охладители (402) → Охладители за процесори (4)
UPDATE products SET category_id = 4,   updated_at = CURRENT_TIMESTAMP WHERE category_id = 402  AND show_flag = true;

-- Флаш памети (321, 517) → USB памети (57)
UPDATE products SET category_id = 57,  updated_at = CURRENT_TIMESTAMP WHERE category_id IN (321, 517) AND show_flag = true;

-- Игри и мултимедия (443) → Геймърска периферия (167)
UPDATE products SET category_id = 167, updated_at = CURRENT_TIMESTAMP WHERE category_id = 443  AND show_flag = true;

-- Захранване и кабели (380) → Кабели (127)
UPDATE products SET category_id = 127, updated_at = CURRENT_TIMESTAMP WHERE category_id = 380  AND show_flag = true;

-- Аксесоари (298) → Аксесоари за компютри (35)
UPDATE products SET category_id = 35,  updated_at = CURRENT_TIMESTAMP WHERE category_id = 298  AND show_flag = true;

-- Смарт устройства (500) → Смарт часовници, телефони и аксесоари (153)
UPDATE products SET category_id = 153, updated_at = CURRENT_TIMESTAMP WHERE category_id = 500  AND show_flag = true;

-- Аидио устройства (489) → Звукови системи и тонколони (59)
UPDATE products SET category_id = 59,  updated_at = CURRENT_TIMESTAMP WHERE category_id = 489  AND show_flag = true;

-- ============================================================
-- SPECIAL: Make "Мултимедиен хардуер" (325) visible
-- Products stay in their category — just restore show_flag
-- ============================================================

UPDATE categories
SET show_flag = true, updated_at = CURRENT_TIMESTAMP
WHERE id = 325;

-- ============================================================
-- GROUP 3: Hide household/irrelevant products
-- ============================================================

UPDATE products
SET show_flag  = false,
    status     = 'NOT_AVAILABLE',
    updated_at = CURRENT_TIMESTAMP
WHERE show_flag = true
  AND category_id IN (
    454, 466, 469, 455, 459, 463, 468, 472, 480,
    470, 462, 473, 471, 475, 478, 477, 476, 497
  );

-- ============================================================
-- VERIFICATION: Should return 0 rows for all fixed categories
-- ============================================================

SELECT
    c.id,
    c.name_bg        AS category_name,
    c.show_flag,
    COUNT(p.id)      AS remaining_visible_products
FROM categories c
JOIN products p ON p.category_id = c.id
WHERE c.show_flag = false
  AND p.show_flag = true
  AND c.id IN (
    413, 513, 415, 322, 392, 375, 421, 440, 348, 362, 406, 301,
    333, 342, 398, 395, 360, 436, 269, 272, 275, 282, 293, 266,
    263, 258, 261, 259, 295, 404, 220, 218, 213, 212, 219,
    402, 321, 517, 443, 380, 298, 500, 489,
    454, 466, 469, 455, 459, 463, 468, 472, 480,
    470, 462, 473, 471, 475, 478, 477, 476, 497
  )
GROUP BY c.id, c.name_bg, c.show_flag
ORDER BY remaining_visible_products DESC;

ROLLBACK; -- Replace with COMMIT after verifying 0 rows in the query above
