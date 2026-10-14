-- ============================================================================
-- 58_smart_devices_and_access_control.sql
--
-- Смарт устройства и контрол на достъп — видими вместо спрени (решение на
-- потребителя от 2026-09-25, след скрипт 57).
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
--   1. 11 продукта от скритите 500/548 отиват в съществуващи видими категории:
--      камери и видеозвънци Aqara и IMOU → IP камери (100), комплект IMOU с NVR →
--      NVR (962), GPS тракер CANYON → Други мобилни аксесоари (166).
--   2. 548 става видима „Видеонаблюдение › Контрол на достъп“ (Ubiquiti Access).
--   3. 500 „Смарт устройства“ става видима коренова категория (Aqara, IMOU смарт дом).
--      ИМЕТО ОСТАВА „Смарт устройства“: ASBIS продуктовият sync разпознава нов продукт
--      по името на кореновата категория („Смарт устройства“ във feed-а) — с друго име
--      или под друг родител новите продукти не се внасят. Подкатегориите на ASBIS под
--      500 остават скрити, затова новите продукти (вкл. смарт камери и детски
--      часовници) влизат направо в 500 — админът ги мести при нужда.
--      Ред в менюто 21 (между Софтуер и VR), не 0 — 0 я слага най-отгоре.
--   4. Продуктите, които скрипт 57 спря само защото нямаха категория, се връщат в
--      продажба: Aqara (2), Ubiquiti Access (2) и Realme крушка и кантар (от скритата
--      325 → 500).
--
-- Контролът на достъп на Ubiquiti идва в ASBIS като „Other / Networking - Access
--   Control“ — това име не съвпада с видима категория и преди, и след скрипта, така
--   че новите такива продукти пак не се внасят автоматично.
--
-- ЕДНА КОМАНДА: всичко е в един DO блок — или минава целият, или нищо. Накрая има
--   COMMIT: SQL редакторът на DBeaver може да е в Manual commit (режимът е за всеки
--   редактор поотделно) и тогава без него записът се отменя след 60 s.
--
-- ЗАЩИТА ОТ SYNC: manually_categorized = TRUE за преместените продукти.
-- Откат в края.
-- Дата: 2026-09-25
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

DO $$
DECLARE
    moved INT;
    back  INT;
BEGIN
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((500, 'Смарт устройства'), (548, 'Мрежово - Контрол на достъп'), (230, 'Видеонаблюдение'),
                                (100, 'IP камери'), (962, 'NVR'), (166, 'Други мобилни аксесоари'))) <> 6 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: някоя категория не е тази, която очаквам (вече пуснат скрипт?).';
    END IF;
    IF EXISTS (SELECT 1 FROM categories WHERE slug = 'kontrol-na-dostap') THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: slug kontrol-na-dostap вече е зает.';
    END IF;

    -- 1. Продукти с ясна видима категория (+ Realme от скритата 325 към Смарт устройства)
    UPDATE products p
    SET category_id_pre_phase5 = coalesce(p.category_id_pre_phase5, p.category_id),
        category_id            = m.to_category,
        manually_categorized   = TRUE,
        updated_at             = NOW()
    FROM (VALUES
        (18990, 500, 100), (19093, 500, 100), (18978, 500, 100),   -- Aqara Camera E1, Camera Hub G2H Pro, G3
        (19128, 500, 100),                                         -- Aqara Smart Video Doorbell G4
        (19143, 500, 100), (19124, 500, 100), (19153, 500, 100),   -- IMOU IPC-S21FA, IPC-S41FAP, Cube 4MP
        (19087, 500, 100), (18985, 500, 100),                      -- IMOU Rex 4MP, Doorbell DB61i
        (19046, 500, 962),                                         -- IMOU Wireless CCTV Kit-Lite (4 камери + NVR)
        (19141, 500, 166),                                         -- CANYON GPS тракер ST-02
        (27509, 325, 500), (27512, 325, 500)                       -- Realme кантар и крушка
    ) AS m(product_id, from_category, to_category)
    WHERE p.id = m.product_id AND p.category_id = m.from_category;
    GET DIAGNOSTICS moved = ROW_COUNT;
    IF moved <> 13 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: преместени биха били % от 13 продукта — някой не е там, където го очаквах.', moved;
    END IF;

    -- 2. Контрол на достъп под Видеонаблюдение
    UPDATE categories
    SET parent_id = 230, name_bg = 'Контрол на достъп', name_en = 'Access control', slug = 'kontrol-na-dostap',
        category_path = 'videonablyudenie/kontrol-na-dostap', show_flag = TRUE, sort_order = 7, updated_at = NOW()
    WHERE id = 548;

    -- 3. Смарт устройства — видим корен, 21-ва позиция в менюто
    UPDATE categories SET show_flag = TRUE, sort_order = 21, name_en = 'Smart devices', updated_at = NOW() WHERE id = 500;

    -- 4. Обратно в продажба — спрени от скрипт 57 само заради липсата на категория
    UPDATE products
    SET manually_hidden = FALSE, show_flag = (active AND status = 'AVAILABLE' AND NOT deleted), updated_at = NOW()
    WHERE id IN (19016, 19170, 11408, 11413, 27509, 27512) AND manually_hidden;
    GET DIAGNOSTICS back = ROW_COUNT;

    RAISE NOTICE 'Скрипт 58: преместени % продукта; % върнати в продажба; 548 и 500 са видими.', moved, back;
END $$;

-- Контрола (само чете).
SELECT c.id, c.name_bg, c.parent_id, c.show_flag, c.sort_order,
       (SELECT count(*) FROM products p WHERE p.category_id = c.id AND p.active AND p.show_flag
                                        AND p.status = 'AVAILABLE' AND NOT p.deleted) AS visible_products
FROM categories c WHERE c.id IN (500, 548) ORDER BY c.id;

-- В DBeaver с Manual commit горното чака потвърждение и след 60 s сървърът го отменя
-- (така се провалиха първите два опита). В auto-commit/psql COMMIT е безвреден.
COMMIT;

-- ============================================================================
-- ОТКАТ:
--   BEGIN;
--   UPDATE products p SET category_id = m.f, manually_categorized = FALSE, updated_at = NOW()
--   FROM (VALUES (18990, 500, 100), (19093, 500, 100), (18978, 500, 100), (19128, 500, 100),
--                (19143, 500, 100), (19124, 500, 100), (19153, 500, 100), (19087, 500, 100),
--                (18985, 500, 100), (19046, 500, 962), (19141, 500, 166), (27509, 325, 500),
--                (27512, 325, 500)) AS m(id, f, t)
--   WHERE p.id = m.id AND p.category_id = m.t;
--   UPDATE categories SET parent_id = NULL, name_bg = 'Мрежово - Контрол на достъп',
--          name_en = 'Networking - Access Control', slug = 'networking-access-control-asbis',
--          category_path = 'other/networking-access-control-asbis', show_flag = FALSE, sort_order = 0
--   WHERE id = 548;
--   UPDATE categories SET show_flag = FALSE, sort_order = 0, name_en = 'Смарт устройства' WHERE id = 500;
--   UPDATE products SET manually_hidden = TRUE, show_flag = FALSE
--   WHERE id IN (19016, 19170, 11408, 11413, 27509, 27512);
--   COMMIT;
-- ============================================================================
