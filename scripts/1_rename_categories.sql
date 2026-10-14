-- =============================================================================
-- Скрипт за преименуване и реорганизация на категории
-- Изпълни ръчно: psql -U <user> -d <db> -f rename_categories.sql
-- НЕ е Flyway миграция — може да се изпълнява многократно (UPDATE е идемпотентен)
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. ВИДЕОНАБЛЮДЕНИЕ — подкатегории
-- =============================================================================

-- NDAA → NDAA сертифицирани системи
UPDATE categories
SET name_bg = 'NDAA сертифицирани системи',
    name_en = 'NDAA Certified Systems',
    updated_at = NOW()
WHERE tekra_id = '241';

-- Аксесоари (под Видеонаблюдение) → Консумативи
UPDATE categories
SET name_bg = 'Консумативи',
    name_en = 'Consumables',
    updated_at = NOW()
WHERE tekra_id = '23';

-- HD аналогови системи / Записващи устройства → Записващи устройства - DVR
UPDATE categories
SET name_bg = 'Записващи устройства - DVR',
    name_en = 'Recording Devices - DVR',
    updated_at = NOW()
WHERE tekra_id = '20';

-- IP системи / Записващи устройства → Записващи устройства - NVR
-- (търсим по name_bg + parent, тъй като нямаме уникален tekra_id)
UPDATE categories
SET name_bg = 'Записващи устройства - NVR',
    name_en = 'Recording Devices - NVR',
    updated_at = NOW()
WHERE name_bg = 'Записващи устройства'
  AND parent_id = (SELECT id FROM categories WHERE tekra_id = '22');

-- =============================================================================
-- 2. КОМПЮТЪРНИ КОМПОНЕНТИ — подкатегории
-- =============================================================================

-- Solid State Drive (SSD) дискове → SSD (Solid State Drive)
UPDATE categories
SET name_bg = 'SSD (Solid State Drive)',
    updated_at = NOW()
WHERE external_id = 377;

-- =============================================================================
-- 3. КОМПЮТЪРНИ СИСТЕМИ — преименуване и подкатегории
-- =============================================================================

-- Компютърни системи → КОМПЮТРИ
UPDATE categories
SET name_bg = 'КОМПЮТРИ',
    updated_at = NOW()
WHERE external_id = 549;

-- PC системи → Настолни компютри
UPDATE categories
SET name_bg = 'Настолни компютри',
    name_en = 'Desktop Computers',
    updated_at = NOW()
WHERE external_id = 550;

-- Сървърни системи → Сървъри
UPDATE categories
SET name_bg = 'Сървъри',
    name_en = 'Servers',
    updated_at = NOW()
WHERE external_id = 576;

-- Тънки клиенти → Работни станции
UPDATE categories
SET name_bg = 'Работни станции',
    name_en = 'Workstations',
    updated_at = NOW()
WHERE external_id = 617;

-- =============================================================================
-- 4. ЛАПТОПИ, ТАБЛЕТИ И АКСЕСОАРИ — подкатегории
-- =============================================================================

-- Захранвания за лаптопи → Зарядни за лаптопи
UPDATE categories
SET name_bg = 'Зарядни за лаптопи',
    name_en = 'Laptop Chargers',
    updated_at = NOW()
WHERE external_id = 536;

-- =============================================================================
-- 5. МОНИТОРИ И ДИСПЛЕИ
-- =============================================================================

-- Монитори и публични дисплеи → Монитори
UPDATE categories
SET name_bg = 'Монитори',
    name_en = 'Monitors',
    updated_at = NOW()
WHERE external_id = 394;

-- =============================================================================
-- 6. ПРИНТЕРИ, СКЕНЕРИ И КОНСУМАТИВИ — подкатегории
-- =============================================================================

-- Принтери и МФУ устройства под наем → скрий
UPDATE categories
SET show_flag = false,
    updated_at = NOW()
WHERE external_id = 673;

-- Консумативи за лазерни принтери и копири → Консумативи(тонери) за лазерни устройства
UPDATE categories
SET name_bg = 'Консумативи(тонери) за лазерни устройства',
    name_en = 'Consumables (toners) for laser devices',
    updated_at = NOW()
WHERE external_id = 486;

-- Касети за струйни принтери → Консумативи за мастиленоструйни устройства
UPDATE categories
SET name_bg = 'Консумативи за мастиленоструйни устройства',
    name_en = 'Consumables for inkjet devices',
    updated_at = NOW()
WHERE external_id = 449;

-- Рефили и наливни мастила за принтери → Наливни мастила за принтери
UPDATE categories
SET name_bg = 'Наливни мастила за принтери',
    name_en = 'Liquid inks for printers',
    updated_at = NOW()
WHERE external_id = 450;

-- Хартия за струйни принтери → Хартия за принтери
UPDATE categories
SET name_bg = 'Хартия за принтери',
    name_en = 'Paper for printers',
    updated_at = NOW()
WHERE external_id = 451;

-- =============================================================================
-- 7. МРЕЖОВО ОБОРУДВАНЕ → Рутери и мрежово оборудване
-- =============================================================================

UPDATE categories
SET name_bg = 'Рутери и мрежово оборудване',
    name_en = 'Routers and network equipment',
    updated_at = NOW()
WHERE external_id = 412;

-- IP камери → IP камери и смарт у-ва
UPDATE categories
SET name_bg = 'IP камери и смарт у-ва',
    name_en = 'IP cameras and smart devices',
    updated_at = NOW()
WHERE external_id = 413;

-- Комуникационен шкаф → Комуникационни шкафове - RACK
UPDATE categories
SET name_bg = 'Комуникационни шкафове - RACK',
    name_en = 'Communication cabinets - RACK',
    updated_at = NOW()
WHERE external_id = 418;

-- Суичове - без контрол → Суичове - неуправляеми
UPDATE categories
SET name_bg = 'Суичове - неуправляеми',
    name_en = 'Unmanaged switches',
    updated_at = NOW()
WHERE external_id = 420;

-- Суичове - с контрол → Суичове - управляеми
UPDATE categories
SET name_bg = 'Суичове - управляеми',
    name_en = 'Managed switches',
    updated_at = NOW()
WHERE external_id = 523;

-- =============================================================================
-- 8. МОБИЛНИ ТЕЛЕФОНИ И АКСЕСОАРИ
-- =============================================================================

-- Мобилни телефони и аксесоари → Смарт часовници, телефони и аксесоари
UPDATE categories
SET name_bg = 'Смарт часовници, телефони и аксесоари',
    name_en = 'Smartwatches, phones and accessories',
    updated_at = NOW()
WHERE external_id = 459;

-- Смартчасовници → Смарт часовници
UPDATE categories
SET name_bg = 'Смарт часовници',
    name_en = 'Smartwatches',
    updated_at = NOW()
WHERE external_id = 484;

-- Скрий протектори от "Мост" в категория Мобилни телефони
-- Провери manufacturer name преди изпълнение ('Мост' / 'Most' / точното наименование)
-- UPDATE products
-- SET show_flag = false,
--     updated_at = NOW()
-- WHERE category_id = (SELECT id FROM categories WHERE external_id = 639)
--   AND manufacturer_id IN (
--       SELECT id FROM manufacturers
--       WHERE name ILIKE '%мост%' OR name ILIKE '%most%'
--   );

-- =============================================================================
-- 9. НАВИГАЦИИ, КАМЕРИ И АКСЕСОАРИ ЗА КОЛИ — нови подкатегории
-- =============================================================================

-- Добавяне на "Навигации" като подкатегория
INSERT INTO categories (name_bg, name_en, slug, show_flag, sort_order, parent_id, platform, created_by, last_modified_by, created_at, updated_at)
SELECT
    'Навигации',
    'Navigation systems',
    'navigacii',
    true,
    1,
    id,
    platform,
    'system',
    'system',
    NOW(),
    NOW()
FROM categories
WHERE external_id = 469
  AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'navigacii');

-- Добавяне на "Камери/рекордери" като подкатегория
INSERT INTO categories (name_bg, name_en, slug, show_flag, sort_order, parent_id, platform, created_by, last_modified_by, created_at, updated_at)
SELECT
    'Камери/рекордери',
    'Cameras/recorders',
    'kameri-rekorderi',
    true,
    2,
    id,
    platform,
    'system',
    'system',
    NOW(),
    NOW()
FROM categories
WHERE external_id = 469
  AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'kameri-rekorderi');

-- =============================================================================
-- 10. ПРОДУКТИ ЗА ДОМА → Инструменти и аксесоари
-- =============================================================================

UPDATE categories
SET name_bg = 'Инструменти и аксесоари',
    name_en = 'Tools and accessories',
    updated_at = NOW()
WHERE external_id = 491;

-- Скрий: Комплекти за сервиране
UPDATE categories
SET show_flag = false,
    updated_at = NOW()
WHERE external_id = 645;

-- Скрий: Кутии за храна
UPDATE categories
SET show_flag = false,
    updated_at = NOW()
WHERE external_id = 695;

-- =============================================================================
-- 11. УСЛУГИ → СЕРВИЗНИ УСЛУГИ (направи видима)
-- =============================================================================

UPDATE categories
SET name_bg = 'СЕРВИЗНИ УСЛУГИ',
    name_en = 'Service',
    show_flag = true,
    updated_at = NOW()
WHERE external_id = 708;

-- =============================================================================
-- Проверка на резултата
-- =============================================================================
SELECT id, external_id, tekra_id, name_bg, show_flag
FROM categories
WHERE external_id IN (
    377, 394, 412, 413, 418, 420, 449, 450, 451,
    459, 484, 486, 491, 523, 536, 549, 550, 576,
    617, 639, 645, 673, 695, 708
)
   OR tekra_id IN ('20', '21', '22', '23', '241')
ORDER BY external_id NULLS LAST, tekra_id;

COMMIT;
