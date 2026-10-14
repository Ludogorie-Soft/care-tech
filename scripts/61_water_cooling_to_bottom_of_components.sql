-- ============================================================================
-- 61_water_cooling_to_bottom_of_components.sql
--
-- Сваля „Водно охлаждане" (23) най-отдолу под „Компютърни компоненти" (1): sort_order 7 → 22.
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
-- ЗАЩО (bug-508): скрипт 49 сложи 23 на свободния слот 7, между „Видео карти" (6) и
--   „Захранвания" (8). Само 23 има видими деца под 1 (Блокове за процесори, Фитинги), а
--   NavDropDown рисува категория с деца като заглавие през цялата ширина (column-span: all).
--   Такова заглавие в средата цепи двуколонния списък на две несвързани части — първите 6
--   категории се подреждат 3 + 3 над него, останалите 14 под децата му.
--   Последната видима е „Входно-изходни контролери" (21), слот 22 е свободен — нищо не се
--   пренарежда.
--
-- ЕДНА КОМАНДА: един DO блок + COMMIT накрая (DBeaver в Manual commit).
-- Откат в края. Дата: 2026-10-02
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

DO $$
DECLARE
    v_max_other INT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM categories
                   WHERE id = 23 AND name_bg = 'Водно охлаждане' AND parent_id = 1 AND show_flag) THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: 23 не е видима категория „Водно охлаждане" под 1.';
    END IF;

    SELECT max(sort_order) INTO v_max_other
      FROM categories WHERE parent_id = 1 AND show_flag AND id <> 23;
    IF v_max_other >= 22 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: под 1 вече има видима категория със sort_order % — 22 не е последен.', v_max_other;
    END IF;

    UPDATE categories SET sort_order = 22, updated_at = NOW()
     WHERE id = 23 AND sort_order <> 22;

    RAISE NOTICE '✓ „Водно охлаждане" е последна под „Компютърни компоненти" (22 след %).', v_max_other;
END $$;

COMMIT;

-- Проверка: 23 трябва да е последният ред.
SELECT sort_order, id, name_bg FROM categories
WHERE parent_id = 1 AND show_flag ORDER BY sort_order DESC LIMIT 3;

-- ============================================================================
-- ОТКАТ
--   UPDATE categories SET sort_order = 7, updated_at = NOW() WHERE id = 23;
--   COMMIT;
-- ============================================================================
