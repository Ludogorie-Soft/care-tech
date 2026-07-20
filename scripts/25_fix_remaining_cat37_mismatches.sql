-- ============================================================
-- Script 25: Остатъчни несъответствия в категория Лаптопи (id=37)
-- Поправя пропуснатите от Script 24 случаи:
--   - ASUS HD7 PERS.COVER, ASUS SIDE FLIP COVER и др. с "COVER"
--   - Калъфи за таблети с имена, непокрити от Script 24
-- ============================================================

BEGIN;

-- 1. Кейсове/калъфи с "COVER" в името → Аксесоари за лаптопи/таблети (id=47)
--    Примери: ASUS HD7 PERS.COVER PB/PINK/YG, ASUS SIDE FLIP COVER,
--             ASUS TRAVEL COVER, ASUS VERSASLEAVE X /WH COVER
--    В cat 37 легитимните лаптопи нямат "cover" в name_en или name_bg
UPDATE products
SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%cover%'
    OR name_bg ILIKE '%cover%'
    OR name_bg ILIKE '%калъф%'
    OR name_bg ILIKE '%кейс%'
  );

-- 2. Калъфи с "CASE" в името → Аксесоари за лаптопи/таблети (id=47)
--    Примери: ACER NEO CASE, ASUS TRANS CASE, LENOVO CASE
--    Изключваме "SHOWCAS" и "STAIRCASE" (edge cases)
UPDATE products
SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%case%'
    OR name_bg ILIKE '%case%'
  )
  AND name_en NOT ILIKE '%showcase%'
  AND name_en NOT ILIKE '%staircase%';

-- ============================================================
-- ПРОВЕРКА
-- ============================================================
SELECT c.id, c.name_bg AS категория, COUNT(p.id) AS продукти
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.id IN (37, 40, 47)
GROUP BY c.id, c.name_bg
ORDER BY c.id;
