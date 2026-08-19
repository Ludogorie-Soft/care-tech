-- ============================================================
-- Script 35: Премахване на tekra_slug от leaf (level-3) категории
-- Дата: 2026-08-19
--
-- ПРОБЛЕМ: Script 34 добави tekra_slug на всички подкатегории (level-3).
--          Tekra API-то обаче връща продукти САМО на ниво 2 (parent slug).
--          Всички level-3 subsubcat-и имат count=0 в Tekra categories API.
--          Синкът извиква getProductsRaw() за всяка категория с tekra_slug,
--          получава 0 продукта и кешира резултата → цялата Видеонаблюдение
--          секция остава без продукти.
--
-- РЕШЕНИЕ:
--   - Level 2 категории ПАЗЯТ tekra_slug → синкът ги фетчва
--   - Level 3 категории → tekra_slug = NULL, остава само category_path
--     (category_path се ползва от findMostSpecificCategory за routing)
--
-- Категории с tekra_slug след fix:
--   230 videonablyudenie, 231 hd-analogovi-sistemi, 232 ip-sistemi,
--   233 ndaa, 234 aksesoari, 235 hdd, 236 monitori-i-aksesoari,
--   968 softuer, 970 sistemi-za-prenos-na-danni-i-elektrozahranvane
-- ============================================================

BEGIN;

-- ============================================================
-- HD аналогови системи — level-3 подкатегории
-- ============================================================
UPDATE categories SET tekra_slug = NULL, updated_at = NOW()
WHERE id IN (
    237,  -- Аналогови камери (kameri)
    238   -- DVR (zapisvashti-ustroystva)
);

-- ============================================================
-- IP системи — level-3 подкатегории
-- ============================================================
UPDATE categories SET tekra_slug = NULL, updated_at = NOW()
WHERE id IN (
    100,  -- IP камери (kameri)
    962,  -- NVR (zapisvashti-ustroystva)
    963,  -- Мрежови устройства (mrezhovi-ustroystva)
    239   -- Сървъри (survuri)
);

-- ============================================================
-- NDAA — level-3 подкатегории
-- ============================================================
UPDATE categories SET tekra_slug = NULL, updated_at = NOW()
WHERE id IN (
    964,  -- IP камери (ip-kameri)
    240,  -- NVR (nvr)
    241,  -- Основи за монтаж (osnovi-za-montazh-na-kameri)
    242,  -- Комплект (komplekt)
    965,  -- Мрежово оборудване (mrezhovo)
    243   -- Сървър (survur)
);

-- ============================================================
-- Аксесоари — level-3 подкатегории
-- ============================================================
UPDATE categories SET tekra_slug = NULL, updated_at = NOW()
WHERE id IN (
    966,  -- Адаптери (adapteri)
    967,  -- Захранващи блокове (zahranvashti-blokove)
    244,  -- Конектори (konektori)
    245,  -- Видеобалуни (videobaluni)
    246,  -- Стойки и основи (stoyki-i-osnovi-za-kameri)
    247   -- Защити и изолатори (zashtiti-i-izolatori)
);

-- ============================================================
-- Монитори и аксесоари — level-3 подкатегории
-- ============================================================
UPDATE categories SET tekra_slug = NULL, updated_at = NOW()
WHERE id IN (
    249,  -- Монитори (monitori)
    52    -- Интерактивни дисплеи (interaktivni-displei)
);

-- ============================================================
-- Софтуер — level-3 подкатегории
-- ============================================================
UPDATE categories SET tekra_slug = NULL, updated_at = NOW()
WHERE id IN (
    969   -- DSSExpress (dssexpress)
);

-- ============================================================
-- Системи за пренос — level-3 подкатегории
-- ============================================================
UPDATE categories SET tekra_slug = NULL, updated_at = NOW()
WHERE id IN (
    971,  -- Аксесоари (aksesoari-253)
    972,  -- Захранване (zahranvane-254)
    973,  -- Суичове (suichove-255)
    974   -- Конвертори (konvertori)
);

COMMIT;

-- ============================================================
-- ФИНАЛНА ПРОВЕРКА — само level-2 трябва да имат tekra_slug
-- ============================================================
WITH RECURSIVE tree AS (
    SELECT id, name_bg, parent_id, sort_order, tekra_slug, category_path,
           0 AS depth,
           LPAD('', 0) || name_bg AS path_sort
    FROM categories WHERE id = 230
    UNION ALL
    SELECT c.id, c.name_bg, c.parent_id, c.sort_order,
           c.tekra_slug, c.category_path,
           t.depth + 1,
           t.path_sort || '/' || LPAD(c.sort_order::TEXT, 3, '0') || c.name_bg
    FROM categories c JOIN tree t ON t.id = c.parent_id
)
SELECT
    REPEAT('  ', depth) || t.name_bg                                   AS tree,
    t.id,
    t.tekra_slug,
    CASE
        WHEN depth <= 1 AND t.tekra_slug IS NULL THEN '⚠ MISSING SLUG'
        WHEN depth >= 2 AND t.tekra_slug IS NOT NULL THEN '⚠ SHOULD BE NULL'
        WHEN depth <= 1 AND t.tekra_slug IS NOT NULL THEN '✓ fetch'
        ELSE '✓ route-only'
    END AS status
FROM tree t
GROUP BY t.id, t.name_bg, t.depth, t.sort_order, t.path_sort, t.tekra_slug, t.category_path
ORDER BY t.path_sort;
