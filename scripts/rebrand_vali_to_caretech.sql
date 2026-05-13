-- =============================================================================
-- Ребрандиране: VALI COMPUTERS → CARETECH
-- 1. Преименува марката
-- 2. Заменя "VALI" с "CARETECH" в наименованията на продуктите
-- Изпълни ръчно: psql -U <user> -d <db> -f rebrand_vali_to_caretech.sql
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. Преименуване на марката
-- =============================================================================

UPDATE manufacturers
SET name            = 'CARETECH',
    updated_at      = NOW(),
    last_modified_by = 'system'
WHERE name LIKE '%VALI%';

-- =============================================================================
-- 2. Замяна на "VALI" с "CARETECH" в наименованията на продуктите
--    Засяга само продукти, чиято марка вече е CARETECH (след горната стъпка)
-- =============================================================================

UPDATE products p
SET name_bg         = REGEXP_REPLACE(name_bg, 'VALI', 'CARETECH', 'gi'),
    name_en         = REGEXP_REPLACE(name_en, 'VALI', 'CARETECH', 'gi'),
    updated_at      = NOW(),
    last_modified_by = 'system'
FROM manufacturers m
WHERE p.manufacturer_id = m.id
  AND m.name = 'CARETECH'
  AND (p.name_bg ILIKE '%VALI%' OR p.name_en ILIKE '%VALI%');

-- =============================================================================
-- Проверка
-- =============================================================================

SELECT
    m.name              AS manufacturer,
    COUNT(p.id)         AS total_products,
    COUNT(p.id) FILTER (WHERE p.name_bg ILIKE '%VALI%' OR p.name_en ILIKE '%VALI%') AS still_contain_vali
FROM manufacturers m
JOIN products p ON p.manufacturer_id = m.id
WHERE m.name = 'CARETECH'
GROUP BY m.name;

COMMIT;
