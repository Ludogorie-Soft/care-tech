-- ============================================================
-- Script 18: Почистване на категория Лаптопи (id=37)
-- Аксесоари, чанти, зарядни, фолиа попадат там от Asbis sync.
-- Прехвърля ги в правилните подкатегории и скрива нерелевантните.
-- ============================================================

-- 1. Чанти / раници / калъфи / ръкави → Чанти за лаптопи (id=40)
UPDATE products
SET category_id = 40, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%backpack%'
    OR name_en ILIKE '%carry case%'
    OR name_en ILIKE '%portfolio case%'
    OR name_en ILIKE '%portf case%'
    OR name_en ILIKE '%protective case%'
    OR name_en ILIKE '%sleeve%'
    OR name_en ILIKE '%pocket%'
    OR name_en ILIKE '%bag%'
  );

-- 2. Зарядни → Зарядни за лаптопи (id=42)
UPDATE products
SET category_id = 42, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND name_en ILIKE '%charger%';

-- 3. Докинг / донгъли / фолиа / комплекти → Аксесоари за лаптопи/таблети (id=47)
UPDATE products
SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%docking station%'
    OR name_en ILIKE '%dongle%'
    OR name_en ILIKE '%protect film%'
    OR name_en ILIKE '%kit%'
  );

-- 4. Скриване: гаранции и изложбени продукти
UPDATE products
SET show_flag = false, status = 'NOT_AVAILABLE', updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%warr%'
    OR name_en ILIKE '%warranty%'
    OR name_en ILIKE '%exhibition%'
    OR name_en ILIKE '%disney%'
  );

-- ============================================================
-- Проверка след промените
-- ============================================================
SELECT c.name_bg AS категория, COUNT(*) AS продукти
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.id IN (37, 40, 42, 47)
  AND p.show_flag = true
GROUP BY c.id, c.name_bg
ORDER BY c.id;
