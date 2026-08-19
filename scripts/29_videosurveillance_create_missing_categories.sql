-- ============================================================
-- Script 29: Видеонаблюдение — Създаване на липсващи категории
-- Дата: 2026-08-19
-- Потвърдена структура (categories_202608191048.csv):
--   DB колони: show_flag, sort_order, is_promo_active, parent_id
--   Slug НЕ е UNIQUE — използваме WHERE NOT EXISTS за INSERT
--
-- [230] Видеонаблюдение ROOT
--   [231] HD аналогови системи
--     [237] Камери | [238] Записващи устройства - DVR
--   [232] IP системи
--     [100] IP камери и смарт у-ва | [239] Сървъри(hidden)
--     MISSING: Записващи устройства (NVR), Мрежови устройства
--   [233] NDAA сертифицирани системи
--     [240] NVR | [241] Основи за монтаж | [242] Комплект(hidden) | [243] Сървър(hidden)
--     MISSING: IP камери, Мрежово оборудване
--   [234] Консумативи
--     [244] Конектори | [245] Видеобалуни | [246] Стойки | [247] Защити
--     MISSING: Адаптери, Захранващи блокове
--   [235] Твърди дискове (show_flag=false)
--   [236] Монитори и аксесоари
--     [249] Монитори | MISSING: Интерактивни дисплеи (cat 52 е под cat 49)
-- ============================================================

BEGIN;

-- ============================================================
-- СТЪПКА 1: Покажи вече съществуващи но скрити категории
-- ============================================================

UPDATE categories SET show_flag = true, updated_at = NOW() WHERE id = 235;  -- Твърди дискове
UPDATE categories SET show_flag = true, updated_at = NOW() WHERE id = 239;  -- Сървъри (IP системи)
UPDATE categories SET show_flag = true, updated_at = NOW() WHERE id = 242;  -- Комплект (NDAA)
UPDATE categories SET show_flag = true, updated_at = NOW() WHERE id = 243;  -- Сървър (NDAA)

-- ============================================================
-- СТЪПКА 2: Нови подкатегории под IP системи (parent_id=232)
-- ============================================================

-- 2А: Записващи устройства (NVR)
INSERT INTO categories (name_bg, name_en, slug, parent_id, show_flag, sort_order, is_promo_active, platform, created_at, updated_at, created_by, last_modified_by)
SELECT 'Записващи устройства', 'Recording Devices - NVR', 'ip-sistemi-zapisvashti-ustrojstva',
       232, true, 2, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'ip-sistemi-zapisvashti-ustrojstva');

-- 2Б: Мрежови устройства
INSERT INTO categories (name_bg, name_en, slug, parent_id, show_flag, sort_order, is_promo_active, platform, created_at, updated_at, created_by, last_modified_by)
SELECT 'Мрежови устройства', 'Network Devices', 'ip-sistemi-mrezhovi-ustrojstva',
       232, true, 3, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'ip-sistemi-mrezhovi-ustrojstva');

-- Sort на IP системи подкатегории
UPDATE categories SET sort_order = 1, updated_at = NOW() WHERE id = 100;   -- IP камери
UPDATE categories SET sort_order = 2, updated_at = NOW() WHERE slug = 'ip-sistemi-zapisvashti-ustrojstva';
UPDATE categories SET sort_order = 3, updated_at = NOW() WHERE slug = 'ip-sistemi-mrezhovi-ustrojstva';
UPDATE categories SET sort_order = 4, updated_at = NOW() WHERE id = 239;   -- Сървъри

-- ============================================================
-- СТЪПКА 3: Нови подкатегории под NDAA (parent_id=233)
-- ============================================================

-- 3А: IP камери под NDAA
INSERT INTO categories (name_bg, name_en, slug, parent_id, show_flag, sort_order, is_promo_active, platform, created_at, updated_at, created_by, last_modified_by)
SELECT 'IP камери', 'IP Cameras', 'ndaa-ip-kameri',
       233, true, 1, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'ndaa-ip-kameri');

-- 3Б: Мрежово оборудване под NDAA
INSERT INTO categories (name_bg, name_en, slug, parent_id, show_flag, sort_order, is_promo_active, platform, created_at, updated_at, created_by, last_modified_by)
SELECT 'Мрежово оборудване', 'Network Equipment', 'ndaa-mrezhovo-oborudvane',
       233, true, 5, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'ndaa-mrezhovo-oborudvane');

-- Sort на NDAA подкатегории
UPDATE categories SET sort_order = 2, updated_at = NOW() WHERE id = 240;  -- NVR
UPDATE categories SET sort_order = 3, updated_at = NOW() WHERE id = 241;  -- Основи за монтаж
UPDATE categories SET sort_order = 4, updated_at = NOW() WHERE id = 242;  -- Комплект
UPDATE categories SET sort_order = 5, updated_at = NOW() WHERE slug = 'ndaa-mrezhovo-oborudvane';
UPDATE categories SET sort_order = 6, updated_at = NOW() WHERE id = 243;  -- Сървър

-- ============================================================
-- СТЪПКА 4: Нови подкатегории под Консумативи (parent_id=234)
-- ============================================================

-- 4А: Адаптери (DAHUA PFM300, PFM320D)
INSERT INTO categories (name_bg, name_en, slug, parent_id, show_flag, sort_order, is_promo_active, platform, created_at, updated_at, created_by, last_modified_by)
SELECT 'Адаптери', 'Adapters', 'konsumativy-adapteri',
       234, true, 5, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'konsumativy-adapteri');

-- 4Б: Захранващи блокове
INSERT INTO categories (name_bg, name_en, slug, parent_id, show_flag, sort_order, is_promo_active, platform, created_at, updated_at, created_by, last_modified_by)
SELECT 'Захранващи блокове', 'Power Supplies', 'konsumativy-zahranvashti-blokove',
       234, true, 6, false, 'TEKRA', NOW(), NOW(), 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'konsumativy-zahranvashti-blokove');

-- Sort на Консумативи подкатегории
UPDATE categories SET sort_order = 1, updated_at = NOW() WHERE id = 246;  -- Стойки и основи
UPDATE categories SET sort_order = 2, updated_at = NOW() WHERE id = 244;  -- Конектори
UPDATE categories SET sort_order = 3, updated_at = NOW() WHERE id = 245;  -- Видеобалуни
UPDATE categories SET sort_order = 4, updated_at = NOW() WHERE id = 247;  -- Защити и изолатори
UPDATE categories SET sort_order = 5, updated_at = NOW() WHERE slug = 'konsumativy-adapteri';
UPDATE categories SET sort_order = 6, updated_at = NOW() WHERE slug = 'konsumativy-zahranvashti-blokove';

-- ============================================================
-- СТЪПКА 5: Интерактивни дисплеи (cat 52) → под Монитори и аксесоари (236)
-- Текущо: cat 52 е под [49] Монитори и дисплеи (главна секция)
-- Нужно:  cat 52 да е под [236] Монитори и аксесоари (Видеонаблюдение)
-- ============================================================
UPDATE categories
SET parent_id   = 236,
    show_flag   = true,
    sort_order  = 2,
    updated_at  = NOW()
WHERE id = 52;

-- ============================================================
-- СТЪПКА 6: Sort на главните подкатегории на Видеонаблюдение (parent=230)
-- ============================================================
UPDATE categories SET sort_order = 1, updated_at = NOW() WHERE id = 231;  -- HD аналогови
UPDATE categories SET sort_order = 2, updated_at = NOW() WHERE id = 232;  -- IP системи
UPDATE categories SET sort_order = 3, updated_at = NOW() WHERE id = 233;  -- NDAA
UPDATE categories SET sort_order = 4, updated_at = NOW() WHERE id = 234;  -- Консумативи
UPDATE categories SET sort_order = 5, updated_at = NOW() WHERE id = 235;  -- Твърди дискове
UPDATE categories SET sort_order = 6, updated_at = NOW() WHERE id = 236;  -- Монитори и аксесоари

-- Sort на HD аналогови подкатегории
UPDATE categories SET sort_order = 1, updated_at = NOW() WHERE id = 237;  -- Камери
UPDATE categories SET sort_order = 2, updated_at = NOW() WHERE id = 238;  -- DVR
-- Sort на Монитори и аксесоари
UPDATE categories SET sort_order = 1, updated_at = NOW() WHERE id = 249;  -- Монитори

COMMIT;

-- ============================================================
-- ПРОВЕРКА: Ново дърво на Видеонаблюдение
-- ============================================================
WITH RECURSIVE tree AS (
    SELECT id, name_bg, parent_id, show_flag, sort_order,
           0 AS depth, LPAD('', 0) || name_bg AS path_sort
    FROM categories WHERE id = 230
    UNION ALL
    SELECT c.id, c.name_bg, c.parent_id, c.show_flag, c.sort_order,
           t.depth + 1,
           t.path_sort || '/' || LPAD(c.sort_order::TEXT, 3, '0') || c.name_bg
    FROM categories c
    JOIN tree t ON t.id = c.parent_id
)
SELECT
    REPEAT('  ', depth) || name_bg          AS tree,
    id,
    show_flag,
    sort_order,
    (SELECT COUNT(*) FROM products p
     WHERE p.category_id = tree.id
       AND p.status = 'AVAILABLE'
       AND p.show_flag = true) AS available
FROM tree
ORDER BY path_sort;
