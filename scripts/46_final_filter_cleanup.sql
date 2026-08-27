-- =============================================================================
-- Script 46: Final filter cleanup
-- A) Disable is_filter for dimension/admin params across all categories
-- B) Merge "Външен цвят" (4053) → "Цвят" canonical (6032)
-- =============================================================================

-- ── A. Disable is_filter ─────────────────────────────────────────────────────

UPDATE category_parameters
SET is_filter = false
WHERE parameter_id IN (
    6833,  -- Височина
    1011,  -- Сертификати
    4057,  -- Разположение на устройството
    4073   -- Разположение на захранването
);

-- ── B. Merge "Външен цвят" (4053) → "Цвят" (6032) ───────────────────────────

DO $$ BEGIN
  -- 1. Re-parent unique options to canonical
  UPDATE parameter_options
  SET parameter_id = 6032
  WHERE parameter_id = 4053
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6032
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Delete product_parameters rows that would conflict
  DELETE FROM product_parameters pp
  USING parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6032
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = 4053
    AND EXISTS (
      SELECT 1 FROM product_parameters ex
      WHERE ex.product_id = pp.product_id
        AND ex.parameter_id = 6032
        AND ex.parameter_option_id = c.id
    );

  -- 3. Redirect product_parameters to canonical option
  UPDATE product_parameters pp
  SET parameter_id = 6032, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6032
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = 4053;

  -- 4. Redirect remaining product_parameters (re-parented options)
  UPDATE product_parameters
  SET parameter_id = 6032
  WHERE parameter_id = 4053;

  -- 5. Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) rn FROM product_parameters WHERE parameter_id = 6032
    ) t WHERE rn > 1
  );

  -- 6. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6032, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = 4053
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6032
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 7. Remove non-canonical category linkages
  DELETE FROM category_parameters WHERE parameter_id = 4053;

  -- 8. Delete non-canonical parameter (CASCADE removes orphan options)
  DELETE FROM parameters WHERE id = 4053;
END $$;

-- ── Verify ───────────────────────────────────────────────────────────────────
SELECT p.id, p.name_bg, COUNT(DISTINCT cp.category_id) as cats,
       COUNT(DISTINCT pp.product_id) as products
FROM parameters p
JOIN category_parameters cp ON cp.parameter_id = p.id AND cp.is_filter IS NOT FALSE
JOIN product_parameters pp ON pp.parameter_id = p.id
WHERE p.id = 6032
GROUP BY p.id, p.name_bg;
