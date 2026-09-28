-- ============================================================================
-- 60_asbis_aliases_smart_devices_and_components.sql
--
-- Скритите ASBIS подкатегории стават alias на видимите категории, в които продуктите им трябва да
-- отиват. Решение на потребителя от 2026-09-28 („Смарт + Компоненти“).
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
-- ЗАЩО: ASBIS продуктовият sync избира само видими категории. Скрита подкатегория (напр.
--   „Смарт устройства › Смарт часовник“) означаваше, че продуктът пада в корена — така 225
--   продукта се натрупаха в „Смарт устройства“ (скрипт 59 ги разпредели). От новия бекенд
--   (AsbisCategoryResolver) скрита категория с alias_of_id към видима праща продуктите си там —
--   същият механизъм, който сайтът вече ползва за гейминг категориите 531–543.
--
--   Смарт устройства (500):
--     762 Смарт часовник              → 156 Смарт часовници      (44 продукта във feed-а)
--     761, 864, 951 Детски часовник   → 156 Смарт часовници      (feed-ът вече пише „Kids Watch“ —
--                                        празният дубликат 951 се преименува на „Kids Watch“, 13)
--     765 Външна смарт IP камера      → 100 IP камери            (57)
--     767 Вътрешна смарт IP камера    → 100 IP камери            (40)
--     Сензорите, осветлението, ключовете, хъбовете… остават в „Смарт устройства“.
--   Компютърни компоненти (1) — новите продукти падаха в корена:
--     414 Дънна платка настолна       → 2  Дънни платки          (320)
--     393 Шаси                        → 11 Кутии за компютри     (236)
--     403 Охладител                   → 4  Охладители за процесори (172; ASBIS слага тук и
--                                        термопастите — съществуващите са защитени в 14)
--
-- ПРОВЕРЕНО с днешния feed: без alias-ите новият код дава 0 разлики; с тях се сменят точно
--   882-та продукта от тези 7 групи. Съществуващите са защитени (manually_categorized) освен
--   4 — 2 камери в 500 и 2 кутии в корена 1, които следващият sync ще премести сам.
--
-- ВАЖИ СЛЕД ДЕПЛОЯ на бекенда (стария код alias-ите не засягат — категориите остават скрити
--   и празни). Сайтът показва alias категория само ако е видима — тези са скрити.
--
-- ЕДНА КОМАНДА: един DO блок + COMMIT накрая (DBeaver в Manual commit).
-- Откат в края. Дата: 2026-09-28
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

DO $$
DECLARE
    aliased INT;
BEGIN
    -- Източниците: скрити, празни, без деца, под очаквания корен и без alias досега.
    IF (SELECT count(*) FROM categories c
        WHERE NOT c.show_flag AND c.alias_of_id IS NULL
          AND NOT EXISTS (SELECT 1 FROM products p WHERE p.category_id = c.id AND NOT p.deleted)
          AND NOT EXISTS (SELECT 1 FROM categories k WHERE k.parent_id = c.id)
          AND (c.id, c.name_bg, c.parent_id) IN (
              (762, 'Смарт часовник', 500), (761, 'Детски часовник', 500), (864, 'Детски часовник', 500),
              (951, 'Детски часовник', 500), (765, 'Външна смарт IP камера', 500),
              (767, 'Вътрешна смарт IP камера', 500),
              (414, 'Дънна платка настолна', 1), (393, 'Шаси', 1), (403, 'Охладител', 1))) <> 9 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: някоя от 9-те ASBIS категории не е каквато я очаквам (вече пуснат скрипт?).';
    END IF;
    -- Целите: видими до корена.
    IF (SELECT count(*) FROM categories c
        LEFT JOIN categories p ON p.id = c.parent_id
        LEFT JOIN categories g ON g.id = p.parent_id
        WHERE c.show_flag AND (p.id IS NULL OR p.show_flag) AND (g.id IS NULL OR g.show_flag)
          AND (c.id, c.name_bg) IN ((156, 'Смарт часовници'), (100, 'IP камери'), (2, 'Дънни платки'),
                                    (11, 'Кутии за компютри'), (4, 'Охладители за процесори'))) <> 5 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: някоя целева категория не е видима.';
    END IF;
    IF EXISTS (SELECT 1 FROM categories WHERE parent_id = 500 AND lower(name_bg) = 'kids watch') THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: под 500 вече има „Kids Watch“.';
    END IF;

    UPDATE categories SET name_bg = 'Kids Watch', updated_at = NOW() WHERE id = 951;

    UPDATE categories c
    SET alias_of_id = m.target, updated_at = NOW()
    FROM (VALUES (762, 156), (761, 156), (864, 156), (951, 156), (765, 100), (767, 100),
                 (414, 2), (393, 11), (403, 4)) AS m(source, target)
    WHERE c.id = m.source;
    GET DIAGNOSTICS aliased = ROW_COUNT;

    RAISE NOTICE 'Скрипт 60: % ASBIS категории станаха alias (важи след деплоя на бекенда); 951 е „Kids Watch“.', aliased;
END $$;

-- Контрола (само чете).
SELECT c.id, c.name_bg, c.parent_id, c.show_flag, c.alias_of_id, t.name_bg AS goes_to
FROM categories c JOIN categories t ON t.id = c.alias_of_id
WHERE c.id IN (762, 761, 864, 951, 765, 767, 414, 393, 403)
ORDER BY c.parent_id, c.id;

-- В DBeaver с Manual commit горното чака потвърждение и след 60 s сървърът го отменя.
-- В auto-commit/psql COMMIT е безвреден.
COMMIT;

-- ============================================================================
-- ОТКАТ:
--   BEGIN;
--   UPDATE categories SET alias_of_id = NULL WHERE id IN (762, 761, 864, 951, 765, 767, 414, 393, 403);
--   UPDATE categories SET name_bg = 'Детски часовник' WHERE id = 951;
--   COMMIT;
-- ============================================================================
