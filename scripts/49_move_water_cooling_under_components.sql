-- ============================================================================
-- 49_move_water_cooling_under_components.sql
--
-- Премества „Водно охлаждане" от корена под „Компютърни компоненти".
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
-- ОСНОВАНИЕ (bug-507):
--   Категория 23 „Водно охлаждане" е КОРЕНОВА в главното меню със sort_order 21,
--   между „Софтуер" (20) и „VR - Виртуална реалност" (22). Докато беше празна,
--   мястото ѝ нямаше значение. След стъпка 2а получи 40 продукта и вече личи.
--   Логически принадлежи до останалите охлаждащи категории, които са под
--   „Компютърни компоненти" (id 1): „Охладители за процесори" (sort 3),
--   „Вентилатори" (15), „Контролери за вентилатори" (16), „Термо пасти" (17).
--
-- ЗАПАЗЕНИ ОГРАНИЧЕНИЯ:
--   • sort_order = 7 е СВОБОДЕН слот под родител 1 (редът минава 6 → 8).
--     Нито една съществуваща категория не се пренарежда.
--   • Йерархията остава три нива: 23 минава на второ ниво, а 7-те ѝ деца —
--     на трето. Проверено: децата НЯМАТ свои деца.
--   • `category_path` НЕ се пипа — всички VALI категории в това поддърво имат
--     празен път, такава е конвенцията там.
--
-- ЗАЩО VALI SYNC НЯМА ДА ГО ВЪРНЕ:
--   ValiSyncService.updateCategoryParentsOptimized() задава родител САМО когато
--   текущият е NULL. След тази промяна 23 има родител, значи VALI повече не го
--   докосва. `show_flag` и `sort_order` се задават единствено при СЪЗДАВАНЕ
--   (createCategoryFromExternal), не при обновяване.
--
-- СЪСТОЯНИЕ ПРЕДИ (за откат):
--   id 23: parent_id = NULL, sort_order = 21, show_flag = true
--
-- ИДЕМПОТЕНТЕН: проверява текущото състояние преди да пише.
--
-- Дата: 2026-09-18
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';
SET statement_timeout = '120s';


-- ─── СЕКЦИЯ A: отчет ПРЕДИ ──────────────────────────────────────────────────

SELECT 'ПРЕДИ' AS etap, c.id, c.name_bg, c.show_flag, c.sort_order, c.parent_id,
       (SELECT count(*) FROM products p WHERE p.category_id = c.id) AS produkti,
       (SELECT count(*) FROM categories x WHERE x.parent_id = c.id) AS deca
FROM categories c WHERE c.id = 23;

-- Слот 7 под „Компютърни компоненти" трябва да е свободен
SELECT sort_order, name_bg FROM categories
WHERE show_flag AND parent_id = 1 AND sort_order BETWEEN 5 AND 9
ORDER BY sort_order;


-- ─── СЕКЦИЯ B: промяната ────────────────────────────────────────────────────

BEGIN;

UPDATE categories
SET parent_id  = 1,
    sort_order = 7,
    updated_at = NOW()
WHERE id = 23
  AND parent_id IS DISTINCT FROM 1;


-- ─── СЕКЦИЯ C: предпазител ──────────────────────────────────────────────────
-- При провал транзакцията става aborted и COMMIT действа като ROLLBACK.

DO $$
DECLARE
    v_parent     bigint;
    v_sort       int;
    v_visible    boolean;
    v_clash      int;
    v_levels     int;
    v_orphans    int;
    v_deca       int;
BEGIN
    SELECT parent_id, sort_order, show_flag INTO v_parent, v_sort, v_visible
      FROM categories WHERE id = 23;

    IF v_parent IS DISTINCT FROM 1 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: 23 не е под „Компютърни компоненти" (parent=%)', v_parent;
    END IF;
    IF v_visible IS NOT TRUE THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: 23 вече не е видима';
    END IF;

    -- Никоя друга видима категория под 1 не дели този sort_order
    SELECT count(*) INTO v_clash
      FROM categories WHERE parent_id = 1 AND show_flag AND sort_order = v_sort AND id <> 23;
    IF v_clash > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: sort_order % се ползва от още % категории под 1', v_sort, v_clash;
    END IF;

    -- Дървото остава точно три нива
    WITH RECURSIVE t AS (
        SELECT id, 1 AS nivo FROM categories WHERE parent_id IS NULL
        UNION ALL
        SELECT c.id, t.nivo + 1 FROM categories c JOIN t ON c.parent_id = t.id
    )
    SELECT max(nivo) INTO v_levels FROM t;
    IF v_levels <> 3 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: дървото стана % нива вместо 3', v_levels;
    END IF;

    -- Нито една видима подкатегория без родител
    SELECT count(*) INTO v_orphans
      FROM categories c WHERE c.show_flag AND c.parent_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM categories p WHERE p.id = c.parent_id);
    IF v_orphans > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % видими подкатегории са останали без родител', v_orphans;
    END IF;

    SELECT count(*) INTO v_deca FROM categories WHERE parent_id = 23;
    RAISE NOTICE '✓ Проверките минаха. „Водно охлаждане" е под „Компютърни компоненти" на позиция %, с % подкатегории.',
                 v_sort, v_deca;
END $$;

COMMIT;


-- ─── СЕКЦИЯ D: отчет СЛЕД ───────────────────────────────────────────────────

-- Подредбата под „Компютърни компоненти" — 7 трябва да е между 6 и 8
SELECT sort_order, name_bg,
       (SELECT count(*) FROM products p WHERE p.category_id = c.id) AS produkti
FROM categories c WHERE show_flag AND parent_id = 1 ORDER BY sort_order, name_bg;

-- Кореновото меню вече без „Водно охлаждане"
SELECT sort_order, name_bg FROM categories
WHERE show_flag AND parent_id IS NULL AND sort_order BETWEEN 19 AND 24
ORDER BY sort_order;

-- Поддървото на водното охлаждане е непокътнато
SELECT c.id, c.name_bg, c.show_flag, c.sort_order,
       (SELECT count(*) FROM products p WHERE p.category_id = c.id) AS produkti
FROM categories c WHERE c.parent_id = 23 ORDER BY c.sort_order;


-- ============================================================================
-- ОТКАТ
--   BEGIN;
--   UPDATE categories SET parent_id = NULL, sort_order = 21 WHERE id = 23;
--   COMMIT;
-- ============================================================================
