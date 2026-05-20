-- =============================================================================
-- Скрипт за активиране на филтри за Asbis продукти
-- Изпълни СЛЕД POST /api/sync/asbis/parameters (трябва да има синхронизирани параметри)
-- НЕ е Flyway миграция — изпълнява се ръчно след Asbis sync
--
-- Стратегия:
--   1. Reset: is_filter = false за всички Asbis category_parameters
--   2. Blacklist: изключва глобални/опаковъчни параметри (ЕАН, тегла, размери на кутия и др.)
--   3. Auto-select: is_filter = true за параметри с брой опции МЕЖДУ 2 И 50
--      (достатъчно опции за полезен филтър, но не твърде много)
-- =============================================================================

BEGIN;

-- =============================================================================
-- Стъпка 1: Reset всички Asbis category_parameters
-- =============================================================================
UPDATE category_parameters cp
SET    is_filter = false
FROM   parameters p
WHERE  cp.parameter_id = p.id
  AND  p.platform = 'ASBIS';

-- =============================================================================
-- Стъпка 2: Активиране на подходящите филтри
--   Критерии:
--   - platform = 'ASBIS'
--   - брой опции (от parameter_options) BETWEEN 2 AND 50
--   - Не попада в blacklist с глобални/опаковъчни параметри
-- =============================================================================
UPDATE category_parameters cp
SET    is_filter = true
FROM   parameters p
WHERE  cp.parameter_id = p.id
  AND  p.platform = 'ASBIS'
  AND  (SELECT COUNT(*) FROM parameter_options po WHERE po.parameter_id = p.id) BETWEEN 2 AND 50
  AND  p.name_bg NOT ILIKE '%EAN%'
  AND  p.name_bg NOT ILIKE '%баркод%'
  AND  p.name_bg NOT ILIKE '%тегло на опаковк%'
  AND  p.name_bg NOT ILIKE '%ширина на опаковк%'
  AND  p.name_bg NOT ILIKE '%дълбочина на опаковк%'
  AND  p.name_bg NOT ILIKE '%дължина на опаковк%'
  AND  p.name_bg NOT ILIKE '%височина на опаковк%'
  AND  p.name_bg NOT ILIKE '%обем на опаковк%'
  AND  p.name_bg NOT ILIKE '%нетно тегло%'
  AND  p.name_bg NOT ILIKE '%бруто тегло%'
  AND  p.name_bg NOT ILIKE '%размери на опаковк%'
  AND  p.name_bg NOT ILIKE '%размер на кутия%'
  AND  p.name_bg NOT ILIKE '%размер на опаковк%'
  AND  p.name_bg NOT ILIKE '%ширина на кутия%'
  AND  p.name_bg NOT ILIKE '%дълбочина на кутия%'
  AND  p.name_bg NOT ILIKE '%дължина на кутия%'
  AND  p.name_bg NOT ILIKE '%височина на кутия%'
  AND  p.name_bg NOT ILIKE '%брой в кашон%'
  AND  p.name_bg NOT ILIKE '%брой в кутия%'
  AND  p.name_bg NOT ILIKE '%master carton%'
  AND  p.name_bg NOT ILIKE '%inner box%'
  AND  p.name_bg NOT ILIKE 'PackHeight%'
  AND  p.name_bg NOT ILIKE 'PackWidth%'
  AND  p.name_bg NOT ILIKE 'PackDepth%'
  AND  p.name_bg NOT ILIKE 'PackWeight%'
  AND  p.name_bg NOT ILIKE '%тарифен номер%'
  AND  p.name_bg NOT ILIKE '%хармонизиран код%'
  AND  p.name_bg NOT ILIKE '%HS код%'
  AND  p.name_bg NOT ILIKE '%TARIC%'
  AND  p.name_bg NOT ILIKE '%произход%'
  AND  p.name_bg NOT ILIKE '%country of origin%'
  AND  (p.name_en IS NULL
        OR (
           p.name_en NOT ILIKE '%EAN%'
           AND p.name_en NOT ILIKE '%barcode%'
           AND p.name_en NOT ILIKE '%package weight%'
           AND p.name_en NOT ILIKE '%package width%'
           AND p.name_en NOT ILIKE '%package depth%'
           AND p.name_en NOT ILIKE '%package length%'
           AND p.name_en NOT ILIKE '%package height%'
           AND p.name_en NOT ILIKE '%package volume%'
           AND p.name_en NOT ILIKE '%net weight%'
           AND p.name_en NOT ILIKE '%gross weight%'
           AND p.name_en NOT ILIKE '%box weight%'
           AND p.name_en NOT ILIKE '%box width%'
           AND p.name_en NOT ILIKE '%box depth%'
           AND p.name_en NOT ILIKE '%box length%'
           AND p.name_en NOT ILIKE '%box height%'
           AND p.name_en NOT ILIKE '%pcs per carton%'
           AND p.name_en NOT ILIKE '%pcs per box%'
           AND p.name_en NOT ILIKE '%master carton%'
           AND p.name_en NOT ILIKE '%inner box%'
           AND p.name_en NOT ILIKE '%harmonized%'
           AND p.name_en NOT ILIKE '%tariff%'
           AND p.name_en NOT ILIKE '%country of origin%'
        )
  );

-- =============================================================================
-- Проверка на резултата
-- =============================================================================
SELECT
    c.name_bg                                                                    AS category,
    p.name_bg                                                                    AS parameter,
    (SELECT COUNT(*) FROM parameter_options po WHERE po.parameter_id = p.id)    AS option_count,
    cp.is_filter
FROM category_parameters cp
JOIN parameters  p ON p.id  = cp.parameter_id
JOIN categories  c ON c.id  = cp.category_id
WHERE p.platform = 'ASBIS'
  AND cp.is_filter = true
ORDER BY c.name_bg, option_count DESC;

COMMIT;
