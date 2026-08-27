-- =============================================================================
-- Script 42: is_filter cleanup — mark non-useful params as is_filter=false
-- These are tech specs, not user-visible filter choices.
-- =============================================================================

BEGIN;

-- ── Part A: Name-based blacklist ──────────────────────────────────────────
UPDATE parameters SET is_filter = false
WHERE TRIM(name_bg) IN (
  'Expansion slots',
  'I/O',
  'Manufacturer',
  'Operating temperature',
  'Security',
  'URL на производителя',
  'Weight',
  'Брутно тегло',
  'Бруто тегло',
  'Височина',
  'Включени аксесоари',
  'Гаранция',
  'Допълнителна информация',
  'Дълбочина',
  'Дължина',
  'Комплектът включва',
  'Консумирана мощност',
  'Нето тегло',
  'Номинално тегло',
  'Размери',
  'Размери (мм)',
  'Спецификации на диска',
  'Тегло',
  'Ширина'
);

-- ── Part B: Propagate to category_parameters ──────────────────────────────
UPDATE category_parameters cp
SET is_filter = false
WHERE cp.parameter_id IN (
  SELECT id FROM parameters WHERE is_filter = false
);

-- ── Part C: Parameters with 0 product usages → not useful ─────────────────
UPDATE parameters p SET is_filter = false
WHERE p.is_filter = true
  AND NOT EXISTS (
    SELECT 1 FROM product_parameters pp WHERE pp.parameter_id = p.id
  );

-- ── Part D: Parameters with < 2 unique option values ───────────────────────
UPDATE category_parameters cp SET is_filter = false
WHERE cp.is_filter = true
  AND (
    SELECT COUNT(DISTINCT LOWER(TRIM(po.name_bg)))
    FROM parameter_options po WHERE po.parameter_id = cp.parameter_id
  ) < 2;

-- ── Part E: Report — parameters with > 50 options (may need review) ────────
-- Run this SELECT to identify candidates for further cleanup:
-- SELECT p.id, p.name_bg, p.platform, COUNT(po.id) AS option_count
-- FROM parameters p JOIN parameter_options po ON po.parameter_id = p.id
-- WHERE p.is_filter = true
-- GROUP BY p.id, p.name_bg, p.platform
-- HAVING COUNT(po.id) > 50
-- ORDER BY COUNT(po.id) DESC;

-- COMMIT; / ROLLBACK;
-- COMMIT;