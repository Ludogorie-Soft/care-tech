-- =============================================================================
-- Script 44: Post-consolidation cleanup and re-scoring
-- Run AFTER scripts 40, 41, 42, 43.
-- =============================================================================

BEGIN;

-- ── 1. Disable is_filter for params with < 2 effective option values ──────────
-- (consolidation may have merged options, leaving single-value params)
UPDATE category_parameters cp SET is_filter = false
WHERE cp.is_filter = true
  AND (
    SELECT COUNT(DISTINCT LOWER(TRIM(po.name_bg)))
    FROM parameter_options po
    WHERE po.parameter_id = cp.parameter_id
      AND po.name_bg IS NOT NULL AND TRIM(po.name_bg) != ''
  ) < 2;

-- ── 2. Disable is_filter for params with 0 product usages ──────────────────
UPDATE parameters p SET is_filter = false
WHERE p.is_filter = true
  AND NOT EXISTS (
    SELECT 1 FROM product_parameters pp WHERE pp.parameter_id = p.id
  );

-- ── 3. Normalize option name_bg casing inconsistencies ──────────────────────
-- Fix options where same value exists with different casing under same parameter
-- Step A: for each param, find lower-case duplicates → redirect product_params
UPDATE product_parameters pp
SET parameter_option_id = (
  SELECT MIN(po_keep.id)
  FROM parameter_options po_keep
  JOIN parameter_options po_curr ON po_curr.id = pp.parameter_option_id
  WHERE po_keep.parameter_id = po_curr.parameter_id
    AND LOWER(TRIM(po_keep.name_bg)) = LOWER(TRIM(po_curr.name_bg))
)
WHERE EXISTS (
  SELECT 1 FROM parameter_options po_curr
  WHERE po_curr.id = pp.parameter_option_id
    AND EXISTS (
      SELECT 1 FROM parameter_options po_dup
      WHERE po_dup.parameter_id = po_curr.parameter_id
        AND po_dup.id < po_curr.id
        AND LOWER(TRIM(po_dup.name_bg)) = LOWER(TRIM(po_curr.name_bg))
    )
);

-- Step B: delete the higher-id case-duplicate options
DELETE FROM parameter_options
WHERE id IN (
  SELECT po.id FROM parameter_options po
  WHERE EXISTS (
    SELECT 1 FROM parameter_options po2
    WHERE po2.parameter_id = po.parameter_id
      AND po2.id < po.id
      AND LOWER(TRIM(po2.name_bg)) = LOWER(TRIM(po.name_bg))
  )
);

-- ── 4. Remove options with empty/null name_bg and no product usage ──────────
DELETE FROM parameter_options
WHERE (name_bg IS NULL OR TRIM(name_bg) = '')
  AND NOT EXISTS (
    SELECT 1 FROM product_parameters pp WHERE pp.parameter_option_id = parameter_options.id
  );

-- ── 5. Final statistics (verify before commit) ───────────────────────────────
-- SELECT 'parameters' AS tbl, COUNT(*) FROM parameters
-- UNION ALL SELECT 'parameter_options', COUNT(*) FROM parameter_options
-- UNION ALL SELECT 'product_parameters', COUNT(*) FROM product_parameters
-- UNION ALL SELECT 'is_filter=true (params)', COUNT(*) FROM parameters WHERE is_filter = true
-- UNION ALL SELECT 'cats with dup filters (should be 0)', COUNT(*) FROM (
--   SELECT cp.category_id FROM category_parameters cp
--   JOIN parameters p ON p.id = cp.parameter_id
--   WHERE cp.is_filter = true
--   GROUP BY cp.category_id, LOWER(TRIM(p.name_bg))
--   HAVING COUNT(*) > 1) x;

-- COMMIT; / ROLLBACK;
-- COMMIT;