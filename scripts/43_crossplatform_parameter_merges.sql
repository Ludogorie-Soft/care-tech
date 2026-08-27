-- =============================================================================
-- Script 43: Cross-platform parameter merges (top 20 by category impact)
-- Run AFTER scripts 40 and 41.
-- For each case: VALI canonical absorbs TEKRA/ASBIS equivalents.
-- OPTIONS ARE NOT MERGED (different languages) — just parameter linkage.
-- =============================================================================

BEGIN;

-- ── "интерфейс": ASBIS param 5746 → VALI canonical 6953 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 6953
  WHERE parameter_id = 5746
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 6953
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 6953, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 6953
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 5746;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 6953
  WHERE parameter_id = 5746;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 6953) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6953, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 5746
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 6953)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 5746;
  DELETE FROM parameters WHERE id = 5746;
END $$;

-- ── "цвят": TEKRA param 4023 → VALI canonical 6032 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 6032
  WHERE parameter_id = 4023
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 6032
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 6032, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 6032
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4023;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 6032
  WHERE parameter_id = 4023;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 6032) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6032, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4023
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 6032)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4023;
  DELETE FROM parameters WHERE id = 4023;
END $$;

-- ── "тип продукт": ASBIS param 5092 → VALI canonical 7070 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 7070
  WHERE parameter_id = 5092
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 7070
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 7070, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 7070
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 5092;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 7070
  WHERE parameter_id = 5092;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 7070) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 7070, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 5092
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 7070)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 5092;
  DELETE FROM parameters WHERE id = 5092;
END $$;

-- ── "честотен диапазон": ASBIS param 4105 → VALI canonical 6869 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 6869
  WHERE parameter_id = 4105
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 6869
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 6869, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 6869
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4105;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 6869
  WHERE parameter_id = 4105;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 6869) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6869, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4105
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 6869)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4105;
  DELETE FROM parameters WHERE id = 4105;
END $$;

-- ── "размери": TEKRA param 4020 → VALI canonical 3211 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 3211
  WHERE parameter_id = 4020
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 3211
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 3211, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 3211
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4020;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 3211
  WHERE parameter_id = 4020;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 3211) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3211, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4020
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 3211)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4020;
  DELETE FROM parameters WHERE id = 4020;
END $$;

-- ── "размери": ASBIS param 4841 → VALI canonical 3211 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 3211
  WHERE parameter_id = 4841
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 3211
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 3211, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 3211
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4841;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 3211
  WHERE parameter_id = 4841;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 3211) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3211, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4841
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 3211)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4841;
  DELETE FROM parameters WHERE id = 4841;
END $$;

-- ── "операционна система": ASBIS param 5604 → VALI canonical 69 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 69
  WHERE parameter_id = 5604
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 69
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 69, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 69
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 5604;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 69
  WHERE parameter_id = 5604;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 69) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 69, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 5604
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 69)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 5604;
  DELETE FROM parameters WHERE id = 5604;
END $$;

-- ── "резолюция": TEKRA param 4022 → VALI canonical 3410 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 3410
  WHERE parameter_id = 4022
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 3410
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 3410, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 3410
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4022;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 3410
  WHERE parameter_id = 4022;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 3410) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3410, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4022
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 3410)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4022;
  DELETE FROM parameters WHERE id = 4022;
END $$;

-- ── "технология": TEKRA param 4019 → VALI canonical 57 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 57
  WHERE parameter_id = 4019
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 57
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 57, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 57
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4019;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 57
  WHERE parameter_id = 4019;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 57) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 57, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4019
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 57)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4019;
  DELETE FROM parameters WHERE id = 4019;
END $$;

-- ── "антена": ASBIS param 4721 → VALI canonical 6481 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 6481
  WHERE parameter_id = 4721
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 6481
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 6481, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 6481
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4721;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 6481
  WHERE parameter_id = 4721;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 6481) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6481, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4721
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 6481)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4721;
  DELETE FROM parameters WHERE id = 4721;
END $$;

-- ── "контраст": MOST param 5857 → VALI canonical 186 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 186
  WHERE parameter_id = 5857
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 186
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 186, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 186
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 5857;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 186
  WHERE parameter_id = 5857;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 186) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 186, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 5857
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 186)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 5857;
  DELETE FROM parameters WHERE id = 5857;
END $$;

-- ── "входно напрежение": ASBIS param 4976 → VALI canonical 324 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 324
  WHERE parameter_id = 4976
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 324
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 324, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 324
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4976;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 324
  WHERE parameter_id = 4976;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 324) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 324, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4976
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 324)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4976;
  DELETE FROM parameters WHERE id = 4976;
END $$;

-- ── "капацитет": TEKRA param 4025 → VALI canonical 3710 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 3710
  WHERE parameter_id = 4025
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 3710
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 3710, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 3710
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4025;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 3710
  WHERE parameter_id = 4025;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 3710) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3710, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4025
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 3710)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4025;
  DELETE FROM parameters WHERE id = 4025;
END $$;

-- ── "захранващ адаптер": MOST param 5837 → VALI canonical 4267 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 4267
  WHERE parameter_id = 5837
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4267
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 4267, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 4267
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 5837;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 4267
  WHERE parameter_id = 5837;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4267) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4267, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 5837
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4267)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 5837;
  DELETE FROM parameters WHERE id = 5837;
END $$;

-- ── "тип памет": ASBIS param 5165 → VALI canonical 3470 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 3470
  WHERE parameter_id = 5165
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 3470
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 3470, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 3470
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 5165;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 3470
  WHERE parameter_id = 5165;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 3470) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3470, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 5165
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 3470)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 5165;
  DELETE FROM parameters WHERE id = 5165;
END $$;

-- ── "чувствителност": ASBIS param 5375 → VALI canonical 6993 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 6993
  WHERE parameter_id = 5375
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 6993
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 6993, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 6993
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 5375;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 6993
  WHERE parameter_id = 5375;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 6993) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6993, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 5375
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 6993)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 5375;
  DELETE FROM parameters WHERE id = 5375;
END $$;

-- ── "размер на дисплея": ASBIS param 5613 → VALI canonical 6526 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 6526
  WHERE parameter_id = 5613
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 6526
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 6526, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 6526
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 5613;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 6526
  WHERE parameter_id = 5613;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 6526) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6526, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 5613
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 6526)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 5613;
  DELETE FROM parameters WHERE id = 5613;
END $$;

-- ── "скорост на въртене": ASBIS param 5799 → VALI canonical 6945 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 6945
  WHERE parameter_id = 5799
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 6945
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 6945, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 6945
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 5799;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 6945
  WHERE parameter_id = 5799;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 6945) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6945, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 5799
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 6945)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 5799;
  DELETE FROM parameters WHERE id = 5799;
END $$;

-- ── "форм фактор": ASBIS param 5190 → VALI canonical 6719 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 6719
  WHERE parameter_id = 5190
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 6719
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 6719, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 6719
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 5190;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 6719
  WHERE parameter_id = 5190;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 6719) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6719, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 5190
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 6719)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 5190;
  DELETE FROM parameters WHERE id = 5190;
END $$;

-- ── "комплектът включва": ASBIS param 4418 → VALI canonical 3611 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 3611
  WHERE parameter_id = 4418
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 3611
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 3611, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 3611
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4418;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 3611
  WHERE parameter_id = 4418;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 3611) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3611, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4418
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 3611)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4418;
  DELETE FROM parameters WHERE id = 4418;
END $$;

-- ── "мощност": TEKRA param 4026 → VALI canonical 317 ──
DO $$ BEGIN
  -- Re-parent options (keep separate — different language)
  UPDATE parameter_options SET parameter_id = 317
  WHERE parameter_id = 4026
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 317
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  -- Options that DO match by name → redirect product_params to master option
  UPDATE product_parameters pp
  SET parameter_id = 317, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 317
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = 4026;
  -- Remaining product_params (re-parented options)
  UPDATE product_parameters SET parameter_id = 317
  WHERE parameter_id = 4026;
  -- Remove duplicates
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 317) t WHERE rn > 1);
  -- category_parameters
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 317, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = 4026
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 317)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = 4026;
  DELETE FROM parameters WHERE id = 4026;
END $$;

-- COMMIT; / ROLLBACK;
-- COMMIT;