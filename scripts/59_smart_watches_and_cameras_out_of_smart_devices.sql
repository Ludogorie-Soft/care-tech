-- ============================================================================
-- 59_smart_watches_and_cameras_out_of_smart_devices.sql
--
-- Смарт часовниците и камерите от „Смарт устройства“ (500) отиват в истинските си видими
-- категории. Продължение на скрипт 58 (решение на потребителя от 2026-09-28).
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
-- ЗАЩО: откакто 500 е видима (скрипт 58), всичко, което ASBIS има в нея, е в менюто „Смарт
--   устройства“ — и нощният sync слага там всеки нов продукт, чиято ASBIS подкатегория е
--   скрита („Смарт часовник“, „Външна/Вътрешна смарт IP камера“…). В прод на 2026-09-28 в 500
--   има 225 продукта (53 видими), сред тях:
--     • 41 часовника и гривни (22 видими) — REDMI Watch 6, Xiaomi Watch S5, Xiaomi Smart Band
--       10 Pro / 11 Active, CANYON (и детските) → 156 „Смарт часовници“. Гривните също там:
--       „Фитнес гривни“ (157) е скрита категория.
--     • 92 камери, видеозвънци и комплекти с камера (13 видими) — IMOU, Xiaomi, Aqara Camera
--       Hub → 100 „IP камери“ (както камерите и звънците на Aqara/IMOU в скрипт 58).
--   Остават в 500: сензори, ключове, крушки, хъбове, хранилки, соларният панел за камера
--   на Xiaomi и Aqara Cube Controller.
--
-- ИЗБОР ПО ИМЕ, с проверка на броя: часовник/гривна = „watch“ или „smart band“ в името;
--   камера = „camera“, „cam“, „doorbell“ или „door view“ (без двете изключения по-горе),
--   а детските часовници с камера са часовници. Нов продукт от тази нощ, ако е часовник или
--   камера, също се мести; ако броят излезе извън очакваното, скриптът спира.
--
-- ЕДНА КОМАНДА: всичко е в един DO блок — или минава целият, или нищо. Накрая има COMMIT
--   (SQL редакторът на DBeaver може да е в Manual commit; в auto-commit/psql е безвреден).
--
-- ЗАЩИТА ОТ SYNC: manually_categorized = TRUE (V37) — ASBIS не връща категорията.
-- Откат в края. Дата: 2026-09-28
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

DO $$
DECLARE
    watch_rx  CONSTANT TEXT := '(watch|smart ?band)';
    camera_rx CONSTANT TEXT := '(camera|(^|[^[:alpha:]])cam([^[:alpha:]]|$)|doorbell|door view)';
    keep_rx   CONSTANT TEXT := '(solar panel \(|cube controller)';
    watches INT; cameras INT; protected INT;
BEGIN
    IF (SELECT count(*) FROM categories
        WHERE show_flag AND (id, name_bg) IN ((500, 'Смарт устройства'), (156, 'Смарт часовници'), (100, 'IP камери'))) <> 3 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: 500, 156 или 100 не е видимата категория, която очаквам.';
    END IF;

    SELECT count(*) FILTER (WHERE name_bg ~* watch_rx),
           count(*) FILTER (WHERE name_bg !~* watch_rx AND name_bg ~* camera_rx AND name_bg !~* keep_rx)
      INTO watches, cameras
    FROM products WHERE category_id = 500 AND NOT deleted;
    -- 2026-09-28: 41 и 92. Долната граница хваща преместени/преименувани, горната — изненади.
    IF watches NOT BETWEEN 41 AND 60 OR cameras NOT BETWEEN 92 AND 120 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: в 500 има % часовника/гривни (очаквах 41) и % камери (очаквах 92) — вече пуснат скрипт?',
            watches, cameras;
    END IF;

    UPDATE products p
    SET category_id_pre_phase5 = coalesce(p.category_id_pre_phase5, p.category_id),
        category_id            = CASE WHEN p.name_bg ~* watch_rx THEN 156 ELSE 100 END,
        manually_categorized   = TRUE,
        updated_at             = NOW()
    WHERE p.category_id = 500 AND NOT p.deleted
      AND (p.name_bg ~* watch_rx OR (p.name_bg ~* camera_rx AND p.name_bg !~* keep_rx));

    -- 5 часовника CANYON, преместени от 500 в 156 преди V37 — без защита ASBIS би ги върнал в 500,
    -- щом се появят пак във feed-а (500 вече е видима).
    UPDATE products
    SET manually_categorized = TRUE, updated_at = NOW()
    WHERE category_id = 156 AND category_id_pre_phase5 = 500 AND NOT manually_categorized AND NOT deleted;
    GET DIAGNOSTICS protected = ROW_COUNT;

    RAISE NOTICE 'Скрипт 59: % часовника и гривни → „Смарт часовници“, % камери → „IP камери“; % по-рано преместени часовника защитени.',
        watches, cameras, protected;
END $$;

-- Контрола (само чете): какво има сега в трите категории.
SELECT c.id, c.name_bg,
       count(p.id) AS products,
       count(p.id) FILTER (WHERE p.show_flag AND p.status = 'AVAILABLE') AS visible
FROM categories c LEFT JOIN products p ON p.category_id = c.id AND NOT p.deleted
WHERE c.id IN (500, 156, 100)
GROUP BY c.id, c.name_bg ORDER BY c.id;

-- В DBeaver с Manual commit горното чака потвърждение и след 60 s сървърът го отменя.
-- В auto-commit/psql COMMIT е безвреден.
COMMIT;

-- ============================================================================
-- ОТКАТ — без продуктите, които вече бяха извън 500 преди скрипта (9 камери от скрипт 58 и
--   5 часовника CANYON отпреди V37; те само губят новата защита):
--   BEGIN;
--   UPDATE products SET category_id = 500, manually_categorized = FALSE, updated_at = NOW()
--   WHERE category_id IN (100, 156) AND category_id_pre_phase5 = 500
--     AND id NOT IN (18978, 18985, 18990, 19087, 19093, 19124, 19128, 19143, 19153,
--                    18972, 19099, 19109, 19155, 19180);
--   UPDATE products SET manually_categorized = FALSE WHERE id IN (18972, 19099, 19109, 19155, 19180);
--   COMMIT;
-- ============================================================================
