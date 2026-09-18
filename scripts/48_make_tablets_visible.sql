-- ============================================================================
-- 48_make_tablets_visible.sql
--
-- Прави категория „Таблети" видима и я премества под „Лаптопи, таблети и аксесоари",
-- след което слива в нея продуктите от двете ѝ деца.
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
-- ОСНОВАНИЕ (CATEGORY_CLEANUP_PLAN.md, фаза 5):
--   Категория 744 „Таблети" е коренова, НЕВИДИМА, създадена от ASBIS като родител
--   от ниво 1. В нея има 19 MOST продукта, попаднали там през правилото
--   LAPTOP_CATEGORY_NAME_OVERRIDES → „Таблети". Двете ѝ деца — 745 „PC таблет" (48
--   продукта) и 746 „PC таблет с Windows" (1) — също са невидими.
--   Общо 68 продукта, до които не се стига с разглеждане.
--
-- ⚠️ ПРЕДИ ДА ПУСНЕШ: промяната в AsbisSyncService трябва да е деплойната.
--   Тя добавя проверка дали категория с това име вече съществува НЯКЪДЕ в дървото,
--   преди да създаде нова коренова. Без нея следващият ASBIS sync няма да намери
--   „Таблети" сред кореновите (защото я местим) и ще създаде нова — при това
--   createCategory слага show = true и sort_order = 0, тоест нов ВИДИМ корен най-отгоре
--   в менюто.
--
-- ЗАПАЗЕНИ ОГРАНИЧЕНИЯ:
--   • sort_order = 7 е СВОБОДЕН слот под родител 36 (редът минава 6 → 8).
--     Нито една съществуваща категория не се пренарежда.
--   • Йерархията остава три нива: 744 става второ ниво под корена 36.
--   • 745 и 746 остават скрити и празни — не се трият, за да е обратимо.
--
-- ИДЕМПОТЕНТЕН: всяка стъпка проверява текущото състояние преди да пише.
--
-- ОТКАТ: в края на файла.
--
-- Дата: 2026-09-18
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';
SET statement_timeout = '120s';


-- ─── СЕКЦИЯ A: отчет ПРЕДИ ──────────────────────────────────────────────────

SELECT 'ПРЕДИ' AS etap, c.id, c.name_bg, c.platform, c.show_flag, c.sort_order,
       c.parent_id, c.category_path,
       (SELECT count(*) FROM products p WHERE p.category_id = c.id) AS produkti
FROM categories c WHERE c.id IN (36, 744, 745, 746) ORDER BY c.id;

-- Контрола: слот 7 под родител 36 трябва да е свободен
SELECT sort_order, count(*) AS kategorii
FROM categories WHERE parent_id = 36 AND show_flag
GROUP BY sort_order ORDER BY sort_order;


-- ─── СЕКЦИЯ B: резервна колона за преместените продукти ─────────────────────
-- Отделна транзакция, за да се освободи ACCESS EXCLUSIVE веднага.

BEGIN;

ALTER TABLE products ADD COLUMN IF NOT EXISTS category_id_pre_phase5 BIGINT;

COMMENT ON COLUMN products.category_id_pre_phase5 IS
  'Категорията преди преместванията от фаза 5. NULL = продуктът не е местен.';

COMMIT;


-- ─── СЕКЦИЯ C: промените ────────────────────────────────────────────────────

BEGIN;

-- C1. „Таблети" става видима подкатегория на „Лаптопи, таблети и аксесоари"
UPDATE categories
SET parent_id     = 36,
    show_flag     = true,
    sort_order    = 7,
    category_path = 'laptops-tablets-and-accessories/tableti',
    updated_at    = NOW()
WHERE id = 744
  AND (parent_id IS DISTINCT FROM 36 OR show_flag IS NOT TRUE);

-- C2. Пътят на двете деца следва новия път на родителя
UPDATE categories
SET category_path = 'laptops-tablets-and-accessories/tableti/' || slug,
    updated_at    = NOW()
WHERE parent_id = 744
  AND category_path IS DISTINCT FROM 'laptops-tablets-and-accessories/tableti/' || slug;

-- C3. Продуктите от двете деца се сливат в „Таблети"
UPDATE products
SET category_id_pre_phase5 = category_id,
    category_id            = 744,
    updated_at             = NOW()
WHERE category_id IN (745, 746)
  AND category_id_pre_phase5 IS NULL;


-- ─── СЕКЦИЯ D: предпазител ──────────────────────────────────────────────────
-- При провал транзакцията става aborted и COMMIT действа като ROLLBACK.

DO $$
DECLARE
    v_parent      bigint;
    v_visible     boolean;
    v_sort        int;
    v_sort_clash  int;
    v_levels      int;
    v_leftover    int;
    v_tablets     int;
BEGIN
    SELECT parent_id, show_flag, sort_order INTO v_parent, v_visible, v_sort
      FROM categories WHERE id = 744;

    IF v_parent IS DISTINCT FROM 36 OR v_visible IS NOT TRUE THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: 744 не е видима подкатегория на 36 (parent=%, show=%)', v_parent, v_visible;
    END IF;

    -- Никоя друга видима категория под 36 не бива да дели sort_order с нея
    SELECT count(*) INTO v_sort_clash
      FROM categories WHERE parent_id = 36 AND show_flag AND sort_order = v_sort AND id <> 744;
    IF v_sort_clash > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: sort_order % вече се ползва от % друга категория под 36', v_sort, v_sort_clash;
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

    SELECT count(*) INTO v_leftover FROM products WHERE category_id IN (745, 746);
    IF v_leftover > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % продукта още са в 745/746', v_leftover;
    END IF;

    SELECT count(*) INTO v_tablets FROM products WHERE category_id = 744;
    RAISE NOTICE '✓ Проверките минаха. „Таблети" е видима под 36 на позиция %, съдържа % продукта.',
                 v_sort, v_tablets;
END $$;

COMMIT;


-- ─── СЕКЦИЯ E: отчет СЛЕД ───────────────────────────────────────────────────

SELECT 'СЛЕД' AS etap, c.id, c.name_bg, c.show_flag, c.sort_order, c.parent_id,
       c.category_path,
       (SELECT count(*) FROM products p WHERE p.category_id = c.id) AS produkti
FROM categories c WHERE c.id IN (744, 745, 746) ORDER BY c.id;

-- Подредбата под „Лаптопи, таблети и аксесоари" — 7 трябва да е между 6 и 8
SELECT sort_order, name_bg,
       (SELECT count(*) FROM products p WHERE p.category_id = c.id) AS produkti
FROM categories c WHERE parent_id = 36 AND show_flag ORDER BY sort_order, name_bg;

-- Колко от тях са реално видими продукти
SELECT count(*) AS vidimi_produkti_v_tableti
FROM products p
WHERE p.category_id = 744
  AND p.active AND p.show_flag AND p.status = 'AVAILABLE'
  AND p.deleted = false AND p.image_url IS NOT NULL AND p.image_url <> '';


-- ============================================================================
-- ОТКАТ
--   BEGIN;
--   UPDATE products
--   SET category_id = category_id_pre_phase5, category_id_pre_phase5 = NULL
--   WHERE category_id_pre_phase5 IS NOT NULL;
--
--   UPDATE categories
--   SET parent_id = NULL, show_flag = false, sort_order = 0, category_path = 'tableti'
--   WHERE id = 744;
--
--   UPDATE categories
--   SET category_path = 'tableti/' || slug WHERE parent_id = 744;
--   COMMIT;
-- ============================================================================
