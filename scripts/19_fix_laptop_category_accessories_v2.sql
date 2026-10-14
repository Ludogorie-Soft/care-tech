-- ============================================================
-- Script 19: Допълнително почистване на Лаптопи (id=37)
-- Продължение на скрипт 18 — допълнителни пропуснати патърни.
-- ============================================================

-- 1. Калъфи (PORTFOL CASE, TURN CASE, и всякакви " CASE") → Чанти за лаптопи (id=40)
UPDATE products
SET category_id = 40, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND name_en ILIKE '% CASE%';

-- 2. ASUS раници по BP-номенклатура → Чанти за лаптопи (id=40)
UPDATE products
SET category_id = 40, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND name_en ~* '^ASUS BP[0-9]';

-- 3. Адаптери / захранвания → Зарядни за лаптопи (id=42)
UPDATE products
SET category_id = 42, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%adapt%'
    OR name_en ILIKE '%adap %'
  );

-- 4. Скриване: останали гаранции (SKU започва с SV. — Acer warranty SKU конвенция)
UPDATE products
SET show_flag = false, status = 'NOT_AVAILABLE', updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND sku ILIKE 'SV.%';

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
