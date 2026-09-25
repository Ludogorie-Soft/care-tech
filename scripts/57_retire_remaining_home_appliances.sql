-- ============================================================================
-- 57_retire_remaining_home_appliances.sql
--
-- Дребната бяла техника и смарт домът, останали видими в скрити категории, спират
-- да се предлагат — довършва скрипт 50.
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
-- ОСНОВАНИЕ: решение на потребителя от 2026-09-18 (скрипт 50) — дребната бяла
--   техника не се предлага и за нея няма видима категория; потвърдено на 2026-09-25
--   и за смарт дома без категория. Тези продукти са в скрити ASBIS корени извън
--   поддървото 454, което скрипт 50 покри, затова са останали видими в търсенето.
--
-- ОБХВАТ (проверено срещу прод на 2026-09-25):
--   1. ВСИЧКИ продукти (и неналичните — за да не изплуват утре) в категориите с
--      чисто домакинска техника и смарт брави: 549 Massage Guns, 557 Air Fryers,
--      561 Прахосмукачка прътова, 570 Dehumidifiers, 572 Compressor, 581 Аксесоари
--      за вакуумзатваряне, 585 Electric Fans, 586 Иригатори, 591 Smart Locks,
--      592 Lint Removers — 26 продукта, 23 от тях видими.
--   2. Само видимите по id: Aqara (500 Смарт устройства), Ubiquiti Access (548) и
--      Realme крушка и кантар (325). Останалите 76 продукта в 500 (вкл. камери
--      Aqara) и 17 в 548 НЕ се пипат — отделно решение.
--
-- МЕХАНИЗЪМ: manually_hidden = true (V36), както скрипт 50. Само show_flag = false
--   не стига — sync-ът преизчислява видимостта всеки прогон. Обратимо през админ
--   панела (показване на продукт вдига флага) или с отката в края.
--
-- Дата: 2026-09-25
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

CREATE TEMP TABLE retire_57 ON COMMIT DROP AS
SELECT p.id
FROM products p
WHERE NOT p.deleted
  AND (p.category_id IN (549, 557, 561, 570, 572, 581, 585, 586, 591, 592)
       OR p.id IN (19016, 19170,      -- Aqara контакт и LED лента (500)
                   11408, 11413,      -- Ubiquiti Enterprise Access Hub, UA-G3-B (548)
                   27509, 27512));    -- Realme кантар и крушка (325)

DO $$
DECLARE n INT; wrong INT;
BEGIN
    SELECT count(*) INTO n FROM retire_57;
    IF n <> 32 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: очаквах 32 продукта, намерени са % — нещо се е променило.', n;
    END IF;
    -- Продуктите по id трябва още да са в категориите, в които ги видях.
    SELECT count(*) INTO wrong FROM products
    WHERE (id IN (19016, 19170) AND category_id <> 500) OR (id IN (11408, 11413) AND category_id <> 548)
       OR (id IN (27509, 27512) AND category_id <> 325);
    IF wrong > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % продукта по id са преместени междувременно — провери.', wrong;
    END IF;
END $$;

UPDATE products p
SET manually_hidden = true,
    show_flag       = false,
    updated_at      = NOW()
FROM retire_57 r
WHERE p.id = r.id
  AND (p.manually_hidden IS NOT TRUE OR p.show_flag IS NOT FALSE);

DO $$
DECLARE hidden INT;
BEGIN
    SELECT count(*) INTO hidden FROM products p JOIN retire_57 r ON r.id = p.id
    WHERE p.manually_hidden AND NOT p.show_flag;
    RAISE NOTICE 'Скрипт 57: спрени от продажба % от 32 продукта.', hidden;
END $$;

COMMIT;

-- Контрола (след COMMIT, само чете): видими продукти, останали в скрити категории.
WITH RECURSIVE anc AS (SELECT id, id AS leaf, parent_id, show_flag FROM categories
                       UNION ALL
                       SELECT c.id, anc.leaf, c.parent_id, c.show_flag FROM anc JOIN categories c ON c.id = anc.parent_id),
hidden AS (SELECT leaf FROM anc GROUP BY leaf HAVING bool_or(NOT show_flag))
SELECT c.id, c.name_bg, count(*) AS visible_products
FROM hidden h JOIN categories c ON c.id = h.leaf
JOIN products p ON p.category_id = c.id AND p.active AND p.show_flag AND p.status = 'AVAILABLE' AND NOT p.deleted
GROUP BY 1, 2 ORDER BY 3 DESC;

-- ============================================================================
-- ОТКАТ (show_flag се преизчислява от следващия sync):
--   BEGIN;
--   UPDATE products SET manually_hidden = false, updated_at = NOW()
--   WHERE NOT deleted
--     AND (category_id IN (549, 557, 561, 570, 572, 581, 585, 586, 591, 592)
--          OR id IN (19016, 19170, 11408, 11413, 27509, 27512));
--   COMMIT;
-- ============================================================================
