-- ============================================================
-- Script 34: Поправка на tekra_slug и category_path след верификация с Tekra API
-- Дата: 2026-08-19
--
-- ПРОБЛЕМ: Script 33 използва грешни tekra_slug стойности за 3 категории
--          и не е сетнал tekra_slug/category_path за 7 съществуващи категории.
--          Верните стойности са потвърдени от GET /api/sync/tekra/tekra-categories.
--
-- ГРЕШНИ tekra_slug (script 33):
--   cat 235: 'tvrdi-diskove'       → 'hdd'
--   cat 965: 'mrezhovo-oborudvane' → 'mrezhovo'
--   cat 971: 'aksesoari'           → 'aksesoari-253'
--
-- ЛИПСВАЩИ tekra_slug + category_path (script 33 не ги е сетнал):
--   cat 237 (Аналогови камери)   → kameri / videonablyudenie/hd-analogovi-sistemi/kameri
--   cat 238 (DVR)                → zapisvashti-ustroystva / videonablyudenie/hd-analogovi-sistemi/...
--   cat 239 (Сървъри IP)         → survuri / videonablyudenie/ip-sistemi/survuri
--   cat 240 (NVR NDAA)           → nvr / videonablyudenie/ndaa/nvr
--   cat 241 (Основи за монтаж)   → osnovi-za-montazh-na-kameri / ...
--   cat 242 (Комплект NDAA)      → komplekt / videonablyudenie/ndaa/komplekt
--   cat 243 (Сървър NDAA)        → survur / videonablyudenie/ndaa/survur
--
-- ЛИПСВАЩИ tekra_slug (path беше сетнат, slug — не):
--   cat 244 (Конектори), 245 (Видеобалуни), 246 (Стойки), 247 (Защити)
--   cat 249 (Монитори), cat 52 (Интерактивни дисплеи)
--
-- ЛИПСВАЩИ нови категории под Системи за пренос (parent=970):
--   zahranvane-254, suichove-255, konvertori
-- ============================================================

BEGIN;

-- ============================================================
-- А: ПОПРАВКА НА ГРЕШНИТЕ tekra_slug
-- ============================================================

-- cat 235 (Твърди дискове): Tekra slug е 'hdd', не 'tvrdi-diskove'
UPDATE categories
SET tekra_slug    = 'hdd',
    category_path = 'videonablyudenie/hdd',
    updated_at    = NOW()
WHERE id = 235;

-- cat 965 (Мрежово оборудване под NDAA): Tekra slug е 'mrezhovo', не 'mrezhovo-oborudvane'
-- 'mrezhovo-oborudvane' е slug на ГЛАВНАТА категория Мрежово оборудване (Tekra id 63)
UPDATE categories
SET tekra_slug    = 'mrezhovo',
    category_path = 'videonablyudenie/ndaa/mrezhovo',
    updated_at    = NOW()
WHERE id = 965;

-- cat 971 (Аксесоари под Системи за пренос): Tekra slug е 'aksesoari-253', не 'aksesoari'
-- 'aksesoari' е slug на Аксесоари под Видеонаблюдение (cat 234)
UPDATE categories
SET tekra_slug    = 'aksesoari-253',
    category_path = 'videonablyudenie/sistemi-za-prenos-na-danni-i-elektrozahranvane/aksesoari-253',
    updated_at    = NOW()
WHERE id = 971;

-- ============================================================
-- Б: ЛИПСВАЩИ tekra_slug + category_path за съществуващи категории
--    Script 33 ги преименува, но не им сетна slug/path
-- ============================================================

-- cat 237 (Аналогови камери под HD аналогови системи)
UPDATE categories
SET tekra_slug    = 'kameri',
    category_path = 'videonablyudenie/hd-analogovi-sistemi/kameri',
    updated_at    = NOW()
WHERE id = 237;

-- cat 238 (DVR под HD аналогови системи)
UPDATE categories
SET tekra_slug    = 'zapisvashti-ustroystva',
    category_path = 'videonablyudenie/hd-analogovi-sistemi/zapisvashti-ustroystva',
    updated_at    = NOW()
WHERE id = 238;

-- cat 239 (Сървъри под IP системи) — script 33 сетна само path, не slug
UPDATE categories
SET tekra_slug    = 'survuri',
    category_path = 'videonablyudenie/ip-sistemi/survuri',
    updated_at    = NOW()
WHERE id = 239;

-- cat 240 (NVR под NDAA)
UPDATE categories
SET tekra_slug    = 'nvr',
    category_path = 'videonablyudenie/ndaa/nvr',
    updated_at    = NOW()
WHERE id = 240;

-- cat 241 (Основи за монтаж под NDAA)
UPDATE categories
SET tekra_slug    = 'osnovi-za-montazh-na-kameri',
    category_path = 'videonablyudenie/ndaa/osnovi-za-montazh-na-kameri',
    updated_at    = NOW()
WHERE id = 241;

-- cat 242 (Комплект под NDAA)
UPDATE categories
SET tekra_slug    = 'komplekt',
    category_path = 'videonablyudenie/ndaa/komplekt',
    updated_at    = NOW()
WHERE id = 242;

-- cat 243 (Сървър под NDAA)
UPDATE categories
SET tekra_slug    = 'survur',
    category_path = 'videonablyudenie/ndaa/survur',
    updated_at    = NOW()
WHERE id = 243;

-- ============================================================
-- В: ЛИПСВАЩИ tekra_slug за Аксесоари подкатегории
--    Script 33 сетна само category_path за тях
-- ============================================================

-- cat 244 (Конектори)
UPDATE categories
SET tekra_slug = 'konektori',
    updated_at = NOW()
WHERE id = 244;

-- cat 245 (Видеобалуни)
UPDATE categories
SET tekra_slug = 'videobaluni',
    updated_at = NOW()
WHERE id = 245;

-- cat 246 (Стойки и основи за камери)
UPDATE categories
SET tekra_slug = 'stoyki-i-osnovi-za-kameri',
    updated_at = NOW()
WHERE id = 246;

-- cat 247 (Защити и изолатори)
UPDATE categories
SET tekra_slug = 'zashtiti-i-izolatori',
    updated_at = NOW()
WHERE id = 247;

-- cat 249 (Монитори)
UPDATE categories
SET tekra_slug = 'monitori',
    updated_at = NOW()
WHERE id = 249;

-- cat 52 (Интерактивни дисплеи)
UPDATE categories
SET tekra_slug = 'interaktivni-displei',
    updated_at = NOW()
WHERE id = 52;

-- ============================================================
-- Г: НОВИ КАТЕГОРИИ под Системи за пренос (parent=970)
-- ============================================================

-- Захранване (Tekra id 254, slug: zahranvane-254)
INSERT INTO categories (name_bg, name_en, slug, tekra_slug, category_path, parent_id,
                        show_flag, sort_order, is_promo_active, platform,
                        created_at, updated_at, created_by, last_modified_by)
SELECT 'Захранване', 'Power Supply',
       'sistemi-prenos-zahranvane', 'zahranvane-254',
       'videonablyudenie/sistemi-za-prenos-na-danni-i-elektrozahranvane/zahranvane-254',
       970, true, 2, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'sistemi-prenos-zahranvane');

-- Суичове (Tekra id 255, slug: suichove-255)
INSERT INTO categories (name_bg, name_en, slug, tekra_slug, category_path, parent_id,
                        show_flag, sort_order, is_promo_active, platform,
                        created_at, updated_at, created_by, last_modified_by)
SELECT 'Суичове', 'Switches',
       'sistemi-prenos-suichove', 'suichove-255',
       'videonablyudenie/sistemi-za-prenos-na-danni-i-elektrozahranvane/suichove-255',
       970, true, 3, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'sistemi-prenos-suichove');

-- Конвертори (Tekra id 256, slug: konvertori)
INSERT INTO categories (name_bg, name_en, slug, tekra_slug, category_path, parent_id,
                        show_flag, sort_order, is_promo_active, platform,
                        created_at, updated_at, created_by, last_modified_by)
SELECT 'Конвертори', 'Converters',
       'sistemi-prenos-konvertori', 'konvertori',
       'videonablyudenie/sistemi-za-prenos-na-danni-i-elektrozahranvane/konvertori',
       970, true, 4, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'sistemi-prenos-konvertori');

COMMIT;

-- ============================================================
-- ФИНАЛНА ПРОВЕРКА — пълно дърво с tekra_slug
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
    REPEAT('  ', depth) || t.name_bg          AS tree,
    t.id,
    t.tekra_slug,
    CASE WHEN t.tekra_slug IS NULL THEN '⚠ MISSING' ELSE '✓' END AS slug_ok,
    COUNT(p.id) FILTER (WHERE p.platform = 'TEKRA') AS tekra_products
FROM tree t
LEFT JOIN products p ON p.category_id = t.id
GROUP BY t.id, t.name_bg, t.depth, t.sort_order, t.path_sort, t.tekra_slug, t.category_path
ORDER BY t.path_sort;
