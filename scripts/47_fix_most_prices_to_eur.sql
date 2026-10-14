-- ============================================================================
-- 47_fix_most_prices_to_eur.sql
--
-- Коригира MOST цените от ЛЕВА към ЕВРО.
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент, и през psql.
--
-- ОСНОВАНИЕ (bug-466, SEARCH_AUDIT_PLAN.md фаза 0.4):
--   MostSyncService умножаваше цената по курса лев/евро 1.95583, но MOST
--   feed-ът вече подава евро (<currency>EUR</currency> за 6089/6089 продукта).
--   Конвенцията на системата е price_client / final_price = ЕВРО без ДДС —
--   потвърдено от ASBIS (<CURRENCY_CODE>EUR</CURRENCY_CODE>), VALI и TEKRA.
--   Резултат: 5430 MOST реда с цена > 0 са точно 1.95583x завишени.
--
-- ОБХВАТ: само platform = 'MOST'. Останалите три платформи са коректни.
--   price_partner, price_promo, price_client_promo са NULL за всички MOST реда.
--   markup_percentage е 0 за всички MOST реда. 11 реда имат discount > 0.
--   115 реда имат price_client <= 0 — умишлено се прескачат.
--
-- ИДЕМПОТЕНТЕН: пази оригинала в price_client_pre_eur_fix и коригира само
--   редове, където тази колона е още NULL. Повторно пускане не прави нищо.
--
-- ============================================================================
-- КАК СЕ ПУСКА
--   Изпълни целия файл като СКРИПТ (DBeaver: Alt+X / "Execute script";
--   DataGrip: Run file; psql: -f <файл>). НЕ като единична заявка.
--   Секция A слага предпазни таймаути, така че скриптът да не може да блокира
--   сайта дори ако клиентът е в manual-commit режим.
-- ============================================================================


-- ─── СЕКЦИЯ A: предпазни таймаути за сесията ────────────────────────────────
-- ВАЖНО: ALTER TABLE взима ACCESS EXCLUSIVE lock върху products — най-силният
-- лок, който блокира дори четене. Ако клиент в manual-commit режим го държи
-- незакоммитен, целият сайт спира (това се случи при първия опит: 22 заявки
-- на приложението блокирани за 6 минути).
--   lock_timeout                        -> ALTER-ът се отказва бързо, вместо да
--                                          застане на опашка и да блокира и
--                                          всички следващи четения зад себе си
--   idle_in_transaction_session_timeout -> ако въпреки всичко остане отворена
--                                          транзакция, сървърът я прекъсва сам

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';
SET statement_timeout = '300s';


-- ─── СЕКЦИЯ B: backup колона, в собствена транзакция ────────────────────────
-- Изрично BEGIN/COMMIT, за да се освободи ACCESS EXCLUSIVE веднага,
-- независимо от auto-commit настройката на клиента.

BEGIN;

ALTER TABLE products ADD COLUMN IF NOT EXISTS price_client_pre_eur_fix NUMERIC(10,2);

COMMENT ON COLUMN products.price_client_pre_eur_fix IS
  'Оригиналната (левова) цена преди корекцията от скрипт 47. NULL = не е коригиран.';

COMMIT;


-- ─── СЕКЦИЯ C: отчет ПРЕДИ ──────────────────────────────────────────────────
-- Очаквано при първо пускане: shte_bydat_korigirani = 5430, veche_korigirani = 0

SELECT
    'ПРЕДИ'                                             AS etap,
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

-- Контролна извадка — тези стойности трябва да се разделят на 1.95583
SELECT sku,
       left(name_bg, 34)                  AS name_bg,
       price_client                       AS sega,
       round(price_client / 1.95583, 2)   AS sled_korekciya
FROM products
WHERE platform = 'MOST'
  AND price_client_pre_eur_fix IS NULL
  AND sku IN ('C1806A_', '9S7-16RK11-483', 'C9486A_', 'C4931A_', 'RPT-1003DU')
ORDER BY sku;


-- ─── СЕКЦИЯ D: корекцията (в транзакция с проверка) ─────────────────────────

BEGIN;

-- D1. price_client: лева -> евро, оригиналът отива в backup колоната
UPDATE products
SET price_client_pre_eur_fix = price_client,
    price_client             = round(price_client / 1.95583, 2),
    updated_at               = NOW(),
    last_modified_by         = 'script_47_eur_fix'
WHERE platform = 'MOST'
  AND price_client_pre_eur_fix IS NULL
  AND price_client > 0;

-- D2. final_price: преизчисляване по същата формула като Product.calculateFinalPrice()
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

-- D3. Автоматичен предпазител: при провал хвърля изключение, транзакцията става
--     aborted и COMMIT-ът действа като ROLLBACK — нищо не се записва.
--
--     ЗАБЕЛЕЖКА ЗА ПРОВЕРКАТА НА ТОЧНОСТТА: сравнява се АБСОЛЮТНИЯТ остатък
--     |старо - ново*1.95583|, НЕ отношението старо/ново. Отношението е негодна
--     проверка, защото ново е закръглено до 2 знака и при малки цени това
--     изкривява отношението: 0.02 -> 0.01 дава отношение 2.00, а не 1.956,
--     макар грешката да е под половин стотинка. Проверено на прод: максималният
--     абсолютен остатък за всичките 5430 реда е 0.005, тоест толеранс 0.01
--     (= 0.005 * 1.95583, закръглено нагоре) е точната граница.

DO $$
DECLARE
    v_korigirani  int;
    v_zero        int;
    v_null_final  int;
    v_bad_amount  int;
    v_bad_final   int;
    v_max_greshka numeric;
    v_median      numeric;
BEGIN
    SELECT count(*),
           count(*) FILTER (WHERE price_client <= 0),
           count(*) FILTER (WHERE final_price IS NULL),
           count(*) FILTER (WHERE abs(price_client_pre_eur_fix - price_client * 1.95583) > 0.01),
           count(*) FILTER (WHERE COALESCE(discount,0) = 0 AND final_price <> price_client),
           max(abs(price_client_pre_eur_fix - price_client * 1.95583))
      INTO v_korigirani, v_zero, v_null_final, v_bad_amount, v_bad_final, v_max_greshka
      FROM products
     WHERE platform = 'MOST' AND price_client_pre_eur_fix IS NOT NULL;

    IF v_korigirani = 0 THEN
        RAISE NOTICE 'Няма нищо за коригиране — вече е приложено. Нищо не се променя.';
        RETURN;
    END IF;

    IF v_zero > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % реда с цена <= 0 след корекцията', v_zero;
    END IF;
    IF v_null_final > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % реда с NULL final_price', v_null_final;
    END IF;
    IF v_bad_amount > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % реда, където |старо - ново*1.95583| > 0.01 (макс. %)',
                        v_bad_amount, v_max_greshka;
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

    RAISE NOTICE '✓ Проверките минаха. Коригирани % реда, макс. отклонение %, медиана MOST/други %.',
                 v_korigirani, v_max_greshka, v_median;
END $$;

COMMIT;


-- ─── СЕКЦИЯ E: отчет СЛЕД ───────────────────────────────────────────────────

SELECT
    'СЛЕД'                                                        AS etap,
    count(*)                                                      AS most_redove,
    count(*) FILTER (WHERE price_client_pre_eur_fix IS NOT NULL)  AS korigirani,
    round(min(price_client), 2)                                   AS min_cena,
    round(max(price_client), 2)                                   AS max_cena
FROM products
WHERE platform = 'MOST';

-- Контролната извадка след корекцията
SELECT sku,
       left(name_bg, 34)                                  AS name_bg,
       price_client_pre_eur_fix                           AS stara_leva,
       price_client                                       AS nova_evro,
       final_price,
       round(price_client_pre_eur_fix / price_client, 5)   AS otnoshenie
FROM products
WHERE platform = 'MOST'
  AND sku IN ('C1806A_', '9S7-16RK11-483', 'C9486A_', 'C4931A_', 'RPT-1003DU')
ORDER BY sku;

-- Сверка срещу другите платформи — медианата трябва да е паднала от 1.846 на ~1.0
WITH pairs AS (
    SELECT (m.price_client / NULLIF(o.price_client, 0))::numeric AS ratio
    FROM products m
    JOIN products o ON o.sku = m.sku AND o.id <> m.id AND o.platform <> 'MOST'
    WHERE m.platform = 'MOST' AND m.sku IS NOT NULL
      AND m.price_client > 0 AND o.price_client > 0
)
SELECT count(*)                                                              AS sraveni_dvoyki,
       round(percentile_cont(0.5) WITHIN GROUP (ORDER BY ratio)::numeric, 3) AS mediana_most_vs_drugi
FROM pairs;

-- Колко скрити MOST AVAILABLE продукта вече са най-евтини за своя SKU.
-- Тези трябва да станат видими след фаза 1 (кодовата поправка на ратчетите).
-- Преди корекцията: 272. Очаквано след нея: ~384.
SELECT count(*) AS gotovi_za_pokazvane_sled_faza_1
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
-- видимостта иска кодовата поправка от фаза 1.
--
-- ОТКАТ (ако се наложи) — пусни само този блок:
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
