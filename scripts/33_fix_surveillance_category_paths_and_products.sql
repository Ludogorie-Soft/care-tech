-- ============================================================
-- Script 33: Поправка на tekra_slug, category_path и продукти
--            за раздел Видеонаблюдение
-- Дата: 2026-08-19
--
-- ПРОБЛЕМ: tekra_slug и category_path са от СТАРА структура на Текра.
-- Синкът ги ползва за path-matching → продуктите отиват в грешни категории.
-- 207 TEKRA продукта са в cat 231 (HD аналогови — родителска) вместо IP.
--
-- ДЕЙСТВИЯ:
--   А — Преименуване на 4 категории (да съответстват на Текра)
--   Б — Поправка на tekra_slug и category_path за 19 категории
--   В — Добавяне на tekra_slug/path на новите категории (962-967)
--   Г — Създаване на 4 липсващи категории (Софтуер, DSSExpress, Системи...)
--   Д — Преместване на грешно наредени TEKRA продукти (cat 231, 233, 230)
-- ============================================================

BEGIN;

-- ============================================================
-- А: ПРЕИМЕНУВАНЕ — привеждаме имената към Текра структурата
-- ============================================================

-- HD аналогови: "Камери" → "Аналогови камери" (ясно разграничение от IP)
UPDATE categories
SET name_bg    = 'Аналогови камери',
    name_en    = 'Analog Cameras',
    updated_at = NOW()
WHERE id = 237;

-- HD аналогови: "Записващи устройства - DVR" → "DVR"
UPDATE categories
SET name_bg    = 'DVR',
    name_en    = 'DVR',
    updated_at = NOW()
WHERE id = 238;

-- IP системи: "IP камери и смарт у-ва" → "IP камери"
UPDATE categories
SET name_bg    = 'IP камери',
    name_en    = 'IP Cameras',
    updated_at = NOW()
WHERE id = 100;

-- IP системи: "Записващи устройства" (новата cat 962) → "NVR"
UPDATE categories
SET name_bg    = 'NVR',
    name_en    = 'NVR',
    updated_at = NOW()
WHERE id = 962;

-- NDAA: "NDAA сертифицирани системи" → "NDAA, NIS2"
UPDATE categories
SET name_bg    = 'NDAA, NIS2',
    name_en    = 'NDAA, NIS2',
    updated_at = NOW()
WHERE id = 233;

-- Консумативи → "Аксесоари"
UPDATE categories
SET name_bg    = 'Аксесоари',
    name_en    = 'Accessories',
    updated_at = NOW()
WHERE id = 234;

-- ============================================================
-- Б: ПОПРАВКА НА tekra_slug И category_path — главни категории
-- ============================================================

-- cat 231 (HD аналогови системи):
-- Стар slug: kameri (от СТАРА структура)
-- Нов slug: hd-analogovi-sistemi
UPDATE categories
SET tekra_slug    = 'hd-analogovi-sistemi',
    category_path = 'videonablyudenie/hd-analogovi-sistemi',
    updated_at    = NOW()
WHERE id = 231;

-- cat 232 (IP системи):
-- Стар slug: zapisvashti-ustroystva (от СТАРА структура)
-- Нов slug: ip-sistemi
UPDATE categories
SET tekra_slug    = 'ip-sistemi',
    category_path = 'videonablyudenie/ip-sistemi',
    updated_at    = NOW()
WHERE id = 232;

-- cat 233 (NDAA, NIS2):
-- slug ndaa е правилен, само path се потвърждава
UPDATE categories
SET category_path = 'videonablyudenie/ndaa',
    updated_at    = NOW()
WHERE id = 233;

-- cat 234 (Аксесоари — бивши Консумативи):
-- Стар slug: mrezhovi-ustroystva (от СТАРА структура)
-- Нов slug: aksesoari
UPDATE categories
SET tekra_slug    = 'aksesoari',
    category_path = 'videonablyudenie/aksesoari',
    updated_at    = NOW()
WHERE id = 234;

-- cat 235 (Твърди дискове):
-- Стар slug: adapteri (от СТАРА структура — тогава кат. се е казвала "Адаптери")
-- Нов slug: tvrdi-diskove
UPDATE categories
SET tekra_slug    = 'tvrdi-diskove',
    category_path = 'videonablyudenie/tvrdi-diskove',
    updated_at    = NOW()
WHERE id = 235;

-- cat 236 (Монитори и аксесоари):
-- Стар slug: zahranvashti-blokove (от СТАРА структура)
-- Нов slug: monitori-i-aksesoari
UPDATE categories
SET tekra_slug    = 'monitori-i-aksesoari',
    category_path = 'videonablyudenie/monitori-i-aksesoari',
    updated_at    = NOW()
WHERE id = 236;

-- ============================================================
-- Б: ПОПРАВКА НА category_path — подкатегории на IP системи
-- ============================================================

-- cat 100 (Камери под IP системи)
UPDATE categories
SET tekra_slug    = 'kameri',
    category_path = 'videonablyudenie/ip-sistemi/kameri',
    updated_at    = NOW()
WHERE id = 100;

-- cat 239 (Сървъри под IP системи)
-- Стар path: videonablyudenie/zapisvashti-ustroystva/survuri (грешен родител)
UPDATE categories
SET category_path = 'videonablyudenie/ip-sistemi/survuri',
    updated_at    = NOW()
WHERE id = 239;

-- ============================================================
-- Б: ПОПРАВКА НА category_path — подкатегории на Аксесоари
-- ============================================================

-- cat 244 (Конектори) — path сочеше mrezhovi-ustroystva (стара структура)
UPDATE categories
SET category_path = 'videonablyudenie/aksesoari/konektori',
    updated_at    = NOW()
WHERE id = 244;

-- cat 246 (Стойки и основи за камери)
UPDATE categories
SET category_path = 'videonablyudenie/aksesoari/stoyki-i-osnovi-za-kameri',
    updated_at    = NOW()
WHERE id = 246;

-- cat 245 (Видеобалуни)
UPDATE categories
SET category_path = 'videonablyudenie/aksesoari/videobaluni',
    updated_at    = NOW()
WHERE id = 245;

-- cat 247 (Защити и изолатори)
UPDATE categories
SET category_path = 'videonablyudenie/aksesoari/zashtiti-i-izolatori',
    updated_at    = NOW()
WHERE id = 247;

-- ============================================================
-- Б: ПОПРАВКА НА category_path — Монитори и аксесоари
-- ============================================================

-- cat 249 (Монитори) — path сочеше zahranvashti-blokove (стара структура)
UPDATE categories
SET category_path = 'videonablyudenie/monitori-i-aksesoari/monitori',
    updated_at    = NOW()
WHERE id = 249;

-- cat 52 (Интерактивни дисплеи) — path сочеше monitors-and-displays (грешно дърво)
UPDATE categories
SET category_path = 'videonablyudenie/monitori-i-aksesoari/interaktivni-displei',
    updated_at    = NOW()
WHERE id = 52;

-- ============================================================
-- В: tekra_slug и category_path за НОВИТЕ категории (от script 29)
--    Тези нямаха никакви стойности → синкът не можеше да ги намери
-- ============================================================

-- cat 962 (Записващи устройства под IP системи)
UPDATE categories
SET tekra_slug    = 'zapisvashti-ustroystva',
    category_path = 'videonablyudenie/ip-sistemi/zapisvashti-ustroystva',
    updated_at    = NOW()
WHERE id = 962;

-- cat 963 (Мрежови устройства под IP системи)
UPDATE categories
SET tekra_slug    = 'mrezhovi-ustroystva',
    category_path = 'videonablyudenie/ip-sistemi/mrezhovi-ustroystva',
    updated_at    = NOW()
WHERE id = 963;

-- cat 964 (IP камери под NDAA)
UPDATE categories
SET tekra_slug    = 'ip-kameri',
    category_path = 'videonablyudenie/ndaa/ip-kameri',
    updated_at    = NOW()
WHERE id = 964;

-- cat 965 (Мрежово оборудване под NDAA)
UPDATE categories
SET tekra_slug    = 'mrezhovo-oborudvane',
    category_path = 'videonablyudenie/ndaa/mrezhovo-oborudvane',
    updated_at    = NOW()
WHERE id = 965;

-- cat 966 (Адаптери под Аксесоари)
UPDATE categories
SET tekra_slug    = 'adapteri',
    category_path = 'videonablyudenie/aksesoari/adapteri',
    updated_at    = NOW()
WHERE id = 966;

-- cat 967 (Захранващи блокове под Аксесоари)
UPDATE categories
SET tekra_slug    = 'zahranvashti-blokove',
    category_path = 'videonablyudenie/aksesoari/zahranvashti-blokove',
    updated_at    = NOW()
WHERE id = 967;

-- ============================================================
-- Г: НОВИ КАТЕГОРИИ (Софтуер, Системи за пренос)
-- ============================================================

-- Г1: Софтуер (parent=230)
INSERT INTO categories (name_bg, name_en, slug, tekra_slug, category_path, parent_id,
                        show_flag, sort_order, is_promo_active, platform,
                        created_at, updated_at, created_by, last_modified_by)
SELECT 'Софтуер', 'Software', 'videonablyudenie-softuer', 'softuer',
       'videonablyudenie/softuer',
       230, true, 7, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'videonablyudenie-softuer');

-- Г2: DSSExpress (parent = Софтуер)
INSERT INTO categories (name_bg, name_en, slug, tekra_slug, category_path, parent_id,
                        show_flag, sort_order, is_promo_active, platform,
                        created_at, updated_at, created_by, last_modified_by)
SELECT 'DSSExpress', 'DSSExpress', 'videonablyudenie-softuer-dssexpress', 'dssexpress',
       'videonablyudenie/softuer/dssexpress',
       (SELECT id FROM categories WHERE slug = 'videonablyudenie-softuer'),
       true, 1, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'videonablyudenie-softuer-dssexpress')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'videonablyudenie-softuer');

-- Г3: Системи за пренос на данни и електрозахранване (parent=230)
INSERT INTO categories (name_bg, name_en, slug, tekra_slug, category_path, parent_id,
                        show_flag, sort_order, is_promo_active, platform,
                        created_at, updated_at, created_by, last_modified_by)
SELECT 'Системи за пренос на данни и електрозахранване',
       'Data Transmission and Power Systems',
       'sistemi-za-prenos-na-danni',
       'sistemi-za-prenos-na-danni-i-elektrozahranvane',
       'videonablyudenie/sistemi-za-prenos-na-danni-i-elektrozahranvane',
       230, true, 8, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'sistemi-za-prenos-na-danni');

-- Г4: Аксесоари под Системи за пренос
INSERT INTO categories (name_bg, name_en, slug, tekra_slug, category_path, parent_id,
                        show_flag, sort_order, is_promo_active, platform,
                        created_at, updated_at, created_by, last_modified_by)
SELECT 'Аксесоари', 'Accessories', 'sistemi-prenos-aksesoari', 'aksesoari',
       'videonablyudenie/sistemi-za-prenos-na-danni-i-elektrozahranvane/aksesoari',
       (SELECT id FROM categories WHERE slug = 'sistemi-za-prenos-na-danni'),
       true, 1, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'sistemi-prenos-aksesoari')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'sistemi-za-prenos-na-danni');

-- ============================================================
-- Д: ПРЕМЕСТВАНЕ НА ГРЕШНО НАРЕДЕНИ TEKRA ПРОДУКТИ
-- ============================================================

-- Д1: cat 231 → DVR → cat 238 (Записващи устройства под HD аналогови)
UPDATE products
SET category_id = 238,
    updated_at  = NOW()
WHERE category_id = 231
  AND platform    = 'TEKRA'
  AND (name_en ILIKE '%DVR%' OR name_bg ILIKE '%DVR%'
    OR name_en ILIKE '%Pentabrid%' OR name_en ILIKE '%Penta-Brid%');

-- Д2: cat 231 → NVR → cat 962 (Записващи устройства под IP системи)
UPDATE products
SET category_id = 962,
    updated_at  = NOW()
WHERE category_id = 231
  AND platform    = 'TEKRA'
  AND (name_en ILIKE '%NVR%' OR name_bg ILIKE '%NVR%');

-- Д3: cat 231 → HDCVI/HDTVI аналогови камери → cat 237 (Камери под HD аналогови)
UPDATE products
SET category_id = 237,
    updated_at  = NOW()
WHERE category_id = 231
  AND platform    = 'TEKRA'
  AND (name_en ILIKE '%HDCVI%' OR name_en ILIKE '%HDTVI%'
    OR name_en ILIKE '% CVI %' OR name_en ILIKE '% TVI %'
    OR name_en ILIKE '%аналог%' OR name_en ILIKE '%analog%'
    OR name_en ILIKE '%HAC-%'   -- Dahua HAC = HDCVI серия
    OR name_en ILIKE '%HAC %');

-- Д4: cat 231 → всички останали (IP камери, WiFi, PTZ, Solar...) → cat 100
UPDATE products
SET category_id = 100,
    updated_at  = NOW()
WHERE category_id = 231
  AND platform    = 'TEKRA';

-- Д5: cat 233 → Comelit сървъри → cat 243 (Сървър под NDAA)
UPDATE products
SET category_id = 243,
    updated_at  = NOW()
WHERE category_id = 233
  AND platform    = 'TEKRA'
  AND (name_en ILIKE '%SRV-%'   OR name_bg ILIKE '%сървър%'
    OR name_en ILIKE '%server%' OR name_en ILIKE '%Server%');

-- Д6: cat 233 → Comelit PoE суичове → cat 965 (Мрежово оборудване под NDAA)
UPDATE products
SET category_id = 965,
    updated_at  = NOW()
WHERE category_id = 233
  AND platform    = 'TEKRA'
  AND (name_en ILIKE '%суич%'  OR name_en ILIKE '%switch%'
    OR name_en ILIKE '%IPSW%'  OR name_en ILIKE '%PoE%'
    OR name_bg ILIKE '%суич%');

-- Д7: cat 230 (root) → DAHUA оптичен модул → cat 244 (Конектори)
UPDATE products
SET category_id = 244,
    updated_at  = NOW()
WHERE category_id = 230
  AND platform    = 'TEKRA';

COMMIT;

-- ============================================================
-- ФИНАЛНА ПРОВЕРКА
-- ============================================================

-- Пълно дърво на Видеонаблюдение с продукти
WITH RECURSIVE tree AS (
    SELECT id, name_bg, parent_id, show_flag, sort_order, tekra_slug, category_path,
           0 AS depth, LPAD('', 0) || name_bg AS path_sort
    FROM categories WHERE id = 230
    UNION ALL
    SELECT c.id, c.name_bg, c.parent_id, c.show_flag, c.sort_order,
           c.tekra_slug, c.category_path,
           t.depth + 1,
           t.path_sort || '/' || LPAD(c.sort_order::TEXT,3,'0') || c.name_bg
    FROM categories c JOIN tree t ON t.id = c.parent_id
)
SELECT
    REPEAT('  ', depth) || t.name_bg                            AS tree,
    t.id,
    t.tekra_slug,
    SUBSTRING(t.category_path, 1, 60)                          AS category_path,
    COUNT(p.id) FILTER (WHERE p.status = 'AVAILABLE')          AS available,
    COUNT(p.id) FILTER (WHERE p.platform = 'TEKRA')            AS tekra,
    COUNT(p.id) FILTER (WHERE p.platform = 'ASBIS')            AS asbis,
    COUNT(p.id) FILTER (WHERE p.platform = 'MOST')             AS most
FROM tree t
LEFT JOIN products p ON p.category_id = t.id
GROUP BY t.id, t.name_bg, t.show_flag, t.depth, t.sort_order, t.path_sort, t.tekra_slug, t.category_path
ORDER BY t.path_sort;

-- Провери: не трябва да има TEKRA продукти в родителски категории
SELECT category_id, COUNT(*) AS cnt
FROM products
WHERE platform    = 'TEKRA'
  AND category_id IN (230, 231, 232, 233, 234)
GROUP BY category_id
HAVING COUNT(*) > 0;
