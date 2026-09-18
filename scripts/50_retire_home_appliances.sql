-- ============================================================================
-- 50_retire_home_appliances.sql
--
-- Две отделни действия върху поддървото „Дребни домакински уреди":
--   1. Преместване на 87 компютърни вентилатора, сложени по погрешка там
--   2. Спиране от продажба на 268 продукта дребна бяла техника
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
-- ОСНОВАНИЕ: решение на потребителя — дребната бяла техника не се предлага в
--   магазина. Видимото дърво няма коренова категория за нея и няма да се създава.
--
-- ⚠️ ВАЖНО ЗА ОБХВАТА — проверено срещу прод, не прието на доверие:
--   Категория 455 „Вентилатори" под домакинското поддърво съдържа CORSAIR RS120,
--   DeepCool FL12, GELID LYRA и подобни — 87 продукта, всичките КОМПЮТЪРНИ
--   вентилатори, нито един домакински. Затова тя НЕ се скрива, а продуктите ѝ
--   отиват при компютърните вентилатори (id 12).
--   Останалата част от поддървото е сканирана за подобно замърсяване
--   (corsair|asus|msi|gigabyte|noctua|arctic|nzxt|deepcool|120mm|140mm|...) —
--   няма друго.
--
-- МЕХАНИЗЪМ НА СКРИВАНЕТО: `manually_hidden = true` (колоната от фаза 1, V36).
--   Само `show_flag = false` НЕ стига — sync-ът преизчислява видимостта всеки
--   прогон и би ги върнал. С този флаг и четирите sync сервиза ги прескачат.
--   Обратимо през админ панела: показване на продукт вдига флага
--   (ProductService.applyAdminVisibility).
--
-- ЗАЩО ВСИЧКИТЕ 268, а не само 72-та видими: продукт със статус NOT_AVAILABLE
--   може да стане наличен утре и тогава ще изплува в магазина.
--
-- ДЪРВОТО НЕ СЕ ПИПА: не се създават, трият или преместват категории;
--   `sort_order`, родители и нива остават непокътнати.
--
-- Дата: 2026-09-18
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';
SET statement_timeout = '300s';


-- ─── СЕКЦИЯ A: отчет ПРЕДИ ──────────────────────────────────────────────────

SELECT 'за преместване в „Вентилатори" (12)' AS deystvie, count(*) AS produkti,
       count(*) FILTER (WHERE active AND show_flag AND status='AVAILABLE'
                          AND deleted=false AND image_url<>'') AS vidimi
FROM products WHERE category_id = 455
UNION ALL
SELECT 'за спиране от продажба', count(*),
       count(*) FILTER (WHERE p.active AND p.show_flag AND p.status='AVAILABLE'
                          AND p.deleted=false AND p.image_url<>'')
FROM products p JOIN categories c ON c.id = p.category_id
WHERE (c.id IN (454, 556) OR (c.parent_id = 454 AND c.id <> 455));


-- ─── СЕКЦИЯ B: промените ────────────────────────────────────────────────────

BEGIN;

-- B1. Компютърните вентилатори отиват при компютърните вентилатори.
--     Оригиналната категория се пази в колоната от скрипт 48.
UPDATE products
SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
    category_id            = 12,
    updated_at             = NOW()
WHERE category_id = 455;

-- B2. Дребната бяла техника спира да се предлага.
UPDATE products p
SET manually_hidden = true,
    show_flag       = false,
    updated_at      = NOW()
FROM categories c
WHERE c.id = p.category_id
  AND (c.id IN (454, 556) OR (c.parent_id = 454 AND c.id <> 455))
  AND (p.manually_hidden IS NOT TRUE OR p.show_flag IS NOT FALSE);


-- ─── СЕКЦИЯ C: предпазител ──────────────────────────────────────────────────

DO $$
DECLARE
    v_ostanali_455   int;
    v_vidimi_bqla    int;
    v_ne_markirani   int;
    v_ventilatori    int;
    v_markirani      int;
    v_zasegnati_drugi int;
BEGIN
    SELECT count(*) INTO v_ostanali_455 FROM products WHERE category_id = 455;
    IF v_ostanali_455 > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % продукта още са в 455', v_ostanali_455;
    END IF;

    -- Нито един от спрените не бива да остане видим
    SELECT count(*) INTO v_vidimi_bqla
      FROM products p JOIN categories c ON c.id = p.category_id
     WHERE (c.id IN (454, 556) OR (c.parent_id = 454 AND c.id <> 455))
       AND p.active AND p.show_flag AND p.status = 'AVAILABLE'
       AND p.deleted = false AND p.image_url <> '';
    IF v_vidimi_bqla > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % продукта от бялата техника още са видими', v_vidimi_bqla;
    END IF;

    SELECT count(*) INTO v_ne_markirani
      FROM products p JOIN categories c ON c.id = p.category_id
     WHERE (c.id IN (454, 556) OR (c.parent_id = 454 AND c.id <> 455))
       AND p.manually_hidden IS NOT TRUE;
    IF v_ne_markirani > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % продукта не са маркирани manually_hidden', v_ne_markirani;
    END IF;

    -- Информативно, не блокиращо. UPDATE-ът е ограничен с WHERE по категория, така
    -- че не може да маркира нищо извън обхвата; но ако админ е скрил продукт ръчно
    -- през панела, той също ще е с този флаг и това е нормално. Базовата линия при
    -- писането на скрипта беше 0.
    SELECT count(*) INTO v_zasegnati_drugi
      FROM products p WHERE p.manually_hidden IS TRUE
       AND p.category_id NOT IN (
           SELECT c.id FROM categories c
            WHERE c.id IN (454, 556) OR (c.parent_id = 454 AND c.id <> 455));
    IF v_zasegnati_drugi > 0 THEN
        RAISE NOTICE 'Забележка: % продукта извън обхвата също са manually_hidden — вероятно скрити от админ.',
                     v_zasegnati_drugi;
    END IF;

    SELECT count(*) INTO v_ventilatori FROM products WHERE category_id = 12;
    SELECT count(*) INTO v_markirani
      FROM products p JOIN categories c ON c.id = p.category_id
     WHERE (c.id IN (454, 556) OR (c.parent_id = 454 AND c.id <> 455));

    RAISE NOTICE '✓ Проверките минаха. Преместени в „Вентилатори" — там вече има % продукта. Спрени от продажба: %.',
                 v_ventilatori, v_markirani;
END $$;

COMMIT;


-- ─── СЕКЦИЯ D: отчет СЛЕД ───────────────────────────────────────────────────

SELECT (SELECT count(*) FROM products WHERE category_id = 12)          AS ventilatori_12,
       (SELECT count(*) FROM products WHERE category_id = 455)          AS ostanali_v_455,
       (SELECT count(*) FROM products WHERE manually_hidden)            AS spreni_ot_prodazhba,
       (SELECT count(*) FROM products p JOIN categories c ON c.id = p.category_id
         WHERE NOT c.show_flag AND p.active AND p.show_flag AND p.status = 'AVAILABLE'
           AND p.deleted = false AND p.image_url <> '')                 AS vidimi_v_nevidimi_kategorii;


-- ============================================================================
-- ОТКАТ
--   BEGIN;
--   UPDATE products SET category_id = 455, category_id_pre_phase5 = NULL
--    WHERE category_id_pre_phase5 = 455;
--
--   UPDATE products p SET manually_hidden = false
--     FROM categories c
--    WHERE c.id = p.category_id
--      AND (c.id IN (454, 556) OR (c.parent_id = 454 AND c.id <> 455));
--   -- show_flag се възстановява от следващия sync прогон по наличност и цена
--   COMMIT;
-- ============================================================================
