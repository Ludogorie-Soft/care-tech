-- ============================================================================
-- 55_filters_curated_16_smart_devices_leftovers.sql
--
-- Курация на каноничния филтърен слой, партида 16 — остатъкът след партида 15 и скрипт 59
-- (rebuild #13 в прод, 2026-09-28).
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
--   1. „Смарт устройства“ (500, автоматичен режим): след като часовниците и камерите излязоха,
--      rebuild-ът направи две групи от параметрите на ASBIS, които не стават за филтър:
--        • „Размер на интелигентния контакт“ — габарити („55x55x85 мм“), по един продукт на стойност;
--        • „Описание на сензора“ — „Smart Plug“ / „Smart Socket“ / „Temperature Sensor<br/>…“,
--          повтаря вече показаната група „Тип продукт“.
--      → IGNORE по име (както бутона „Игнорирай“ в админа): остават в спецификацията на продукта,
--        не са филтър. Автоматичните свойства остават празни.
--   2. Отчетът „Несъпоставени“ (3):
--        • Обектив „2.3мм“ → нова стойност „2.3 mm“ (между 1.65 и 2.8 mm) + шаблон като на
--          останалите обективи (не хваща диапазони „2.3–12 мм“ — те са варифокални);
--        • Wi-Fi „bluetooth 5.4“ и списък с функции на камера при „Звук“ → без стойност.
--
-- ИЗИСКВА: 55_filters_curated_15 и rebuild след него.
-- СЛЕД СКРИПТА: Админ → „Филтри“ → Rebuild.
--
-- ЕДНА КОМАНДА: целият запис е в един DO блок. Накрая има COMMIT (DBeaver в Manual commit).
-- ИДЕМПОТЕНТНОСТ: правилата с note = '55 партида 16' се трият и вмъкват наново; точните правила
--   по отчета ('55 партида 16 — по отчета') остават.
-- Дата: 2026-09-28
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

DO $$
DECLARE
    ignored INT; lens_rules INT; drops INT;
BEGIN
    IF (SELECT count(*) FROM filter_attributes
        WHERE origin = 'AUTO' AND auto_key IN ('размер на интелигентния контакт', 'описание на сензора')) <> 2 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: няма двете автоматични групи на „Смарт устройства“ — първо rebuild.';
    END IF;
    IF (SELECT count(*) FROM filter_values v JOIN filter_attributes a ON a.id = v.attribute_id
        WHERE a.slug = 'camera-lens' AND v.norm_key IN ('1.65 mm', '2.8 mm', '3.6 mm', '4 mm', '6 mm', 'варифокален / зуум')) <> 6 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: стойностите на „Обектив“ не са каквито очаквам — първо пусни 55_filters_curated_15.';
    END IF;

    DELETE FROM filter_value_rules WHERE note = '55 партида 16';
    DELETE FROM filter_attribute_sources WHERE note = '55 партида 16';

    -- ── 1. Двете групи → IGNORE ──────────────────────────────────────────────
    INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, origin, note)
    SELECT 'IGNORE', NULL, a.auto_key, 'MANUAL', '55 партида 16'
    FROM filter_attributes a
    WHERE a.origin = 'AUTO' AND a.auto_key IN ('размер на интелигентния контакт', 'описание на сензора');
    GET DIAGNOSTICS ignored = ROW_COUNT;

    -- ── 2. Обектив 2.3 mm ────────────────────────────────────────────────────
    INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
    SELECT a.id, '2.3 mm', '2.3 mm', filter_norm('2.3 mm'), 1, 'MANUAL'
    FROM filter_attributes a WHERE a.slug = 'camera-lens'
    ON CONFLICT (attribute_id, norm_key) DO NOTHING;

    -- 1.65 · 2.3 · 2.8 · 3.6 · 4 · 6 · варифокален
    UPDATE filter_values v
    SET sort_order = CASE v.norm_key WHEN '1.65 mm' THEN 0 WHEN '2.3 mm' THEN 1 WHEN '2.8 mm' THEN 2
                                     WHEN '3.6 mm' THEN 3 WHEN '4 mm' THEN 4 WHEN '6 mm' THEN 5
                                     WHEN 'варифокален / зуум' THEN 6 END
    FROM filter_attributes a
    WHERE a.id = v.attribute_id AND a.slug = 'camera-lens'
      AND v.norm_key IN ('1.65 mm', '2.3 mm', '2.8 mm', '3.6 mm', '4 mm', '6 mm', 'варифокален / зуум');

    INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
    SELECT a.id, '(?<![0-9.])(?<![-–~] )(?<![-–~])2[.,]3 ?(mm|мм)(?! ?[-–~])', v.id, '55 партида 16'
    FROM filter_attributes a
    JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = '2.3 mm'
    WHERE a.slug = 'camera-lens';
    GET DIAGNOSTICS lens_rules = ROW_COUNT;

    PERFORM '' ~* pattern FROM filter_value_rules WHERE note = '55 партида 16' AND pattern IS NOT NULL;

    -- ── 3. Шум → без стойност (само двата реда от отчета) ────────────────────
    INSERT INTO filter_value_rules (attribute_id, raw_norm, value_id, note)
    SELECT u.attribute_id, u.raw_norm, NULL, '55 партида 16 — по отчета'
    FROM filter_unmapped_values u
    JOIN filter_attributes a ON a.id = u.attribute_id
    WHERE u.raw_norm = filter_norm(u.raw_norm)
      AND ((a.slug = 'wifi' AND u.raw_norm = 'bluetooth 5.4')
        OR (a.slug = 'camera-audio' AND u.raw_norm ~ '^наклон<br/>pan<br/>push notifications<br/>'))
    ON CONFLICT DO NOTHING;
    GET DIAGNOSTICS drops = ROW_COUNT;

    IF ignored <> 2 OR lens_rules <> 1 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: очаквах 2 правила IGNORE и 1 шаблон за обектив; има % и %.', ignored, lens_rules;
    END IF;

    RAISE NOTICE 'Партида 16: % групи игнорирани, нова стойност „2.3 mm“ с % шаблон, % текста без стойност. Следва Rebuild.',
        ignored, lens_rules, drops;
END $$;

-- Контрола (само чете).
SELECT 'source' AS kind, action AS what, name_norm AS detail FROM filter_attribute_sources WHERE note = '55 партида 16'
UNION ALL
SELECT 'rule', CASE WHEN value_id IS NULL THEN 'без стойност' ELSE 'стойност' END, left(coalesce(pattern, raw_norm), 60)
FROM filter_value_rules WHERE note LIKE '55 партида 16%'
ORDER BY 1, 2;

-- В DBeaver с Manual commit горното чака потвърждение и след 60 s сървърът го отменя.
-- В auto-commit/psql COMMIT е безвреден.
COMMIT;

-- ============================================================================
-- ОТКАТ:
--   DELETE FROM filter_attribute_sources WHERE note = '55 партида 16';
--   DELETE FROM filter_value_rules WHERE note LIKE '55 партида 16%';
--   (стойността „2.3 mm“ остава безвредна; после Rebuild)
-- ============================================================================
