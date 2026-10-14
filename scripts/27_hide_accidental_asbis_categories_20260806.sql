-- ============================================================
-- Script 27: Скриване на случайно добавени Asbis категории
-- Причина: Asbis category sync беше стартиран неволно на 2026-08-06
-- Резултат: 430 нови Asbis категории, 13 с show=true (видими на фронтенда)
-- Действие: show=false за ВСИЧКИ Asbis категории, създадени на 2026-08-06
-- ============================================================

-- Преглед преди промяна (покажи засегнатите):
-- SELECT id, name_bg, show, parent_id, created_at
-- FROM categories
-- WHERE platform = 'ASBIS'
--   AND DATE(created_at) = '2026-08-06'
-- ORDER BY show DESC, id;

-- Скриване на всички нови Asbis категории от днес:
UPDATE categories
SET show = false
WHERE platform = 'ASBIS'
  AND DATE(created_at) = '2026-08-06';

-- Проверка след промяна (трябва да върне 0 редове):
-- SELECT id, name_bg, show
-- FROM categories
-- WHERE platform = 'ASBIS'
--   AND DATE(created_at) = '2026-08-06'
--   AND show = true;
