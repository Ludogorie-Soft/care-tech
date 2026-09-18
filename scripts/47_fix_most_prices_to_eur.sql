-- ============================================================================
-- 47_fix_most_prices_to_eur.sql
--
-- Коригира MOST цените от ЛЕВА към ЕВРО.
--
-- ОСНОВАНИЕ (bug-466, SEARCH_AUDIT_PLAN.md фаза 0):
--   MostSyncService:931-933 умножава цената по EUR_TO_BGN_RATE = 1.95583,
--   но MOST feed-ът вече подава евро (<currency>EUR</currency> за 6089/6089
--   продукта, URL-ът изрично иска ?currency=EUR). Конвенцията на системата е
--   price_client / final_price = ЕВРО без ДДС — потвърдено от ASBIS
--   (<CURRENCY_CODE>EUR</CURRENCY_CODE>), VALI и TEKRA.
--   Резултат: 5545 MOST реда са точно 1.95583x завишени.
--
-- ОБХВАТ: само platform = 'MOST'. Останалите три платформи са коректни.
--   price_partner, price_promo, price_client_promo са NULL за всички MOST реда.
--   markup_percentage е 0 за всички MOST реда. 11 реда имат discount > 0.
--   115 реда имат price_client <= 0 — умишлено се прескачат (и без това са
--   невидими по правилото "няма цена -> невидим").
--
-- ВАЛИДАЦИЯ НА ФОРМУЛАТА (изпълнена read-only на прод преди писането на скрипта):
--   формулата за final_price по-долу, приложена върху текущите цени, връща
--   точно текущия final_price за 5429 от 5430 реда. Единственото разминаване е
--   SGM-3010-KKMF1 (база 9.66, формула 9.67) — там формулата съвпада с
--   Product.calculateFinalPrice(), а записаната стойност е остаряла.
--   Скриптът я преизчислява коректно.
--
-- ИДЕМПОТЕНТЕН: пази оригинала в price_client_pre_eur_fix и коригира само
--   редове, където тази колона е още NULL. Повторно пускане не прави нищо.
--
-- ⚠️  ПРЕДИ ДА ПУСНЕШ ТОЗИ СКРИПТ: поправката в MostSyncService.java (фаза 0.1)
--     трябва вече да е деплойната. Иначе следващият нощен sync ще върне
--     старите цени.
--
-- ИЗПОЛЗВАНЕ:
--   psql -h 63.182.239.155 -U techstore_user -d techstore -f scripts/47_fix_most_prices_to_eur.sql
--
-- Дата: 2026-09-18
-- ============================================================================

\set ON_ERROR_STOP on
\timing on

-- ── 0. Backup колона (idempotent) ───────────────────────────────────────────
ALTER TABLE products ADD COLUMN IF NOT EXISTS price_client_pre_eur_fix NUMERIC(10,2);

COMMENT ON COLUMN products.price_client_pre_eur_fix IS
  'Оригиналната (левова) цена преди корекцията от скрипт 47. NULL = не е коригиран.';


-- ── 1. ОТЧЕТ ПРЕДИ ──────────────────────────────────────────────────────────
\echo ''
\echo '════════════════ СЪСТОЯНИЕ ПРЕДИ КОРЕКЦИЯТА ════════════════'

SELECT
    count(*)                                            AS most_redove_obshto,
    count(*) FILTER (WHERE price_client_pre_eur_fix IS NULL
                       AND price_client > 0)            AS shte_bydat_korigirani,
    count(*) FILTER (WHERE price_client_pre_eur_fix IS NOT NULL)
                                                        AS veche_korigirani,
    count(*) FILTER (WHERE price_client IS NULL OR price_client <= 0)
                                                        AS bez_cena_preskachat_se,
    round(min(price_client), 2)                         AS min_cena,
    round(max(price_client), 2)                         AS max_cena
FROM products
WHERE platform = 'MOST';

\echo ''
\echo '-- Контролна извадка (тези стойности трябва да се разделят на 1.95583):'
SELECT sku, left(name_bg, 34) AS name_bg, price_client AS sega,
       round(price_client / 1.95583, 2) AS sled_korekciya
FROM products
WHERE platform = 'MOST' AND price_client_pre_eur_fix IS NULL
  AND sku IN ('C1806A_', '9S7-16RK11-483', 'C9486A_', 'C4931A_', 'RPT-1003DU')
ORDER BY sku;


-- ── 2. КОРЕКЦИЯТА ───────────────────────────────────────────────────────────
BEGIN;

-- 2a. price_client: лева -> евро, оригиналът отива в backup колоната
UPDATE products
SET price_client_pre_eur_fix = price_client,
    price_client             = round(price_client / 1.95583, 2),
    updated_at               = NOW(),
    last_modified_by         = 'script_47_eur_fix'
WHERE platform = 'MOST'
  AND price_client_pre_eur_fix IS NULL
  AND price_client > 0;

-- 2b. final_price: преизчисляване по същата формула като Product.calculateFinalPrice()
--     markup = price_client * round(markup_percentage/100, 2)   [scale 2, както в Java]
--     base   = price_client + markup
--     final  = base * (1 - round(discount/100, 6))  при discount > 0, иначе base
UPDATE products
SET final_price = CASE
        WHEN COALESCE(discount, 0) > 0 THEN
            round(
                (price_client + price_client * round(COALESCE(markup_percentage, 0) / 100.0, 2))
                * (1 - round(discount / 100.0, 6))
            , 2)
        ELSE
            round(
                price_client + price_client * round(COALESCE(markup_percentage, 0) / 100.0, 2)
            , 2)
    END
WHERE platform = 'MOST'
  AND price_client_pre_eur_fix IS NOT NULL
  AND price_client > 0;


-- ── 3. ПРОВЕРКА ПРЕДИ COMMIT ────────────────────────────────────────────────
\echo ''
\echo '════════════════ ПРОВЕРКА (още в транзакция) ════════════════'

\echo '-- Контролната извадка след корекцията:'
SELECT sku, left(name_bg, 34) AS name_bg,
       price_client_pre_eur_fix AS stara_leva,
       price_client             AS nova_evro,
       final_price,
       round(price_client_pre_eur_fix / price_client, 5) AS otnoshenie_trqbva_1_95583
FROM products
WHERE platform = 'MOST'
  AND sku IN ('C1806A_', '9S7-16RK11-483', 'C9486A_', 'C4931A_', 'RPT-1003DU')
ORDER BY sku;

\echo ''
\echo '-- Здравословни проверки (всички трябва да са 0):'
SELECT
    count(*) FILTER (WHERE price_client <= 0)                       AS greshka_nula_cena,
    count(*) FILTER (WHERE final_price IS NULL)                     AS greshka_null_final,
    count(*) FILTER (WHERE round(price_client_pre_eur_fix / price_client, 3) <> 1.956)
                                                                    AS greshka_losho_otnoshenie,
    count(*) FILTER (WHERE COALESCE(discount,0) = 0
                       AND final_price <> price_client)             AS greshka_final_bez_otstupka
FROM products
WHERE platform = 'MOST' AND price_client_pre_eur_fix IS NOT NULL;

\echo ''
\echo '-- Сверка срещу другите платформи (медианата трябва да падне от 1.865 на ~1.0):'
WITH pairs AS (
    SELECT (m.price_client / NULLIF(o.price_client, 0))::numeric AS ratio
    FROM products m
    JOIN products o ON o.sku = m.sku AND o.id <> m.id AND o.platform <> 'MOST'
    WHERE m.platform = 'MOST' AND m.sku IS NOT NULL
      AND m.price_client > 0 AND o.price_client > 0
)
SELECT count(*) AS sraveni_dvoyki,
       round(percentile_cont(0.5) WITHIN GROUP (ORDER BY ratio)::numeric, 3) AS mediana_most_vs_drugi
FROM pairs;

-- Автоматичен предпазител: при провал на която и да е проверка транзакцията
-- се прекъсва с грешка и НИЩО не се записва (ON_ERROR_STOP + RAISE).
DO $$
DECLARE
    v_zero        int;
    v_null_final  int;
    v_bad_ratio   int;
    v_bad_final   int;
    v_median      numeric;
BEGIN
    SELECT count(*) FILTER (WHERE price_client <= 0),
           count(*) FILTER (WHERE final_price IS NULL),
           count(*) FILTER (WHERE round(price_client_pre_eur_fix / price_client, 3) <> 1.956),
           count(*) FILTER (WHERE COALESCE(discount,0) = 0 AND final_price <> price_client)
      INTO v_zero, v_null_final, v_bad_ratio, v_bad_final
      FROM products
     WHERE platform = 'MOST' AND price_client_pre_eur_fix IS NOT NULL;

    IF v_zero > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: %  реда с цена <= 0 след корекцията', v_zero;
    END IF;
    IF v_null_final > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % реда с NULL final_price', v_null_final;
    END IF;
    IF v_bad_ratio > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % реда с отношение старо/ново различно от 1.956', v_bad_ratio;
    END IF;
    IF v_bad_final > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % реда без отстъпка, където final_price <> price_client', v_bad_final;
    END IF;

    SELECT round(percentile_cont(0.5) WITHIN GROUP (ORDER BY ratio)::numeric, 3)
      INTO v_median
      FROM (SELECT (m.price_client / NULLIF(o.price_client, 0))::numeric AS ratio
              FROM products m
              JOIN products o ON o.sku = m.sku AND o.id <> m.id AND o.platform <> 'MOST'
             WHERE m.platform = 'MOST' AND m.sku IS NOT NULL
               AND m.price_client > 0 AND o.price_client > 0) t;

    IF v_median IS NOT NULL AND (v_median < 0.7 OR v_median > 1.4) THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: медианата MOST/други е % — очаква се ~1.0', v_median;
    END IF;

    RAISE NOTICE '✓ Всички проверки минаха. Медиана MOST/други = %. Записва се.', v_median;
END $$;

COMMIT;


-- ── 4. ОТЧЕТ СЛЕД ───────────────────────────────────────────────────────────
\echo ''
\echo '════════════════ СЛЕД КОРЕКЦИЯТА ════════════════'

SELECT
    count(*)                                              AS most_redove,
    count(*) FILTER (WHERE price_client_pre_eur_fix IS NOT NULL) AS korigirani,
    round(min(price_client), 2)                           AS min_cena,
    round(max(price_client), 2)                           AS max_cena
FROM products
WHERE platform = 'MOST';

\echo ''
\echo '-- Колко скрити MOST AVAILABLE продукта вече са най-евтини за своя SKU'
\echo '-- (тези трябва да станат видими след фаза 1 — кодовата поправка на ратчета):'
SELECT count(*) AS gotovi_za_pokazvane
FROM products h
WHERE h.platform = 'MOST'
  AND h.status = 'AVAILABLE' AND h.active AND h.show_flag IS NOT TRUE
  AND h.image_url IS NOT NULL AND h.image_url <> ''
  AND NOT EXISTS (
        SELECT 1 FROM products o
        WHERE o.sku = h.sku AND o.id <> h.id AND o.price_client > 0
          AND o.price_client < h.price_client
          AND o.show_flag AND o.active AND o.status = 'AVAILABLE');


-- ============================================================================
-- СЛЕДВАЩИ СТЪПКИ (НЕ са в този скрипт — умишлено)
--
-- Този скрипт НЕ пуска deduplicateCrossPlatformBySku() и НЕ променя show_flag.
-- Причина: dedup заявката гледа само редове с show_flag = true, затова НЕ МОЖЕ
-- да върне скритите продукти — може само да скрие още. Възстановяването на
-- видимостта иска кодовата поправка от фаза 1 (ратчетът в MostSyncService:963-972
-- и dedup-ът в ProductRepository:225-253).
--
-- Затова редът е: този скрипт -> фаза 1 (код) -> преизчисляване на видимостта.
--
-- ОТКАТ (ако се наложи):
--   BEGIN;
--   UPDATE products
--   SET price_client = price_client_pre_eur_fix,
--       final_price  = CASE
--           WHEN COALESCE(discount,0) > 0 THEN
--               round((price_client_pre_eur_fix
--                     + price_client_pre_eur_fix * round(COALESCE(markup_percentage,0)/100.0, 2))
--                     * (1 - round(discount/100.0, 6)), 2)
--           ELSE round(price_client_pre_eur_fix
--                     + price_client_pre_eur_fix * round(COALESCE(markup_percentage,0)/100.0, 2), 2)
--       END,
--       price_client_pre_eur_fix = NULL
--   WHERE platform = 'MOST' AND price_client_pre_eur_fix IS NOT NULL;
--   COMMIT;
-- ============================================================================
