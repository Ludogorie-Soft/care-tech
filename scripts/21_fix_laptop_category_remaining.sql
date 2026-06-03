-- ============================================================
-- Script 21: Финално почистване на Лаптопи (id=37)
-- Останали след скриптове 18-20.
-- ============================================================

-- 1. Чанти → Чанти за лаптопи (id=40)
--    LENOVO T210 = ThinkPad Essential bag series
UPDATE products
SET category_id = 40, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND name_en ILIKE 'LENOVO T2%';

-- 2. Folio клавиатури + стилуси (SIMPRO) → Аксесоари за лаптопи/таблети (id=47)
UPDATE products
SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%folio%'
    OR name_en ILIKE '%simpro%'
  );

-- 3. ASUS ROG AC зарядни → Зарядни за лаптопи (id=42)
UPDATE products
SET category_id = 42, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND name_en ~* '^ASUS ROG AC';

-- 4. Докинг станции (вкл. HYBRID DOCK) → Аксесоари за лаптопи/таблети (id=47)
UPDATE products
SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%dock%'
  );

-- 5. Таблети → скриване (няма отделна категория Таблети)
UPDATE products
SET show_flag = false, status = 'NOT_AVAILABLE', updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE 'LENOVO TAB%'
    OR name_en ILIKE 'REALME PAD%'
    OR name_en ILIKE '%tablet%'
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
