-- ============================================================
-- Script 26: Изчерпателно почистване на категория Лаптопи (id=37)
-- Покрива всички платформи: VALI, MOST, ASBIS
-- Проверява ЕДНОВРЕМЕННО name_en И name_bg за всяко условие
-- ============================================================

BEGIN;

-- ============================================================
-- 1. Чанти / раници / ръкави → Чанти за лаптопи (id=40)
-- ============================================================
UPDATE products
SET category_id = 40, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%backpack%'    OR name_bg ILIKE '%backpack%'
    OR name_en ILIKE '%back pack%'   OR name_bg ILIKE '%back pack%'
    OR name_en ILIKE '%carry case%'  OR name_bg ILIKE '%carry case%'
    OR name_en ILIKE '%carry bag%'   OR name_bg ILIKE '%carry bag%'
    OR name_en ILIKE '%notebook bag%' OR name_bg ILIKE '%notebook bag%'
    OR name_en ILIKE '%laptop bag%'  OR name_bg ILIKE '%laptop bag%'
    OR name_en ILIKE '%sleeve%'      OR name_bg ILIKE '%sleeve%'
    OR name_en ILIKE '%rolltop%'     OR name_bg ILIKE '%rolltop%'
    OR name_en ILIKE '% bag%'        OR name_bg ILIKE '% bag%'
    OR name_en ILIKE '%чанта%'       OR name_bg ILIKE '%чанта%'
    OR name_en ILIKE '%раница%'      OR name_bg ILIKE '%раница%'
    OR name_en ILIKE '%ранец%'       OR name_bg ILIKE '%ранец%'
  );

-- ============================================================
-- 2. Калъфи / кейсове / протектори → Аксесоари за лаптопи/таблети (id=47)
-- ============================================================
UPDATE products
SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%cover%'           OR name_bg ILIKE '%cover%'
    OR name_en ILIKE '% case%'           OR name_bg ILIKE '% case%'
    OR name_en ILIKE '%case %'           OR name_bg ILIKE '%case %'
    OR name_en ILIKE '%protective%'      OR name_bg ILIKE '%protective%'
    OR name_en ILIKE '%tricover%'        OR name_bg ILIKE '%tricover%'
    OR name_en ILIKE '%versasleave%'     OR name_bg ILIKE '%versasleave%'
    OR name_en ILIKE '%versasleeve%'     OR name_bg ILIKE '%versasleeve%'
    OR name_en ILIKE '%magsmart%'        OR name_bg ILIKE '%magsmart%'
    OR name_en ILIKE '%flip cover%'      OR name_bg ILIKE '%flip cover%'
    OR name_en ILIKE '%folio%'           OR name_bg ILIKE '%folio%'
    OR name_en ILIKE '%simpro%'          OR name_bg ILIKE '%simpro%'
    OR name_en ILIKE '%protect film%'    OR name_bg ILIKE '%protect film%'
    OR name_en ILIKE '%screen protector%' OR name_bg ILIKE '%screen protector%'
    OR name_en ILIKE '%aglr protect%'    OR name_bg ILIKE '%aglr protect%'
    OR name_en ILIKE '%ag protect%'      OR name_bg ILIKE '%ag protect%'
    OR name_en ILIKE '%калъф%'           OR name_bg ILIKE '%калъф%'
    OR name_en ILIKE '%кейс%'            OR name_bg ILIKE '%кейс%'
    OR name_en ILIKE '%протектор%'       OR name_bg ILIKE '%протектор%'
    OR name_en ILIKE '%фолио%'           OR name_bg ILIKE '%фолио%'
  );

-- ============================================================
-- 3. Зарядни / адаптери → Зарядни за лаптопи (id=42)
-- ============================================================
UPDATE products
SET category_id = 42, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%charger%'         OR name_bg ILIKE '%charger%'
    OR name_en ILIKE '%charging%'        OR name_bg ILIKE '%charging%'
    OR name_en ILIKE '% adapter%'        OR name_bg ILIKE '% adapter%'
    OR name_en ILIKE '%ac adapter%'      OR name_bg ILIKE '%ac adapter%'
    OR name_en ILIKE '%power adapter%'   OR name_bg ILIKE '%power adapter%'
    OR name_en ILIKE '%mini adapter%'    OR name_bg ILIKE '%mini adapter%'
    OR name_en ILIKE '%nb adapter%'      OR name_bg ILIKE '%nb adapter%'
    OR name_en ILIKE '%зарядно%'         OR name_bg ILIKE '%зарядно%'
    OR name_en ILIKE '%адаптер%'         OR name_bg ILIKE '%адаптер%'
  );

-- ============================================================
-- 4. Таблети → Таблети (id=39)
-- ============================================================
UPDATE products
SET category_id = 39, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE 'LENOVO TAB%'       OR name_bg ILIKE 'LENOVO TAB%'
    OR name_en ILIKE 'REALME PAD%'       OR name_bg ILIKE 'REALME PAD%'
    OR name_en ILIKE '%tablet%'          OR name_bg ILIKE '%таблет%'
  );

-- ============================================================
-- 5. Докинг станции / хъбове / донгъли → Аксесоари за лаптопи/таблети (id=47)
-- ============================================================
UPDATE products
SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%docking%'         OR name_bg ILIKE '%docking%'
    OR name_en ILIKE '%dock%'            OR name_bg ILIKE '%докинг%'
    OR name_en ILIKE '%dongle%'          OR name_bg ILIKE '%dongle%'
    OR name_en ILIKE '%докинг%'          OR name_bg ILIKE '%dock%'
  );

-- ============================================================
-- 6. Гаранции → скриване
-- ============================================================
UPDATE products
SET show_flag = false, status = 'NOT_AVAILABLE', updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%warranty%'        OR name_bg ILIKE '%warranty%'
    OR name_en ILIKE '% warr%'           OR name_bg ILIKE '% warr%'
    OR name_en ILIKE '%гаранция%'        OR name_bg ILIKE '%гаранция%'
  );

-- ============================================================
-- ПРОВЕРКА
-- ============================================================
SELECT c.id, c.name_bg AS категория, COUNT(p.id) AS всички,
       COUNT(p.id) FILTER (WHERE p.show_flag = true) AS видими
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.id IN (37, 39, 40, 42, 47)
GROUP BY c.id, c.name_bg
ORDER BY c.id;
