-- ============================================================
-- Script 20: Калъфи с "COVER" или "CASE" в името → Чанти (id=40)
-- Пропуснати от скрипт 18/19:
--   COVER: TRICOVER, FLIPCOVER, DUOCOVER и др.
--   CASE:  SLOT-IN-CASE (дефис, не интервал — пропуснато от '% CASE%')
-- ============================================================

UPDATE products
SET category_id = 40, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%cover%'
    OR name_en ILIKE '%case%'
  );

-- Проверка
SELECT c.name_bg AS категория, COUNT(*) AS продукти
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.id IN (37, 40)
  AND p.show_flag = true
GROUP BY c.id, c.name_bg
ORDER BY c.id;
