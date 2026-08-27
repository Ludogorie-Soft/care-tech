-- =============================================================================
-- Script 41: ASBIS + MOST parameter consolidation
-- ASBIS groups: 78 | ASBIS rows to eliminate: 81 | MOST groups: 2 | MOST rows: 2
-- =============================================================================

BEGIN;

-- GROUP: "Серия" → canonical: 4124  non-canonical: ['4852', '4847']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4124
  WHERE parameter_id = ANY(ARRAY[4852, 4847])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4124
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4124, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4124
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4852, 4847]);

  UPDATE product_parameters SET parameter_id = 4124
  WHERE parameter_id = ANY(ARRAY[4852, 4847]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4124) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4124, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4852, 4847])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4124)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4852, 4847]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4852, 4847]);
END $$;

-- GROUP: "Аудио интерфейс" → canonical: 4170  non-canonical: ['5193', '5610']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4170
  WHERE parameter_id = ANY(ARRAY[5193, 5610])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4170
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4170, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4170
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5193, 5610]);

  UPDATE product_parameters SET parameter_id = 4170
  WHERE parameter_id = ANY(ARRAY[5193, 5610]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4170) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4170, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5193, 5610])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4170)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5193, 5610]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5193, 5610]);
END $$;

-- GROUP: "Безжично зареждане" → canonical: 4498  non-canonical: ['4466', '5643']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4498
  WHERE parameter_id = ANY(ARRAY[4466, 5643])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4498
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4498, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4498
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4466, 5643]);

  UPDATE product_parameters SET parameter_id = 4498
  WHERE parameter_id = ANY(ARRAY[4466, 5643]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4498) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4498, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4466, 5643])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4498)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4466, 5643]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4466, 5643]);
END $$;

-- GROUP: "Минимална входна честота" → canonical: 4072  non-canonical: ['5805']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4072
  WHERE parameter_id = ANY(ARRAY[5805])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4072
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4072, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4072
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5805]);

  UPDATE product_parameters SET parameter_id = 4072
  WHERE parameter_id = ANY(ARRAY[5805]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4072) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4072, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5805])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4072)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5805]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5805]);
END $$;

-- GROUP: "Максимално входно напрежение" → canonical: 4083  non-canonical: ['5815']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4083
  WHERE parameter_id = ANY(ARRAY[5815])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4083
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4083, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4083
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5815]);

  UPDATE product_parameters SET parameter_id = 4083
  WHERE parameter_id = ANY(ARRAY[5815]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4083) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4083, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5815])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4083)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5815]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5815]);
END $$;

-- GROUP: "Работен режим" → canonical: 5411  non-canonical: ['5438']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5411
  WHERE parameter_id = ANY(ARRAY[5438])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5411
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5411, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5411
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5438]);

  UPDATE product_parameters SET parameter_id = 5411
  WHERE parameter_id = ANY(ARRAY[5438]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5411) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5411, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5438])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5411)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5438]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5438]);
END $$;

-- GROUP: "Брой температурни режими" → canonical: 5428  non-canonical: ['5524']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5428
  WHERE parameter_id = ANY(ARRAY[5524])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5428
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5428, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5428
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5524]);

  UPDATE product_parameters SET parameter_id = 5428
  WHERE parameter_id = ANY(ARRAY[5524]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5428) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5428, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5524])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5428)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5524]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5524]);
END $$;

-- GROUP: "Максимална скорост" → canonical: 4407  non-canonical: ['5444']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4407
  WHERE parameter_id = ANY(ARRAY[5444])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4407
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4407, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4407
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5444]);

  UPDATE product_parameters SET parameter_id = 4407
  WHERE parameter_id = ANY(ARRAY[5444]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4407) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4407, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5444])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4407)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5444]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5444]);
END $$;

-- GROUP: "Вътрешна тактова честота" → canonical: 4064  non-canonical: ['5177']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4064
  WHERE parameter_id = ANY(ARRAY[5177])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4064
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4064, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4064
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5177]);

  UPDATE product_parameters SET parameter_id = 4064
  WHERE parameter_id = ANY(ARRAY[5177]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4064) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4064, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5177])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4064)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5177]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5177]);
END $$;

-- GROUP: "Максимална входна честота" → canonical: 4078  non-canonical: ['5823']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4078
  WHERE parameter_id = ANY(ARRAY[5823])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4078
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4078, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4078
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5823]);

  UPDATE product_parameters SET parameter_id = 4078
  WHERE parameter_id = ANY(ARRAY[5823]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4078) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4078, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5823])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4078)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5823]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5823]);
END $$;

-- GROUP: "Брой инсталирани захранвания" → canonical: 4098  non-canonical: ['5255']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4098
  WHERE parameter_id = ANY(ARRAY[5255])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4098
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4098, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4098
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5255]);

  UPDATE product_parameters SET parameter_id = 4098
  WHERE parameter_id = ANY(ARRAY[5255]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4098) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4098, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5255])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4098)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5255]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5255]);
END $$;

-- GROUP: "Брой инсталирани процесори" → canonical: 4100  non-canonical: ['5169']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4100
  WHERE parameter_id = ANY(ARRAY[5169])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4100
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4100, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4100
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5169]);

  UPDATE product_parameters SET parameter_id = 4100
  WHERE parameter_id = ANY(ARRAY[5169]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4100) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4100, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5169])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4100)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5169]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5169]);
END $$;

-- GROUP: "Буферна памет" → canonical: 4104  non-canonical: ['4094']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4104
  WHERE parameter_id = ANY(ARRAY[4094])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4104
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4104, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4104
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4094]);

  UPDATE product_parameters SET parameter_id = 4104
  WHERE parameter_id = ANY(ARRAY[4094]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4104) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4104, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4094])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4104)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4094]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4094]);
END $$;

-- GROUP: "Характеристики" → canonical: 4108  non-canonical: ['5258']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4108
  WHERE parameter_id = ANY(ARRAY[5258])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4108
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4108, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4108
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5258]);

  UPDATE product_parameters SET parameter_id = 4108
  WHERE parameter_id = ANY(ARRAY[5258]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4108) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4108, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5258])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4108)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5258]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5258]);
END $$;

-- GROUP: "Материал на корпуса" → canonical: 4110  non-canonical: ['5323']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4110
  WHERE parameter_id = ANY(ARRAY[5323])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4110
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4110, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4110
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5323]);

  UPDATE product_parameters SET parameter_id = 4110
  WHERE parameter_id = ANY(ARRAY[5323]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4110) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4110, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5323])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4110)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5323]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5323]);
END $$;

-- GROUP: "Материал на изработка" → canonical: 4133  non-canonical: ['4514']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4133
  WHERE parameter_id = ANY(ARRAY[4514])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4133
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4133, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4133
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4514]);

  UPDATE product_parameters SET parameter_id = 4133
  WHERE parameter_id = ANY(ARRAY[4514]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4133) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4133, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4514])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4133)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4514]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4514]);
END $$;

-- GROUP: "Нискотонов говорител" → canonical: 4131  non-canonical: ['4621']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4131
  WHERE parameter_id = ANY(ARRAY[4621])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4131
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4131, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4131
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4621]);

  UPDATE product_parameters SET parameter_id = 4131
  WHERE parameter_id = ANY(ARRAY[4621]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4131) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4131, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4621])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4131)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4621]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4621]);
END $$;

-- GROUP: "Изходна мощност" → canonical: 4149  non-canonical: ['4522']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4149
  WHERE parameter_id = ANY(ARRAY[4522])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4149
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4149, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4149
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4522]);

  UPDATE product_parameters SET parameter_id = 4149
  WHERE parameter_id = ANY(ARRAY[4522]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4149) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4149, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4522])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4149)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4522]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4522]);
END $$;

-- GROUP: "Технология на батерията" → canonical: 4172  non-canonical: ['5606']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4172
  WHERE parameter_id = ANY(ARRAY[5606])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4172
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4172, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4172
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5606]);

  UPDATE product_parameters SET parameter_id = 4172
  WHERE parameter_id = ANY(ARRAY[5606]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4172) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4172, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5606])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4172)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5606]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5606]);
END $$;

-- GROUP: "Тип захранващи конектори" → canonical: 4173  non-canonical: ['5189']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4173
  WHERE parameter_id = ANY(ARRAY[5189])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4173
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4173, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4173
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5189]);

  UPDATE product_parameters SET parameter_id = 4173
  WHERE parameter_id = ANY(ARRAY[5189]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4173) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4173, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5189])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4173)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5189]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5189]);
END $$;

-- GROUP: "Артикул" → canonical: 4176  non-canonical: ['4525']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4176
  WHERE parameter_id = ANY(ARRAY[4525])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4176
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4176, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4176
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4525]);

  UPDATE product_parameters SET parameter_id = 4176
  WHERE parameter_id = ANY(ARRAY[4525]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4176) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4176, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4525])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4176)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4525]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4525]);
END $$;

-- GROUP: "Защита на веригата" → canonical: 4204  non-canonical: ['4971']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4204
  WHERE parameter_id = ANY(ARRAY[4971])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4204
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4204, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4204
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4971]);

  UPDATE product_parameters SET parameter_id = 4204
  WHERE parameter_id = ANY(ARRAY[4971]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4204) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4204, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4971])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4204)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4971]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4971]);
END $$;

-- GROUP: "Дължина (мм)" → canonical: 4208  non-canonical: ['4202']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4208
  WHERE parameter_id = ANY(ARRAY[4202])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4208
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4208, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4208
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4202]);

  UPDATE product_parameters SET parameter_id = 4208
  WHERE parameter_id = ANY(ARRAY[4202]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4208) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4208, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4202])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4208)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4202]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4202]);
END $$;

-- GROUP: "Общ брой слотове за памет" → canonical: 4276  non-canonical: ['5185']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4276
  WHERE parameter_id = ANY(ARRAY[5185])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4276
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4276, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4276
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5185]);

  UPDATE product_parameters SET parameter_id = 4276
  WHERE parameter_id = ANY(ARRAY[5185]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4276) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4276, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5185])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4276)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5185]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5185]);
END $$;

-- GROUP: "Слушалки" → canonical: 4283  non-canonical: ['4630']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4283
  WHERE parameter_id = ANY(ARRAY[4630])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4283
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4283, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4283
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4630]);

  UPDATE product_parameters SET parameter_id = 4283
  WHERE parameter_id = ANY(ARRAY[4630]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4283) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4283, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4630])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4283)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4630]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4630]);
END $$;

-- GROUP: "Максимална подходяща височина" → canonical: 4348  non-canonical: ['5332']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4348
  WHERE parameter_id = ANY(ARRAY[5332])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4348
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4348, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4348
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5332]);

  UPDATE product_parameters SET parameter_id = 4348
  WHERE parameter_id = ANY(ARRAY[5332]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4348) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4348, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5332])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4348)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5332]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5332]);
END $$;

-- GROUP: "Характеристики на стола" → canonical: 4350  non-canonical: ['5328']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4350
  WHERE parameter_id = ANY(ARRAY[5328])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4350
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4350, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4350
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5328]);

  UPDATE product_parameters SET parameter_id = 4350
  WHERE parameter_id = ANY(ARRAY[5328]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4350) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4350, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5328])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4350)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5328]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5328]);
END $$;

-- GROUP: "Максимално подходящо тегло" → canonical: 4351  non-canonical: ['5329']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4351
  WHERE parameter_id = ANY(ARRAY[5329])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4351
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4351, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4351
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5329]);

  UPDATE product_parameters SET parameter_id = 4351
  WHERE parameter_id = ANY(ARRAY[5329]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4351) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4351, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5329])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4351)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5329]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5329]);
END $$;

-- GROUP: "Макс. брой поддържани захранвания" → canonical: 4390  non-canonical: ['5260']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4390
  WHERE parameter_id = ANY(ARRAY[5260])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4390
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4390, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4390
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5260]);

  UPDATE product_parameters SET parameter_id = 4390
  WHERE parameter_id = ANY(ARRAY[5260]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4390) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4390, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5260])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4390)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5260]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5260]);
END $$;

-- GROUP: "Ляв конектор" → canonical: 4400  non-canonical: ['4719']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4400
  WHERE parameter_id = ANY(ARRAY[4719])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4400
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4400, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4400
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4719]);

  UPDATE product_parameters SET parameter_id = 4400
  WHERE parameter_id = ANY(ARRAY[4719]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4400) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4400, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4719])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4400)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4719]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4719]);
END $$;

-- GROUP: "Десен конектор" → canonical: 4401  non-canonical: ['4718']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4401
  WHERE parameter_id = ANY(ARRAY[4718])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4401
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4401, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4401
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4718]);

  UPDATE product_parameters SET parameter_id = 4401
  WHERE parameter_id = ANY(ARRAY[4718]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4401) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4401, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4718])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4401)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4718]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4718]);
END $$;

-- GROUP: "Съвместими устройства" → canonical: 4464  non-canonical: ['4513']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4464
  WHERE parameter_id = ANY(ARRAY[4513])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4464
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4464, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4464
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4513]);

  UPDATE product_parameters SET parameter_id = 4464
  WHERE parameter_id = ANY(ARRAY[4513]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4464) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4464, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4513])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4464)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4513]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4513]);
END $$;

-- GROUP: "Стандарти за защита" → canonical: 4891  non-canonical: ['4524']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4891
  WHERE parameter_id = ANY(ARRAY[4524])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4891
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4891, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4891
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4524]);

  UPDATE product_parameters SET parameter_id = 4891
  WHERE parameter_id = ANY(ARRAY[4524]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4891) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4891, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4524])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4891)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4524]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4524]);
END $$;

-- GROUP: "Четец за карти" → canonical: 4544  non-canonical: ['4700']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4544
  WHERE parameter_id = ANY(ARRAY[4700])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4544
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4544, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4544
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4700]);

  UPDATE product_parameters SET parameter_id = 4544
  WHERE parameter_id = ANY(ARRAY[4700]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4544) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4544, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4700])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4544)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4700]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4700]);
END $$;

-- GROUP: "Външен цвят" → canonical: 4053  non-canonical: ['4511']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4053
  WHERE parameter_id = ANY(ARRAY[4511])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4053
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4053, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4053
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4511]);

  UPDATE product_parameters SET parameter_id = 4053
  WHERE parameter_id = ANY(ARRAY[4511]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4053) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4053, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4511])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4053)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4511]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4511]);
END $$;

-- GROUP: "Максимален TDP" → canonical: 5079  non-canonical: ['4560']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5079
  WHERE parameter_id = ANY(ARRAY[4560])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5079
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5079, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5079
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4560]);

  UPDATE product_parameters SET parameter_id = 5079
  WHERE parameter_id = ANY(ARRAY[4560]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5079) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5079, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4560])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5079)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4560]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4560]);
END $$;

-- GROUP: "Аудио изход" → canonical: 4636  non-canonical: ['4702']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4636
  WHERE parameter_id = ANY(ARRAY[4702])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4636
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4636, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4636
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4702]);

  UPDATE product_parameters SET parameter_id = 4636
  WHERE parameter_id = ANY(ARRAY[4702]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4636) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4636, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4702])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4636)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4702]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4702]);
END $$;

-- GROUP: "Скорост на пренос на данни" → canonical: 4667  non-canonical: ['5745']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4667
  WHERE parameter_id = ANY(ARRAY[5745])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4667
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4667, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4667
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5745]);

  UPDATE product_parameters SET parameter_id = 4667
  WHERE parameter_id = ANY(ARRAY[5745]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4667) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4667, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5745])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4667)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5745]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5745]);
END $$;

-- GROUP: "Локализация" → canonical: 4705  non-canonical: ['4740']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4705
  WHERE parameter_id = ANY(ARRAY[4740])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4705
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4705, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4705
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4740]);

  UPDATE product_parameters SET parameter_id = 4705
  WHERE parameter_id = ANY(ARRAY[4740]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4705) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4705, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4740])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4705)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4740]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4740]);
END $$;

-- GROUP: "Многоканален преглед" → canonical: 4751  non-canonical: ['5835']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4751
  WHERE parameter_id = ANY(ARRAY[5835])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4751
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4751, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4751
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5835]);

  UPDATE product_parameters SET parameter_id = 4751
  WHERE parameter_id = ANY(ARRAY[5835]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4751) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4751, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5835])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4751)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5835]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5835]);
END $$;

-- GROUP: "Обработка на екрана" → canonical: 4753  non-canonical: ['5806']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4753
  WHERE parameter_id = ANY(ARRAY[5806])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4753
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4753, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4753
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5806]);

  UPDATE product_parameters SET parameter_id = 4753
  WHERE parameter_id = ANY(ARRAY[5806]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4753) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4753, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5806])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4753)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5806]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5806]);
END $$;

-- GROUP: "Макс. работна надморска височина" → canonical: 4760  non-canonical: ['5243']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4760
  WHERE parameter_id = ANY(ARRAY[5243])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4760
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4760, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4760
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5243]);

  UPDATE product_parameters SET parameter_id = 4760
  WHERE parameter_id = ANY(ARRAY[5243]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4760) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4760, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5243])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4760)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5243]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5243]);
END $$;

-- GROUP: "Плътност на пикселите" → canonical: 4761  non-canonical: ['5822']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4761
  WHERE parameter_id = ANY(ARRAY[5822])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4761
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4761, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4761
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5822]);

  UPDATE product_parameters SET parameter_id = 4761
  WHERE parameter_id = ANY(ARRAY[5822]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4761) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4761, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5822])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4761)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5822]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5822]);
END $$;

-- GROUP: "Технология на дисплея" → canonical: 4762  non-canonical: ['5595']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4762
  WHERE parameter_id = ANY(ARRAY[5595])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4762
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4762, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4762
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5595]);

  UPDATE product_parameters SET parameter_id = 4762
  WHERE parameter_id = ANY(ARRAY[5595]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4762) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4762, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5595])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4762)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5595]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5595]);
END $$;

-- GROUP: "Номинално захранващо напрежение" → canonical: 4789  non-canonical: ['5801']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4789
  WHERE parameter_id = ANY(ARRAY[5801])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4789
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4789, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4789
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5801]);

  UPDATE product_parameters SET parameter_id = 4789
  WHERE parameter_id = ANY(ARRAY[5801]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4789) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4789, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5801])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4789)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5801]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5801]);
END $$;

-- GROUP: "Технология на паметта" → canonical: 4791  non-canonical: ['5803']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4791
  WHERE parameter_id = ANY(ARRAY[5803])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4791
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4791, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4791
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5803]);

  UPDATE product_parameters SET parameter_id = 4791
  WHERE parameter_id = ANY(ARRAY[5803]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4791) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4791, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5803])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4791)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5803]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5803]);
END $$;

-- GROUP: "CAS латентност" → canonical: 5221  non-canonical: ['5804']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5221
  WHERE parameter_id = ANY(ARRAY[5804])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5221
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5221, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5221
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5804]);

  UPDATE product_parameters SET parameter_id = 5221
  WHERE parameter_id = ANY(ARRAY[5804]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5221) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5221, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5804])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5221)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5804]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5804]);
END $$;

-- GROUP: "Характеристики на твърдия диск" → canonical: 4801  non-canonical: ['4829']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4801
  WHERE parameter_id = ANY(ARRAY[4829])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4801
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4801, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4801
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4829]);

  UPDATE product_parameters SET parameter_id = 4801
  WHERE parameter_id = ANY(ARRAY[4829]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4801) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4801, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4829])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4801)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4829]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4829]);
END $$;

-- GROUP: "Интерфейси" → canonical: 4812  non-canonical: ['5800']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4812
  WHERE parameter_id = ANY(ARRAY[5800])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4812
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4812, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4812
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5800]);

  UPDATE product_parameters SET parameter_id = 4812
  WHERE parameter_id = ANY(ARRAY[5800]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4812) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4812, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5800])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4812)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5800]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5800]);
END $$;

-- GROUP: "Характеристики на паметта" → canonical: 4858  non-canonical: ['5166']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4858
  WHERE parameter_id = ANY(ARRAY[5166])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4858
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4858, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4858
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5166]);

  UPDATE product_parameters SET parameter_id = 4858
  WHERE parameter_id = ANY(ARRAY[5166]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4858) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4858, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5166])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4858)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5166]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5166]);
END $$;

-- GROUP: "Аудио чипсет" → canonical: 4860  non-canonical: ['5196']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4860
  WHERE parameter_id = ANY(ARRAY[5196])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4860
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4860, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4860
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5196]);

  UPDATE product_parameters SET parameter_id = 4860
  WHERE parameter_id = ANY(ARRAY[5196]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4860) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4860, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5196])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4860)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5196]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5196]);
END $$;

-- GROUP: "Форм фактор на паметта" → canonical: 4863  non-canonical: ['5178']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4863
  WHERE parameter_id = ANY(ARRAY[5178])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4863
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4863, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4863
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5178]);

  UPDATE product_parameters SET parameter_id = 4863
  WHERE parameter_id = ANY(ARRAY[5178]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4863) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4863, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5178])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4863)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5178]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5178]);
END $$;

-- GROUP: "Форм фактор на системата" → canonical: 4866  non-canonical: ['5256']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4866
  WHERE parameter_id = ANY(ARRAY[5256])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4866
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4866, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4866
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5256]);

  UPDATE product_parameters SET parameter_id = 4866
  WHERE parameter_id = ANY(ARRAY[5256]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4866) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4866, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5256])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4866)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5256]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5256]);
END $$;

-- GROUP: "Брой поддържани дънни платки" → canonical: 4868  non-canonical: ['5254']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4868
  WHERE parameter_id = ANY(ARRAY[5254])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4868
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4868, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4868
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5254]);

  UPDATE product_parameters SET parameter_id = 4868
  WHERE parameter_id = ANY(ARRAY[5254]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4868) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4868, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5254])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4868)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5254]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5254]);
END $$;

-- GROUP: "Инсталирана видео памет" → canonical: 4896  non-canonical: ['4875']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4896
  WHERE parameter_id = ANY(ARRAY[4875])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4896
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4896, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4896
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4875]);

  UPDATE product_parameters SET parameter_id = 4896
  WHERE parameter_id = ANY(ARRAY[4875]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4896) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4896, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4875])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4896)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4875]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4875]);
END $$;

-- GROUP: "Размер на вътрешната памет" → canonical: 4888  non-canonical: ['5611']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4888
  WHERE parameter_id = ANY(ARRAY[5611])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4888
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4888, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4888
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5611]);

  UPDATE product_parameters SET parameter_id = 4888
  WHERE parameter_id = ANY(ARRAY[5611]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4888) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4888, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5611])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4888)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5611]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5611]);
END $$;

-- GROUP: "Вътрешна тактова честота на CPU" → canonical: 4893  non-canonical: ['5617']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4893
  WHERE parameter_id = ANY(ARRAY[5617])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4893
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4893, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4893
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5617]);

  UPDATE product_parameters SET parameter_id = 4893
  WHERE parameter_id = ANY(ARRAY[5617]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4893) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4893, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5617])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4893)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5617]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5617]);
END $$;

-- GROUP: "Брой инсталирани вентилатори" → canonical: 4910  non-canonical: ['4969']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4910
  WHERE parameter_id = ANY(ARRAY[4969])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4910
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4910, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4910
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4969]);

  UPDATE product_parameters SET parameter_id = 4910
  WHERE parameter_id = ANY(ARRAY[4969]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4910) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4910, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4969])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4910)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4969]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4969]);
END $$;

-- GROUP: "Резолюция на камерата" → canonical: 5006  non-canonical: ['5614']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5006
  WHERE parameter_id = ANY(ARRAY[5614])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5006
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5006, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5006
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5614]);

  UPDATE product_parameters SET parameter_id = 5006
  WHERE parameter_id = ANY(ARRAY[5614]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5006) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5006, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5614])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5006)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5614]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5614]);
END $$;

-- GROUP: "Минимално входно напрежение" → canonical: 4088  non-canonical: ['5824']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4088
  WHERE parameter_id = ANY(ARRAY[5824])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4088
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4088, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4088
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5824]);

  UPDATE product_parameters SET parameter_id = 4088
  WHERE parameter_id = ANY(ARRAY[5824]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4088) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4088, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5824])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4088)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5824]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5824]);
END $$;

-- GROUP: "Тип продукт" → canonical: 5092  non-canonical: ['4727']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5092
  WHERE parameter_id = ANY(ARRAY[4727])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5092
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5092, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5092
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4727]);

  UPDATE product_parameters SET parameter_id = 5092
  WHERE parameter_id = ANY(ARRAY[4727]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5092) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5092, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4727])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5092)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4727]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4727]);
END $$;

-- GROUP: "Тип клавиши" → canonical: 5110  non-canonical: ['5099']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5110
  WHERE parameter_id = ANY(ARRAY[5099])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5110
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5110, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5110
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5099]);

  UPDATE product_parameters SET parameter_id = 5110
  WHERE parameter_id = ANY(ARRAY[5099]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5110) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5110, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5099])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5110)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5099]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5099]);
END $$;

-- GROUP: "BIOS тип" → canonical: 5141  non-canonical: ['5172']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5141
  WHERE parameter_id = ANY(ARRAY[5172])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5141
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5141, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5141
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5172]);

  UPDATE product_parameters SET parameter_id = 5141
  WHERE parameter_id = ANY(ARRAY[5172]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5141) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5141, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5172])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5141)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5172]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5172]);
END $$;

-- GROUP: "BIOS характеристики" → canonical: 5143  non-canonical: ['5171']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5143
  WHERE parameter_id = ANY(ARRAY[5171])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5143
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5143, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5143
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5171]);

  UPDATE product_parameters SET parameter_id = 5143
  WHERE parameter_id = ANY(ARRAY[5171]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5143) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5143, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5171])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5143)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5171]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5171]);
END $$;

-- GROUP: "Инсталиран процесор" → canonical: 5197  non-canonical: ['5191']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5197
  WHERE parameter_id = ANY(ARRAY[5191])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5197
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5197, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5197
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5191]);

  UPDATE product_parameters SET parameter_id = 5197
  WHERE parameter_id = ANY(ARRAY[5191]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5197) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5197, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5191])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5197)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5191]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5191]);
END $$;

-- GROUP: "Минимален ъгъл на облегалката" → canonical: 5304  non-canonical: ['5333']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5304
  WHERE parameter_id = ANY(ARRAY[5333])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5304
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5304, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5304
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5333]);

  UPDATE product_parameters SET parameter_id = 5304
  WHERE parameter_id = ANY(ARRAY[5333]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5304) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5304, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5333])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5304)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5333]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5333]);
END $$;

-- GROUP: "Регулиране на височината" → canonical: 5307  non-canonical: ['5324']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5307
  WHERE parameter_id = ANY(ARRAY[5324])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5307
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5307, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5307
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5324]);

  UPDATE product_parameters SET parameter_id = 5307
  WHERE parameter_id = ANY(ARRAY[5324]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5307) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5307, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5324])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5307)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5324]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5324]);
END $$;

-- GROUP: "Колела" → canonical: 5308  non-canonical: ['5362']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5308
  WHERE parameter_id = ANY(ARRAY[5362])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5308
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5308, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5308
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5362]);

  UPDATE product_parameters SET parameter_id = 5308
  WHERE parameter_id = ANY(ARRAY[5362]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5308) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5308, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5362])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5308)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5362]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5362]);
END $$;

-- GROUP: "Характеристики на подлакътниците" → canonical: 5311  non-canonical: ['5325']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5311
  WHERE parameter_id = ANY(ARRAY[5325])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5311
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5311, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5311
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5325]);

  UPDATE product_parameters SET parameter_id = 5311
  WHERE parameter_id = ANY(ARRAY[5325]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5311) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5311, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5325])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5311)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5325]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5325]);
END $$;

-- GROUP: "Максимален ъгъл на облегалката" → canonical: 5316  non-canonical: ['5331']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5316
  WHERE parameter_id = ANY(ARRAY[5331])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5316
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5316, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5316
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5331]);

  UPDATE product_parameters SET parameter_id = 5316
  WHERE parameter_id = ANY(ARRAY[5331]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5316) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5316, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5331])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5316)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5331]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5331]);
END $$;

-- GROUP: "Максимално времетраене на готвене" → canonical: 5383  non-canonical: ['5450']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5383
  WHERE parameter_id = ANY(ARRAY[5450])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5383
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5383, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5383
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5450]);

  UPDATE product_parameters SET parameter_id = 5383
  WHERE parameter_id = ANY(ARRAY[5450]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5383) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5383, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5450])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5383)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5450]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5450]);
END $$;

-- GROUP: "Поддържани процесори" → canonical: 5199  non-canonical: ['7079']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5199
  WHERE parameter_id = ANY(ARRAY[7079])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5199
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 5199, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 5199
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[7079]);

  UPDATE product_parameters SET parameter_id = 5199
  WHERE parameter_id = ANY(ARRAY[7079]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5199) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5199, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[7079])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5199)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[7079]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[7079]);
END $$;

-- GROUP: "Диаметър на говорителя" → canonical: 7074  non-canonical: ['5374']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 7074
  WHERE parameter_id = ANY(ARRAY[5374])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 7074
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 7074, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 7074
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5374]);

  UPDATE product_parameters SET parameter_id = 7074
  WHERE parameter_id = ANY(ARRAY[5374]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 7074) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 7074, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5374])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 7074)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5374]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5374]);
END $$;

-- GROUP: "Максимална резолюция" → canonical: 7075  non-canonical: ['5828']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 7075
  WHERE parameter_id = ANY(ARRAY[5828])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 7075
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 7075, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 7075
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5828]);

  UPDATE product_parameters SET parameter_id = 7075
  WHERE parameter_id = ANY(ARRAY[5828]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 7075) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 7075, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5828])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 7075)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5828]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5828]);
END $$;

-- GROUP: "Номинално тегло" → canonical: 4059  non-canonical: ['5609']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4059
  WHERE parameter_id = ANY(ARRAY[5609])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4059
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4059, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4059
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5609]);

  UPDATE product_parameters SET parameter_id = 4059
  WHERE parameter_id = ANY(ARRAY[5609]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4059) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4059, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5609])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4059)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5609]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5609]);
END $$;

-- GROUP: "Материал на рамката" → canonical: 7076  non-canonical: ['5330']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 7076
  WHERE parameter_id = ANY(ARRAY[5330])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 7076
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 7076, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 7076
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5330]);

  UPDATE product_parameters SET parameter_id = 7076
  WHERE parameter_id = ANY(ARRAY[5330]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 7076) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 7076, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5330])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 7076)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5330]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5330]);
END $$;

-- GROUP: "Вградени устройства" → canonical: 4074  non-canonical: ['5605']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4074
  WHERE parameter_id = ANY(ARRAY[5605])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4074
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4074, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4074
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5605]);

  UPDATE product_parameters SET parameter_id = 4074
  WHERE parameter_id = ANY(ARRAY[5605]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4074) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4074, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5605])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4074)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5605]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5605]);
END $$;

-- GROUP: "Тип камера" → canonical: 4316  non-canonical: ['4527']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 4316
  WHERE parameter_id = ANY(ARRAY[4527])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 4316
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  UPDATE product_parameters pp
  SET parameter_id = 4316, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = 4316
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[4527]);

  UPDATE product_parameters SET parameter_id = 4316
  WHERE parameter_id = ANY(ARRAY[4527]);

  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 4316) t WHERE rn > 1);

  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 4316, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[4527])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 4316)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[4527]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[4527]);
END $$;

-- VERIFICATION: 81 rows to be eliminated
-- SELECT COUNT(*) FROM parameters WHERE platform = 'ASBIS';

-- =============================================================================
-- MOST platform internal duplicates (added — 2 groups)
-- =============================================================================

-- MOST GROUP: "Размер на паметта" → canonical: 5920  non-canonical: ['5901']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5920
  WHERE parameter_id = ANY(ARRAY[5901])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5920
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  UPDATE product_parameters pp
  SET parameter_id = 5920, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 5920
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5901]);
  UPDATE product_parameters SET parameter_id = 5920 WHERE parameter_id = ANY(ARRAY[5901]);
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5920) t WHERE rn > 1);
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5920, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5901])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5920)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5901]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5901]);
END $$;

-- MOST GROUP: "Задна камера" → canonical: 5905  non-canonical: ['5909']
DO $$ BEGIN
  UPDATE parameter_options SET parameter_id = 5905
  WHERE parameter_id = ANY(ARRAY[5909])
    AND NOT EXISTS (SELECT 1 FROM parameter_options c WHERE c.parameter_id = 5905
      AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));
  UPDATE product_parameters pp
  SET parameter_id = 5905, parameter_option_id = c.id
  FROM parameter_options old_opt JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg)) AND c.parameter_id = 5905
  WHERE pp.parameter_option_id = old_opt.id AND old_opt.parameter_id = ANY(ARRAY[5909]);
  UPDATE product_parameters SET parameter_id = 5905 WHERE parameter_id = ANY(ARRAY[5909]);
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
    FROM product_parameters WHERE parameter_id = 5905) t WHERE rn > 1);
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5905, cp.is_filter FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5909])
    AND NOT EXISTS (SELECT 1 FROM category_parameters WHERE category_id = cp.category_id AND parameter_id = 5905)
  ON CONFLICT (category_id, parameter_id) DO NOTHING;
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5909]);
  DELETE FROM parameters WHERE id = ANY(ARRAY[5909]);
END $$;

-- VERIFICATION: ASBIS rows eliminated: 81 | MOST rows eliminated: 2
-- SELECT platform, COUNT(*) FROM parameters GROUP BY platform ORDER BY platform;
-- COMMIT; / ROLLBACK;

-- COMMIT;