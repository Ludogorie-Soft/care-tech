-- =============================================================================
-- Script 40: VALI parameter consolidation
-- Merges duplicate VALI parameter rows (same name_bg) into one canonical row
-- Generated automatically from parameters_202608261156.csv
-- Groups to process: 203
-- Parameter rows to eliminate: 810
--
-- SAFETY: Run inside BEGIN/COMMIT. Check row counts before committing.
-- =============================================================================

BEGIN;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Цвят"
--   Total rows: 91  →  canonical: 6032
--   Categories with canonical: 6
--   Products with canonical:   3072
--   Non-canonical ids: ['3142', '3173', '6022', '6044', '6057', '6064', '6072', '6080', '6089', '6102', '6109', '6115', '3361', '6140', '3396', '3420', '6158', '6179', '6186', '3507', '6205', '6210', '6238', '3566', '3581', '3604', '3620', '3628', '3639', '6287', '3665', '6300', '3684', '6322', '3718', '6339', '6345', '6353', '3754', '6387', '3808', '6407', '6418', '3848', '6423', '6427', '3900', '3907', '6468', '6494', '6512', '6517', '6549', '6556', '6565', '6591', '6599', '6662', '6714', '6723', '6725', '6731', '6736', '6761', '6765', '6778', '6796', '6802', '6805', '6806', '6825', '6830', '6834', '6840', '6850', '6868', '6874', '6886', '6888', '6904', '6914', '6930', '6934', '6937', '6942', '6980', '6992', '7013', '7014', '7057']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6032
  WHERE parameter_id = ANY(ARRAY[3142, 3173, 6022, 6044, 6057, 6064, 6072, 6080, 6089, 6102, 6109, 6115, 3361, 6140, 3396, 3420, 6158, 6179, 6186, 3507, 6205, 6210, 6238, 3566, 3581, 3604, 3620, 3628, 3639, 6287, 3665, 6300, 3684, 6322, 3718, 6339, 6345, 6353, 3754, 6387, 3808, 6407, 6418, 3848, 6423, 6427, 3900, 3907, 6468, 6494, 6512, 6517, 6549, 6556, 6565, 6591, 6599, 6662, 6714, 6723, 6725, 6731, 6736, 6761, 6765, 6778, 6796, 6802, 6805, 6806, 6825, 6830, 6834, 6840, 6850, 6868, 6874, 6886, 6888, 6904, 6914, 6930, 6934, 6937, 6942, 6980, 6992, 7013, 7014, 7057])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6032
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6032,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6032
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3142, 3173, 6022, 6044, 6057, 6064, 6072, 6080, 6089, 6102, 6109, 6115, 3361, 6140, 3396, 3420, 6158, 6179, 6186, 3507, 6205, 6210, 6238, 3566, 3581, 3604, 3620, 3628, 3639, 6287, 3665, 6300, 3684, 6322, 3718, 6339, 6345, 6353, 3754, 6387, 3808, 6407, 6418, 3848, 6423, 6427, 3900, 3907, 6468, 6494, 6512, 6517, 6549, 6556, 6565, 6591, 6599, 6662, 6714, 6723, 6725, 6731, 6736, 6761, 6765, 6778, 6796, 6802, 6805, 6806, 6825, 6830, 6834, 6840, 6850, 6868, 6874, 6886, 6888, 6904, 6914, 6930, 6934, 6937, 6942, 6980, 6992, 7013, 7014, 7057]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6032
  WHERE parameter_id = ANY(ARRAY[3142, 3173, 6022, 6044, 6057, 6064, 6072, 6080, 6089, 6102, 6109, 6115, 3361, 6140, 3396, 3420, 6158, 6179, 6186, 3507, 6205, 6210, 6238, 3566, 3581, 3604, 3620, 3628, 3639, 6287, 3665, 6300, 3684, 6322, 3718, 6339, 6345, 6353, 3754, 6387, 3808, 6407, 6418, 3848, 6423, 6427, 3900, 3907, 6468, 6494, 6512, 6517, 6549, 6556, 6565, 6591, 6599, 6662, 6714, 6723, 6725, 6731, 6736, 6761, 6765, 6778, 6796, 6802, 6805, 6806, 6825, 6830, 6834, 6840, 6850, 6868, 6874, 6886, 6888, 6904, 6914, 6930, 6934, 6937, 6942, 6980, 6992, 7013, 7014, 7057]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6032
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6032, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3142, 3173, 6022, 6044, 6057, 6064, 6072, 6080, 6089, 6102, 6109, 6115, 3361, 6140, 3396, 3420, 6158, 6179, 6186, 3507, 6205, 6210, 6238, 3566, 3581, 3604, 3620, 3628, 3639, 6287, 3665, 6300, 3684, 6322, 3718, 6339, 6345, 6353, 3754, 6387, 3808, 6407, 6418, 3848, 6423, 6427, 3900, 3907, 6468, 6494, 6512, 6517, 6549, 6556, 6565, 6591, 6599, 6662, 6714, 6723, 6725, 6731, 6736, 6761, 6765, 6778, 6796, 6802, 6805, 6806, 6825, 6830, 6834, 6840, 6850, 6868, 6874, 6886, 6888, 6904, 6914, 6930, 6934, 6937, 6942, 6980, 6992, 7013, 7014, 7057])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6032
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3142, 3173, 6022, 6044, 6057, 6064, 6072, 6080, 6089, 6102, 6109, 6115, 3361, 6140, 3396, 3420, 6158, 6179, 6186, 3507, 6205, 6210, 6238, 3566, 3581, 3604, 3620, 3628, 3639, 6287, 3665, 6300, 3684, 6322, 3718, 6339, 6345, 6353, 3754, 6387, 3808, 6407, 6418, 3848, 6423, 6427, 3900, 3907, 6468, 6494, 6512, 6517, 6549, 6556, 6565, 6591, 6599, 6662, 6714, 6723, 6725, 6731, 6736, 6761, 6765, 6778, 6796, 6802, 6805, 6806, 6825, 6830, 6834, 6840, 6850, 6868, 6874, 6886, 6888, 6904, 6914, 6930, 6934, 6937, 6942, 6980, 6992, 7013, 7014, 7057]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3142, 3173, 6022, 6044, 6057, 6064, 6072, 6080, 6089, 6102, 6109, 6115, 3361, 6140, 3396, 3420, 6158, 6179, 6186, 3507, 6205, 6210, 6238, 3566, 3581, 3604, 3620, 3628, 3639, 6287, 3665, 6300, 3684, 6322, 3718, 6339, 6345, 6353, 3754, 6387, 3808, 6407, 6418, 3848, 6423, 6427, 3900, 3907, 6468, 6494, 6512, 6517, 6549, 6556, 6565, 6591, 6599, 6662, 6714, 6723, 6725, 6731, 6736, 6761, 6765, 6778, 6796, 6802, 6805, 6806, 6825, 6830, 6834, 6840, 6850, 6868, 6874, 6886, 6888, 6904, 6914, 6930, 6934, 6937, 6942, 6980, 6992, 7013, 7014, 7057]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Други"
--   Total rows: 75  →  canonical: 3993
--   Categories with canonical: 24
--   Products with canonical:   1148
--   Non-canonical ids: ['3167', '3172', '3219', '6024', '6052', '3279', '6060', '6067', '6077', '6081', '6085', '3325', '6096', '6100', '3336', '6106', '3350', '3351', '3392', '6143', '3399', '3404', '6148', '6156', '6171', '3449', '3453', '3508', '6211', '3600', '6276', '6282', '6288', '6324', '6352', '6360', '6372', '6379', '3784', '6389', '3801', '6398', '6405', '6411', '6424', '6428', '6440', '6454', '6457', '6461', '6469', '6475', '6476', '6484', '3958', '3960', '3977', '6505', '6506', '6519', '6536', '6617', '6619', '6624', '6660', '6705', '6878', '6915', '6924', '6985', '6987', '6988', '6989', '7067']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3993
  WHERE parameter_id = ANY(ARRAY[3167, 3172, 3219, 6024, 6052, 3279, 6060, 6067, 6077, 6081, 6085, 3325, 6096, 6100, 3336, 6106, 3350, 3351, 3392, 6143, 3399, 3404, 6148, 6156, 6171, 3449, 3453, 3508, 6211, 3600, 6276, 6282, 6288, 6324, 6352, 6360, 6372, 6379, 3784, 6389, 3801, 6398, 6405, 6411, 6424, 6428, 6440, 6454, 6457, 6461, 6469, 6475, 6476, 6484, 3958, 3960, 3977, 6505, 6506, 6519, 6536, 6617, 6619, 6624, 6660, 6705, 6878, 6915, 6924, 6985, 6987, 6988, 6989, 7067])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3993
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3993,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3993
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3167, 3172, 3219, 6024, 6052, 3279, 6060, 6067, 6077, 6081, 6085, 3325, 6096, 6100, 3336, 6106, 3350, 3351, 3392, 6143, 3399, 3404, 6148, 6156, 6171, 3449, 3453, 3508, 6211, 3600, 6276, 6282, 6288, 6324, 6352, 6360, 6372, 6379, 3784, 6389, 3801, 6398, 6405, 6411, 6424, 6428, 6440, 6454, 6457, 6461, 6469, 6475, 6476, 6484, 3958, 3960, 3977, 6505, 6506, 6519, 6536, 6617, 6619, 6624, 6660, 6705, 6878, 6915, 6924, 6985, 6987, 6988, 6989, 7067]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3993
  WHERE parameter_id = ANY(ARRAY[3167, 3172, 3219, 6024, 6052, 3279, 6060, 6067, 6077, 6081, 6085, 3325, 6096, 6100, 3336, 6106, 3350, 3351, 3392, 6143, 3399, 3404, 6148, 6156, 6171, 3449, 3453, 3508, 6211, 3600, 6276, 6282, 6288, 6324, 6352, 6360, 6372, 6379, 3784, 6389, 3801, 6398, 6405, 6411, 6424, 6428, 6440, 6454, 6457, 6461, 6469, 6475, 6476, 6484, 3958, 3960, 3977, 6505, 6506, 6519, 6536, 6617, 6619, 6624, 6660, 6705, 6878, 6915, 6924, 6985, 6987, 6988, 6989, 7067]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3993
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3993, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3167, 3172, 3219, 6024, 6052, 3279, 6060, 6067, 6077, 6081, 6085, 3325, 6096, 6100, 3336, 6106, 3350, 3351, 3392, 6143, 3399, 3404, 6148, 6156, 6171, 3449, 3453, 3508, 6211, 3600, 6276, 6282, 6288, 6324, 6352, 6360, 6372, 6379, 3784, 6389, 3801, 6398, 6405, 6411, 6424, 6428, 6440, 6454, 6457, 6461, 6469, 6475, 6476, 6484, 3958, 3960, 3977, 6505, 6506, 6519, 6536, 6617, 6619, 6624, 6660, 6705, 6878, 6915, 6924, 6985, 6987, 6988, 6989, 7067])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3993
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3167, 3172, 3219, 6024, 6052, 3279, 6060, 6067, 6077, 6081, 6085, 3325, 6096, 6100, 3336, 6106, 3350, 3351, 3392, 6143, 3399, 3404, 6148, 6156, 6171, 3449, 3453, 3508, 6211, 3600, 6276, 6282, 6288, 6324, 6352, 6360, 6372, 6379, 3784, 6389, 3801, 6398, 6405, 6411, 6424, 6428, 6440, 6454, 6457, 6461, 6469, 6475, 6476, 6484, 3958, 3960, 3977, 6505, 6506, 6519, 6536, 6617, 6619, 6624, 6660, 6705, 6878, 6915, 6924, 6985, 6987, 6988, 6989, 7067]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3167, 3172, 3219, 6024, 6052, 3279, 6060, 6067, 6077, 6081, 6085, 3325, 6096, 6100, 3336, 6106, 3350, 3351, 3392, 6143, 3399, 3404, 6148, 6156, 6171, 3449, 3453, 3508, 6211, 3600, 6276, 6282, 6288, 6324, 6352, 6360, 6372, 6379, 3784, 6389, 3801, 6398, 6405, 6411, 6424, 6428, 6440, 6454, 6457, 6461, 6469, 6475, 6476, 6484, 3958, 3960, 3977, 6505, 6506, 6519, 6536, 6617, 6619, 6624, 6660, 6705, 6878, 6915, 6924, 6985, 6987, 6988, 6989, 7067]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Размери"
--   Total rows: 53  →  canonical: 3211
--   Categories with canonical: 5
--   Products with canonical:   178
--   Non-canonical ids: ['3139', '3168', '3170', '3218', '3252', '3268', '6058', '3278', '6107', '3360', '3372', '3394', '3419', '6155', '6176', '3463', '6184', '6207', '3521', '6226', '6235', '6237', '3571', '3625', '3651', '6323', '3712', '6335', '6344', '6378', '6384', '3927', '6474', '3955', '3992', '6561', '6589', '6627', '6669', '6678', '6699', '6715', '6722', '6754', '6885', '6936', '6951', '6964', '6977', '7004', '7068', '7073']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3211
  WHERE parameter_id = ANY(ARRAY[3139, 3168, 3170, 3218, 3252, 3268, 6058, 3278, 6107, 3360, 3372, 3394, 3419, 6155, 6176, 3463, 6184, 6207, 3521, 6226, 6235, 6237, 3571, 3625, 3651, 6323, 3712, 6335, 6344, 6378, 6384, 3927, 6474, 3955, 3992, 6561, 6589, 6627, 6669, 6678, 6699, 6715, 6722, 6754, 6885, 6936, 6951, 6964, 6977, 7004, 7068, 7073])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3211
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3211,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3211
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3139, 3168, 3170, 3218, 3252, 3268, 6058, 3278, 6107, 3360, 3372, 3394, 3419, 6155, 6176, 3463, 6184, 6207, 3521, 6226, 6235, 6237, 3571, 3625, 3651, 6323, 3712, 6335, 6344, 6378, 6384, 3927, 6474, 3955, 3992, 6561, 6589, 6627, 6669, 6678, 6699, 6715, 6722, 6754, 6885, 6936, 6951, 6964, 6977, 7004, 7068, 7073]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3211
  WHERE parameter_id = ANY(ARRAY[3139, 3168, 3170, 3218, 3252, 3268, 6058, 3278, 6107, 3360, 3372, 3394, 3419, 6155, 6176, 3463, 6184, 6207, 3521, 6226, 6235, 6237, 3571, 3625, 3651, 6323, 3712, 6335, 6344, 6378, 6384, 3927, 6474, 3955, 3992, 6561, 6589, 6627, 6669, 6678, 6699, 6715, 6722, 6754, 6885, 6936, 6951, 6964, 6977, 7004, 7068, 7073]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3211
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3211, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3139, 3168, 3170, 3218, 3252, 3268, 6058, 3278, 6107, 3360, 3372, 3394, 3419, 6155, 6176, 3463, 6184, 6207, 3521, 6226, 6235, 6237, 3571, 3625, 3651, 6323, 3712, 6335, 6344, 6378, 6384, 3927, 6474, 3955, 3992, 6561, 6589, 6627, 6669, 6678, 6699, 6715, 6722, 6754, 6885, 6936, 6951, 6964, 6977, 7004, 7068, 7073])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3211
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3139, 3168, 3170, 3218, 3252, 3268, 6058, 3278, 6107, 3360, 3372, 3394, 3419, 6155, 6176, 3463, 6184, 6207, 3521, 6226, 6235, 6237, 3571, 3625, 3651, 6323, 3712, 6335, 6344, 6378, 6384, 3927, 6474, 3955, 3992, 6561, 6589, 6627, 6669, 6678, 6699, 6715, 6722, 6754, 6885, 6936, 6951, 6964, 6977, 7004, 7068, 7073]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3139, 3168, 3170, 3218, 3252, 3268, 6058, 3278, 6107, 3360, 3372, 3394, 3419, 6155, 6176, 3463, 6184, 6207, 3521, 6226, 6235, 6237, 3571, 3625, 3651, 6323, 3712, 6335, 6344, 6378, 6384, 3927, 6474, 3955, 3992, 6561, 6589, 6627, 6669, 6678, 6699, 6715, 6722, 6754, 6885, 6936, 6951, 6964, 6977, 7004, 7068, 7073]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Материал"
--   Total rows: 28  →  canonical: 6981
--   Categories with canonical: 17
--   Products with canonical:   302
--   Non-canonical ids: ['3169', '3277', '6059', '6091', '6181', '6206', '6243', '6245', '3623', '6317', '3724', '6343', '3746', '3755', '6388', '3849', '6417', '6422', '3906', '6516', '6550', '6555', '6566', '6621', '6772', '6948', '6967']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6981
  WHERE parameter_id = ANY(ARRAY[3169, 3277, 6059, 6091, 6181, 6206, 6243, 6245, 3623, 6317, 3724, 6343, 3746, 3755, 6388, 3849, 6417, 6422, 3906, 6516, 6550, 6555, 6566, 6621, 6772, 6948, 6967])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6981
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6981,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6981
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3169, 3277, 6059, 6091, 6181, 6206, 6243, 6245, 3623, 6317, 3724, 6343, 3746, 3755, 6388, 3849, 6417, 6422, 3906, 6516, 6550, 6555, 6566, 6621, 6772, 6948, 6967]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6981
  WHERE parameter_id = ANY(ARRAY[3169, 3277, 6059, 6091, 6181, 6206, 6243, 6245, 3623, 6317, 3724, 6343, 3746, 3755, 6388, 3849, 6417, 6422, 3906, 6516, 6550, 6555, 6566, 6621, 6772, 6948, 6967]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6981
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6981, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3169, 3277, 6059, 6091, 6181, 6206, 6243, 6245, 3623, 6317, 3724, 6343, 3746, 3755, 6388, 3849, 6417, 6422, 3906, 6516, 6550, 6555, 6566, 6621, 6772, 6948, 6967])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6981
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3169, 3277, 6059, 6091, 6181, 6206, 6243, 6245, 3623, 6317, 3724, 6343, 3746, 3755, 6388, 3849, 6417, 6422, 3906, 6516, 6550, 6555, 6566, 6621, 6772, 6948, 6967]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3169, 3277, 6059, 6091, 6181, 6206, 6243, 6245, 3623, 6317, 3724, 6343, 3746, 3755, 6388, 3849, 6417, 6422, 3906, 6516, 6550, 6555, 6566, 6621, 6772, 6948, 6967]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Интерфейс"
--   Total rows: 27  →  canonical: 6953
--   Categories with canonical: 92
--   Products with canonical:   23
--   Non-canonical ids: ['3129', '3132', '3138', '3163', '5994', '3266', '6114', '6137', '3444', '3450', '6191', '6209', '3519', '6217', '6255', '3598', '6272', '3619', '6295', '6316', '6348', '6419', '6452', '6472', '6628', '6718']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6953
  WHERE parameter_id = ANY(ARRAY[3129, 3132, 3138, 3163, 5994, 3266, 6114, 6137, 3444, 3450, 6191, 6209, 3519, 6217, 6255, 3598, 6272, 3619, 6295, 6316, 6348, 6419, 6452, 6472, 6628, 6718])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6953
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6953,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6953
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3129, 3132, 3138, 3163, 5994, 3266, 6114, 6137, 3444, 3450, 6191, 6209, 3519, 6217, 6255, 3598, 6272, 3619, 6295, 6316, 6348, 6419, 6452, 6472, 6628, 6718]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6953
  WHERE parameter_id = ANY(ARRAY[3129, 3132, 3138, 3163, 5994, 3266, 6114, 6137, 3444, 3450, 6191, 6209, 3519, 6217, 6255, 3598, 6272, 3619, 6295, 6316, 6348, 6419, 6452, 6472, 6628, 6718]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6953
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6953, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3129, 3132, 3138, 3163, 5994, 3266, 6114, 6137, 3444, 3450, 6191, 6209, 3519, 6217, 6255, 3598, 6272, 3619, 6295, 6316, 6348, 6419, 6452, 6472, 6628, 6718])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6953
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3129, 3132, 3138, 3163, 5994, 3266, 6114, 6137, 3444, 3450, 6191, 6209, 3519, 6217, 6255, 3598, 6272, 3619, 6295, 6316, 6348, 6419, 6452, 6472, 6628, 6718]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3129, 3132, 3138, 3163, 5994, 3266, 6114, 6137, 3444, 3450, 6191, 6209, 3519, 6217, 6255, 3598, 6272, 3619, 6295, 6316, 6348, 6419, 6452, 6472, 6628, 6718]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Размери (мм)"
--   Total rows: 26  →  canonical: 3181
--   Categories with canonical: 5
--   Products with canonical:   1172
--   Non-canonical ids: ['5971', '6025', '6084', '6168', '3445', '3480', '6197', '6222', '6258', '3599', '6289', '6303', '6328', '6351', '6365', '6391', '6404', '6412', '6436', '6504', '6511', '6546', '6581', '6583', '6688']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3181
  WHERE parameter_id = ANY(ARRAY[5971, 6025, 6084, 6168, 3445, 3480, 6197, 6222, 6258, 3599, 6289, 6303, 6328, 6351, 6365, 6391, 6404, 6412, 6436, 6504, 6511, 6546, 6581, 6583, 6688])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3181
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3181,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3181
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[5971, 6025, 6084, 6168, 3445, 3480, 6197, 6222, 6258, 3599, 6289, 6303, 6328, 6351, 6365, 6391, 6404, 6412, 6436, 6504, 6511, 6546, 6581, 6583, 6688]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3181
  WHERE parameter_id = ANY(ARRAY[5971, 6025, 6084, 6168, 3445, 3480, 6197, 6222, 6258, 3599, 6289, 6303, 6328, 6351, 6365, 6391, 6404, 6412, 6436, 6504, 6511, 6546, 6581, 6583, 6688]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3181
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3181, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5971, 6025, 6084, 6168, 3445, 3480, 6197, 6222, 6258, 3599, 6289, 6303, 6328, 6351, 6365, 6391, 6404, 6412, 6436, 6504, 6511, 6546, 6581, 6583, 6688])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3181
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5971, 6025, 6084, 6168, 3445, 3480, 6197, 6222, 6258, 3599, 6289, 6303, 6328, 6351, 6365, 6391, 6404, 6412, 6436, 6504, 6511, 6546, 6581, 6583, 6688]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[5971, 6025, 6084, 6168, 3445, 3480, 6197, 6222, 6258, 3599, 6289, 6303, 6328, 6351, 6365, 6391, 6404, 6412, 6436, 6504, 6511, 6546, 6581, 6583, 6688]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип аксесоар"
--   Total rows: 26  →  canonical: 3182
--   Categories with canonical: 5
--   Products with canonical:   1848
--   Non-canonical ids: ['6021', '6065', '6097', '3347', '6141', '3402', '6178', '6199', '6202', '3554', '3572', '6285', '6326', '6356', '6385', '6394', '6400', '6420', '6425', '6466', '6535', '6613', '6615', '6735', '6912']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3182
  WHERE parameter_id = ANY(ARRAY[6021, 6065, 6097, 3347, 6141, 3402, 6178, 6199, 6202, 3554, 3572, 6285, 6326, 6356, 6385, 6394, 6400, 6420, 6425, 6466, 6535, 6613, 6615, 6735, 6912])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3182
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3182,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3182
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6021, 6065, 6097, 3347, 6141, 3402, 6178, 6199, 6202, 3554, 3572, 6285, 6326, 6356, 6385, 6394, 6400, 6420, 6425, 6466, 6535, 6613, 6615, 6735, 6912]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3182
  WHERE parameter_id = ANY(ARRAY[6021, 6065, 6097, 3347, 6141, 3402, 6178, 6199, 6202, 3554, 3572, 6285, 6326, 6356, 6385, 6394, 6400, 6420, 6425, 6466, 6535, 6613, 6615, 6735, 6912]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3182
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3182, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6021, 6065, 6097, 3347, 6141, 3402, 6178, 6199, 6202, 3554, 3572, 6285, 6326, 6356, 6385, 6394, 6400, 6420, 6425, 6466, 6535, 6613, 6615, 6735, 6912])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3182
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6021, 6065, 6097, 3347, 6141, 3402, 6178, 6199, 6202, 3554, 3572, 6285, 6326, 6356, 6385, 6394, 6400, 6420, 6425, 6466, 6535, 6613, 6615, 6735, 6912]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6021, 6065, 6097, 3347, 6141, 3402, 6178, 6199, 6202, 3554, 3572, 6285, 6326, 6356, 6385, 6394, 6400, 6420, 6425, 6466, 6535, 6613, 6615, 6735, 6912]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип продукт"
--   Total rows: 23  →  canonical: 7070
--   Categories with canonical: 53
--   Products with canonical:   823
--   Non-canonical ids: ['6236', '6242', '3607', '3723', '6341', '3757', '6362', '3905', '3944', '6485', '6527', '6590', '6729', '6757', '6773', '6775', '6794', '6831', '6887', '6893', '6899', '6902']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 7070
  WHERE parameter_id = ANY(ARRAY[6236, 6242, 3607, 3723, 6341, 3757, 6362, 3905, 3944, 6485, 6527, 6590, 6729, 6757, 6773, 6775, 6794, 6831, 6887, 6893, 6899, 6902])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 7070
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 7070,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 7070
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6236, 6242, 3607, 3723, 6341, 3757, 6362, 3905, 3944, 6485, 6527, 6590, 6729, 6757, 6773, 6775, 6794, 6831, 6887, 6893, 6899, 6902]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 7070
  WHERE parameter_id = ANY(ARRAY[6236, 6242, 3607, 3723, 6341, 3757, 6362, 3905, 3944, 6485, 6527, 6590, 6729, 6757, 6773, 6775, 6794, 6831, 6887, 6893, 6899, 6902]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 7070
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 7070, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6236, 6242, 3607, 3723, 6341, 3757, 6362, 3905, 3944, 6485, 6527, 6590, 6729, 6757, 6773, 6775, 6794, 6831, 6887, 6893, 6899, 6902])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 7070
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6236, 6242, 3607, 3723, 6341, 3757, 6362, 3905, 3944, 6485, 6527, 6590, 6729, 6757, 6773, 6775, 6794, 6831, 6887, 6893, 6899, 6902]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6236, 6242, 3607, 3723, 6341, 3757, 6362, 3905, 3944, 6485, 6527, 6590, 6729, 6757, 6773, 6775, 6794, 6831, 6887, 6893, 6899, 6902]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Захранване"
--   Total rows: 20  →  canonical: 208
--   Categories with canonical: 44
--   Products with canonical:   106
--   Non-canonical ids: ['3271', '6101', '3393', '6175', '3547', '6263', '3608', '6444', '6483', '6545', '6602', '6695', '6704', '6713', '6956', '6966', '7053', '7062', '3991']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 208
  WHERE parameter_id = ANY(ARRAY[3271, 6101, 3393, 6175, 3547, 6263, 3608, 6444, 6483, 6545, 6602, 6695, 6704, 6713, 6956, 6966, 7053, 7062, 3991])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 208
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 208,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 208
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3271, 6101, 3393, 6175, 3547, 6263, 3608, 6444, 6483, 6545, 6602, 6695, 6704, 6713, 6956, 6966, 7053, 7062, 3991]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 208
  WHERE parameter_id = ANY(ARRAY[3271, 6101, 3393, 6175, 3547, 6263, 3608, 6444, 6483, 6545, 6602, 6695, 6704, 6713, 6956, 6966, 7053, 7062, 3991]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 208
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 208, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3271, 6101, 3393, 6175, 3547, 6263, 3608, 6444, 6483, 6545, 6602, 6695, 6704, 6713, 6956, 6966, 7053, 7062, 3991])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 208
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3271, 6101, 3393, 6175, 3547, 6263, 3608, 6444, 6483, 6545, 6602, 6695, 6704, 6713, 6956, 6966, 7053, 7062, 3991]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3271, 6101, 3393, 6175, 3547, 6263, 3608, 6444, 6483, 6545, 6602, 6695, 6704, 6713, 6956, 6966, 7053, 7062, 3991]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Предназначен за"
--   Total rows: 16  →  canonical: 3764
--   Categories with canonical: 3
--   Products with canonical:   136
--   Non-canonical ids: ['3573', '3603', '3642', '3725', '6342', '3749', '3790', '6730', '6759', '6784', '6791', '6792', '6799', '6804', '6817']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3764
  WHERE parameter_id = ANY(ARRAY[3573, 3603, 3642, 3725, 6342, 3749, 3790, 6730, 6759, 6784, 6791, 6792, 6799, 6804, 6817])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3764
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3764,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3764
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3573, 3603, 3642, 3725, 6342, 3749, 3790, 6730, 6759, 6784, 6791, 6792, 6799, 6804, 6817]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3764
  WHERE parameter_id = ANY(ARRAY[3573, 3603, 3642, 3725, 6342, 3749, 3790, 6730, 6759, 6784, 6791, 6792, 6799, 6804, 6817]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3764
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3764, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3573, 3603, 3642, 3725, 6342, 3749, 3790, 6730, 6759, 6784, 6791, 6792, 6799, 6804, 6817])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3764
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3573, 3603, 3642, 3725, 6342, 3749, 3790, 6730, 6759, 6784, 6791, 6792, 6799, 6804, 6817]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3573, 3603, 3642, 3725, 6342, 3749, 3790, 6730, 6759, 6784, 6791, 6792, 6799, 6804, 6817]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип"
--   Total rows: 14  →  canonical: 3914
--   Categories with canonical: 17
--   Products with canonical:   1355
--   Non-canonical ids: ['3451', '3506', '3534', '6244', '6256', '6260', '3605', '3650', '6346', '6382', '3952', '6514', '6788']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3914
  WHERE parameter_id = ANY(ARRAY[3451, 3506, 3534, 6244, 6256, 6260, 3605, 3650, 6346, 6382, 3952, 6514, 6788])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3914
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3914,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3914
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3451, 3506, 3534, 6244, 6256, 6260, 3605, 3650, 6346, 6382, 3952, 6514, 6788]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3914
  WHERE parameter_id = ANY(ARRAY[3451, 3506, 3534, 6244, 6256, 6260, 3605, 3650, 6346, 6382, 3952, 6514, 6788]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3914
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3914, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3451, 3506, 3534, 6244, 6256, 6260, 3605, 3650, 6346, 6382, 3952, 6514, 6788])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3914
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3451, 3506, 3534, 6244, 6256, 6260, 3605, 3650, 6346, 6382, 3952, 6514, 6788]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3451, 3506, 3534, 6244, 6256, 6260, 3605, 3650, 6346, 6382, 3952, 6514, 6788]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Съвместимост"
--   Total rows: 13  →  canonical: 7066
--   Categories with canonical: 34
--   Products with canonical:   18
--   Non-canonical ids: ['6087', '6133', '6142', '3403', '6200', '6286', '6426', '6467', '3928', '6510', '6913', '6983']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 7066
  WHERE parameter_id = ANY(ARRAY[6087, 6133, 6142, 3403, 6200, 6286, 6426, 6467, 3928, 6510, 6913, 6983])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 7066
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 7066,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 7066
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6087, 6133, 6142, 3403, 6200, 6286, 6426, 6467, 3928, 6510, 6913, 6983]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 7066
  WHERE parameter_id = ANY(ARRAY[6087, 6133, 6142, 3403, 6200, 6286, 6426, 6467, 3928, 6510, 6913, 6983]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 7066
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 7066, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6087, 6133, 6142, 3403, 6200, 6286, 6426, 6467, 3928, 6510, 6913, 6983])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 7066
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6087, 6133, 6142, 3403, 6200, 6286, 6426, 6467, 3928, 6510, 6913, 6983]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6087, 6133, 6142, 3403, 6200, 6286, 6426, 6467, 3928, 6510, 6913, 6983]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Капацитет"
--   Total rows: 12  →  canonical: 3180
--   Categories with canonical: 5
--   Products with canonical:   1688
--   Non-canonical ids: ['6189', '6214', '6279', '3696', '3710', '6403', '3908', '3954', '6489', '6657', '6717']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3180
  WHERE parameter_id = ANY(ARRAY[6189, 6214, 6279, 3696, 3710, 6403, 3908, 3954, 6489, 6657, 6717])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3180
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3180,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3180
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6189, 6214, 6279, 3696, 3710, 6403, 3908, 3954, 6489, 6657, 6717]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3180
  WHERE parameter_id = ANY(ARRAY[6189, 6214, 6279, 3696, 3710, 6403, 3908, 3954, 6489, 6657, 6717]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3180
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3180, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6189, 6214, 6279, 3696, 3710, 6403, 3908, 3954, 6489, 6657, 6717])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3180
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6189, 6214, 6279, 3696, 3710, 6403, 3908, 3954, 6489, 6657, 6717]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6189, 6214, 6279, 3696, 3710, 6403, 3908, 3954, 6489, 6657, 6717]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Конектори"
--   Total rows: 10  →  canonical: 34
--   Categories with canonical: 13
--   Products with canonical:   8
--   Non-canonical ids: ['3222', '3250', '3624', '6357', '6406', '3961', '6520', '6771', '7017']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 34
  WHERE parameter_id = ANY(ARRAY[3222, 3250, 3624, 6357, 6406, 3961, 6520, 6771, 7017])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 34
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 34,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 34
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3222, 3250, 3624, 6357, 6406, 3961, 6520, 6771, 7017]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 34
  WHERE parameter_id = ANY(ARRAY[3222, 3250, 3624, 6357, 6406, 3961, 6520, 6771, 7017]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 34
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 34, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3222, 3250, 3624, 6357, 6406, 3961, 6520, 6771, 7017])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 34
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3222, 3250, 3624, 6357, 6406, 3961, 6520, 6771, 7017]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3222, 3250, 3624, 6357, 6406, 3961, 6520, 6771, 7017]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Цвят."
--   Total rows: 9  →  canonical: 6448
--   Categories with canonical: 3
--   Products with canonical:   22
--   Non-canonical ids: ['6082', '6134', '3719', '6450', '6463', '6521', '6777', '6845']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6448
  WHERE parameter_id = ANY(ARRAY[6082, 6134, 3719, 6450, 6463, 6521, 6777, 6845])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6448
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6448,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6448
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6082, 6134, 3719, 6450, 6463, 6521, 6777, 6845]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6448
  WHERE parameter_id = ANY(ARRAY[6082, 6134, 3719, 6450, 6463, 6521, 6777, 6845]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6448
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6448, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6082, 6134, 3719, 6450, 6463, 6521, 6777, 6845])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6448
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6082, 6134, 3719, 6450, 6463, 6521, 6777, 6845]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6082, 6134, 3719, 6450, 6463, 6521, 6777, 6845]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Съвместим бранд"
--   Total rows: 9  →  canonical: 3693
--   Categories with canonical: 3
--   Products with canonical:   171
--   Non-canonical ids: ['3789', '3945', '6630', '6675', '6758', '6835', '6837', '6884']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3693
  WHERE parameter_id = ANY(ARRAY[3789, 3945, 6630, 6675, 6758, 6835, 6837, 6884])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3693
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3693,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3693
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3789, 3945, 6630, 6675, 6758, 6835, 6837, 6884]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3693
  WHERE parameter_id = ANY(ARRAY[3789, 3945, 6630, 6675, 6758, 6835, 6837, 6884]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3693
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3693, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3789, 3945, 6630, 6675, 6758, 6835, 6837, 6884])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3693
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3789, 3945, 6630, 6675, 6758, 6835, 6837, 6884]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3789, 3945, 6630, 6675, 6758, 6835, 6837, 6884]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Операционна система"
--   Total rows: 8  →  canonical: 69
--   Categories with canonical: 24
--   Products with canonical:   9
--   Non-canonical ids: ['3141', '3269', '3455', '3479', '6453', '6684', '6741']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 69
  WHERE parameter_id = ANY(ARRAY[3141, 3269, 3455, 3479, 6453, 6684, 6741])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 69
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 69,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 69
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3141, 3269, 3455, 3479, 6453, 6684, 6741]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 69
  WHERE parameter_id = ANY(ARRAY[3141, 3269, 3455, 3479, 6453, 6684, 6741]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 69
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 69, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3141, 3269, 3455, 3479, 6453, 6684, 6741])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 69
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3141, 3269, 3455, 3479, 6453, 6684, 6741]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3141, 3269, 3455, 3479, 6453, 6684, 6741]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Функции"
--   Total rows: 8  →  canonical: 3162
--   Categories with canonical: 2
--   Products with canonical:   12
--   Non-canonical ids: ['6050', '3574', '3606', '6364', '6663', '6812', '6818']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3162
  WHERE parameter_id = ANY(ARRAY[6050, 3574, 3606, 6364, 6663, 6812, 6818])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3162
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3162,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3162
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6050, 3574, 3606, 6364, 6663, 6812, 6818]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3162
  WHERE parameter_id = ANY(ARRAY[6050, 3574, 3606, 6364, 6663, 6812, 6818]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3162
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3162, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6050, 3574, 3606, 6364, 6663, 6812, 6818])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3162
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6050, 3574, 3606, 6364, 6663, 6812, 6818]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6050, 3574, 3606, 6364, 6663, 6812, 6818]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Свързване"
--   Total rows: 8  →  canonical: 3263
--   Categories with canonical: 5
--   Products with canonical:   184
--   Non-canonical ids: ['5995', '6152', '6208', '6371', '3879', '3926', '6585']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3263
  WHERE parameter_id = ANY(ARRAY[5995, 6152, 6208, 6371, 3879, 3926, 6585])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3263
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3263,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3263
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[5995, 6152, 6208, 6371, 3879, 3926, 6585]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3263
  WHERE parameter_id = ANY(ARRAY[5995, 6152, 6208, 6371, 3879, 3926, 6585]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3263
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3263, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5995, 6152, 6208, 6371, 3879, 3926, 6585])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3263
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5995, 6152, 6208, 6371, 3879, 3926, 6585]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[5995, 6152, 6208, 6371, 3879, 3926, 6585]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Дължина"
--   Total rows: 8  →  canonical: 6006
--   Categories with canonical: 2
--   Products with canonical:   0
--   Non-canonical ids: ['6547', '6664', '6786', '6839', '6848', '6865', '7012']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6006
  WHERE parameter_id = ANY(ARRAY[6547, 6664, 6786, 6839, 6848, 6865, 7012])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6006
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6006,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6006
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6547, 6664, 6786, 6839, 6848, 6865, 7012]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6006
  WHERE parameter_id = ANY(ARRAY[6547, 6664, 6786, 6839, 6848, 6865, 7012]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6006
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6006, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6547, 6664, 6786, 6839, 6848, 6865, 7012])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6006
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6547, 6664, 6786, 6839, 6848, 6865, 7012]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6547, 6664, 6786, 6839, 6848, 6865, 7012]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Технология"
--   Total rows: 8  →  canonical: 57
--   Categories with canonical: 19
--   Products with canonical:   65
--   Non-canonical ids: ['3212', '6029', '6135', '6349', '6706', '6875', '6994']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 57
  WHERE parameter_id = ANY(ARRAY[3212, 6029, 6135, 6349, 6706, 6875, 6994])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 57
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 57,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 57
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3212, 6029, 6135, 6349, 6706, 6875, 6994]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 57
  WHERE parameter_id = ANY(ARRAY[3212, 6029, 6135, 6349, 6706, 6875, 6994]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 57
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 57, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3212, 6029, 6135, 6349, 6706, 6875, 6994])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 57
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3212, 6029, 6135, 6349, 6706, 6875, 6994]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3212, 6029, 6135, 6349, 6706, 6875, 6994]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Процесор"
--   Total rows: 8  →  canonical: 119
--   Categories with canonical: 62
--   Products with canonical:   655
--   Non-canonical ids: ['6039', '3517', '3876', '6666', '6689', '6766', '6917']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 119
  WHERE parameter_id = ANY(ARRAY[6039, 3517, 3876, 6666, 6689, 6766, 6917])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 119
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 119,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 119
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6039, 3517, 3876, 6666, 6689, 6766, 6917]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 119
  WHERE parameter_id = ANY(ARRAY[6039, 3517, 3876, 6666, 6689, 6766, 6917]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 119
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 119, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6039, 3517, 3876, 6666, 6689, 6766, 6917])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 119
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6039, 3517, 3876, 6666, 6689, 6766, 6917]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6039, 3517, 3876, 6666, 6689, 6766, 6917]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Свързаност"
--   Total rows: 8  →  canonical: 3986
--   Categories with canonical: 3
--   Products with canonical:   72
--   Non-canonical ids: ['6043', '6099', '6110', '6157', '6623', '6876', '7064']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3986
  WHERE parameter_id = ANY(ARRAY[6043, 6099, 6110, 6157, 6623, 6876, 7064])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3986
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3986,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3986
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6043, 6099, 6110, 6157, 6623, 6876, 7064]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3986
  WHERE parameter_id = ANY(ARRAY[6043, 6099, 6110, 6157, 6623, 6876, 7064]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3986
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3986, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6043, 6099, 6110, 6157, 6623, 6876, 7064])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3986
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6043, 6099, 6110, 6157, 6623, 6876, 7064]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6043, 6099, 6110, 6157, 6623, 6876, 7064]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "ОС"
--   Total rows: 8  →  canonical: 289
--   Categories with canonical: 19
--   Products with canonical:   256
--   Non-canonical ids: ['3411', '6296', '3856', '6525', '6671', '6716', '6907']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 289
  WHERE parameter_id = ANY(ARRAY[3411, 6296, 3856, 6525, 6671, 6716, 6907])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 289
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 289,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 289
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3411, 6296, 3856, 6525, 6671, 6716, 6907]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 289
  WHERE parameter_id = ANY(ARRAY[3411, 6296, 3856, 6525, 6671, 6716, 6907]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 289
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 289, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3411, 6296, 3856, 6525, 6671, 6716, 6907])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 289
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3411, 6296, 3856, 6525, 6671, 6716, 6907]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3411, 6296, 3856, 6525, 6671, 6716, 6907]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Портове"
--   Total rows: 8  →  canonical: 3915
--   Categories with canonical: 19
--   Products with canonical:   2527
--   Non-canonical ids: ['6183', '6473', '6479', '6541', '6542', '6694', '6746']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3915
  WHERE parameter_id = ANY(ARRAY[6183, 6473, 6479, 6541, 6542, 6694, 6746])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3915
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3915,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3915
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6183, 6473, 6479, 6541, 6542, 6694, 6746]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3915
  WHERE parameter_id = ANY(ARRAY[6183, 6473, 6479, 6541, 6542, 6694, 6746]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3915
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3915, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6183, 6473, 6479, 6541, 6542, 6694, 6746])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3915
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6183, 6473, 6479, 6541, 6542, 6694, 6746]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6183, 6473, 6479, 6541, 6542, 6694, 6746]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип продукт."
--   Total rows: 8  →  canonical: 3666
--   Categories with canonical: 4
--   Products with canonical:   62
--   Non-canonical ids: ['6442', '6458', '6597', '6836', '6844', '6849', '6855']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3666
  WHERE parameter_id = ANY(ARRAY[6442, 6458, 6597, 6836, 6844, 6849, 6855])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3666
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3666,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3666
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6442, 6458, 6597, 6836, 6844, 6849, 6855]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3666
  WHERE parameter_id = ANY(ARRAY[6442, 6458, 6597, 6836, 6844, 6849, 6855]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3666
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3666, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6442, 6458, 6597, 6836, 6844, 6849, 6855])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3666
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6442, 6458, 6597, 6836, 6844, 6849, 6855]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6442, 6458, 6597, 6836, 6844, 6849, 6855]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Съвместими модели"
--   Total rows: 7  →  canonical: 3570
--   Categories with canonical: 12
--   Products with canonical:   860
--   Non-canonical ids: ['6034', '6056', '3369', '6203', '3694', '6838']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3570
  WHERE parameter_id = ANY(ARRAY[6034, 6056, 3369, 6203, 3694, 6838])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3570
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3570,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3570
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6034, 6056, 3369, 6203, 3694, 6838]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3570
  WHERE parameter_id = ANY(ARRAY[6034, 6056, 3369, 6203, 3694, 6838]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3570
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3570, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6034, 6056, 3369, 6203, 3694, 6838])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3570
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6034, 6056, 3369, 6203, 3694, 6838]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6034, 6056, 3369, 6203, 3694, 6838]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Дължина кабел"
--   Total rows: 7  →  canonical: 3262
--   Categories with canonical: 5
--   Products with canonical:   124
--   Non-canonical ids: ['6159', '3576', '6445', '6588', '6863', '6872']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3262
  WHERE parameter_id = ANY(ARRAY[6159, 3576, 6445, 6588, 6863, 6872])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3262
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3262,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3262
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6159, 3576, 6445, 6588, 6863, 6872]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3262
  WHERE parameter_id = ANY(ARRAY[6159, 3576, 6445, 6588, 6863, 6872]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3262
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3262, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6159, 3576, 6445, 6588, 6863, 6872])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3262
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6159, 3576, 6445, 6588, 6863, 6872]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6159, 3576, 6445, 6588, 6863, 6872]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип памет"
--   Total rows: 6  →  canonical: 3470
--   Categories with canonical: 5
--   Products with canonical:   68
--   Non-canonical ids: ['5992', '3413', '6355', '6604', '6655']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3470
  WHERE parameter_id = ANY(ARRAY[5992, 3413, 6355, 6604, 6655])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3470
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3470,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3470
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[5992, 3413, 6355, 6604, 6655]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3470
  WHERE parameter_id = ANY(ARRAY[5992, 3413, 6355, 6604, 6655]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3470
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3470, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5992, 3413, 6355, 6604, 6655])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3470
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5992, 3413, 6355, 6604, 6655]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[5992, 3413, 6355, 6604, 6655]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Памет"
--   Total rows: 6  →  canonical: 117
--   Categories with canonical: 12
--   Products with canonical:   64
--   Non-canonical ids: ['3198', '3518', '6646', '6696', '7022']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 117
  WHERE parameter_id = ANY(ARRAY[3198, 3518, 6646, 6696, 7022])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 117
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 117,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 117
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3198, 3518, 6646, 6696, 7022]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 117
  WHERE parameter_id = ANY(ARRAY[3198, 3518, 6646, 6696, 7022]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 117
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 117, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3198, 3518, 6646, 6696, 7022])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 117
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3198, 3518, 6646, 6696, 7022]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3198, 3518, 6646, 6696, 7022]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Съвместими телефони"
--   Total rows: 6  →  canonical: 6098
--   Categories with canonical: 3
--   Products with canonical:   6
--   Non-canonical ids: ['6023', '6386', '6421', '6515', '6622']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6098
  WHERE parameter_id = ANY(ARRAY[6023, 6386, 6421, 6515, 6622])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6098
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6098,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6098
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6023, 6386, 6421, 6515, 6622]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6098
  WHERE parameter_id = ANY(ARRAY[6023, 6386, 6421, 6515, 6622]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6098
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6098, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6023, 6386, 6421, 6515, 6622])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6098
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6023, 6386, 6421, 6515, 6622]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6023, 6386, 6421, 6515, 6622]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Батерия"
--   Total rows: 6  →  canonical: 3878
--   Categories with canonical: 9
--   Products with canonical:   1714
--   Non-canonical ids: ['6038', '3417', '3748', '6435', '6614']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3878
  WHERE parameter_id = ANY(ARRAY[6038, 3417, 3748, 6435, 6614])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3878
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3878,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3878
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6038, 3417, 3748, 6435, 6614]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3878
  WHERE parameter_id = ANY(ARRAY[6038, 3417, 3748, 6435, 6614]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3878
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3878, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6038, 3417, 3748, 6435, 6614])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3878
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6038, 3417, 3748, 6435, 6614]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6038, 3417, 3748, 6435, 6614]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Форм фактор"
--   Total rows: 6  →  canonical: 7027
--   Categories with canonical: 80
--   Products with canonical:   693
--   Non-canonical ids: ['3253', '6194', '6219', '6719', '7021']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 7027
  WHERE parameter_id = ANY(ARRAY[3253, 6194, 6219, 6719, 7021])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 7027
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 7027,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 7027
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3253, 6194, 6219, 6719, 7021]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 7027
  WHERE parameter_id = ANY(ARRAY[3253, 6194, 6219, 6719, 7021]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 7027
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 7027, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3253, 6194, 6219, 6719, 7021])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 7027
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3253, 6194, 6219, 6719, 7021]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3253, 6194, 6219, 6719, 7021]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Резолюция"
--   Total rows: 6  →  canonical: 3410
--   Categories with canonical: 4
--   Products with canonical:   142
--   Non-canonical ids: ['6053', '3622', '6748', '6768', '6995']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3410
  WHERE parameter_id = ANY(ARRAY[6053, 3622, 6748, 6768, 6995])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3410
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3410,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3410
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6053, 3622, 6748, 6768, 6995]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3410
  WHERE parameter_id = ANY(ARRAY[6053, 3622, 6748, 6768, 6995]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3410
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3410, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6053, 3622, 6748, 6768, 6995])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3410
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6053, 3622, 6748, 6768, 6995]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6053, 3622, 6748, 6768, 6995]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Дисплей"
--   Total rows: 6  →  canonical: 3985
--   Categories with canonical: 6
--   Products with canonical:   102
--   Non-canonical ids: ['3397', '6522', '6926', '7043', '7063']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3985
  WHERE parameter_id = ANY(ARRAY[3397, 6522, 6926, 7043, 7063])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3985
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3985,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3985
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3397, 6522, 6926, 7043, 7063]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3985
  WHERE parameter_id = ANY(ARRAY[3397, 6522, 6926, 7043, 7063]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3985
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3985, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3397, 6522, 6926, 7043, 7063])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3985
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3397, 6522, 6926, 7043, 7063]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3397, 6522, 6926, 7043, 7063]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Сигурност"
--   Total rows: 6  →  canonical: 3418
--   Categories with canonical: 4
--   Products with canonical:   94
--   Non-canonical ids: ['6262', '6291', '6482', '6703', '7065']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3418
  WHERE parameter_id = ANY(ARRAY[6262, 6291, 6482, 6703, 7065])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3418
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3418,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3418
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6262, 6291, 6482, 6703, 7065]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3418
  WHERE parameter_id = ANY(ARRAY[6262, 6291, 6482, 6703, 7065]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3418
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3418, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6262, 6291, 6482, 6703, 7065])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3418
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6262, 6291, 6482, 6703, 7065]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6262, 6291, 6482, 6703, 7065]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Бранд"
--   Total rows: 6  →  canonical: 6763
--   Categories with canonical: 3
--   Products with canonical:   85
--   Non-canonical ids: ['6632', '6634', '6635', '6642', '6816']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6763
  WHERE parameter_id = ANY(ARRAY[6632, 6634, 6635, 6642, 6816])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6763
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6763,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6763
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6632, 6634, 6635, 6642, 6816]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6763
  WHERE parameter_id = ANY(ARRAY[6632, 6634, 6635, 6642, 6816]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6763
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6763, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6632, 6634, 6635, 6642, 6816])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6763
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6632, 6634, 6635, 6642, 6816]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6632, 6634, 6635, 6642, 6816]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Микрофон"
--   Total rows: 5  →  canonical: 3335
--   Categories with canonical: 5
--   Products with canonical:   262
--   Non-canonical ids: ['5997', '3422', '6567', '6873']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3335
  WHERE parameter_id = ANY(ARRAY[5997, 3422, 6567, 6873])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3335
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3335,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3335
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[5997, 3422, 6567, 6873]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3335
  WHERE parameter_id = ANY(ARRAY[5997, 3422, 6567, 6873]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3335
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3335, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5997, 3422, 6567, 6873])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3335
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5997, 3422, 6567, 6873]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[5997, 3422, 6567, 6873]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Размери (mm)"
--   Total rows: 5  →  canonical: 134
--   Categories with canonical: 6
--   Products with canonical:   404
--   Non-canonical ids: ['3208', '6275', '6576', '6949']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 134
  WHERE parameter_id = ANY(ARRAY[3208, 6275, 6576, 6949])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 134
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 134,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 134
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3208, 6275, 6576, 6949]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 134
  WHERE parameter_id = ANY(ARRAY[3208, 6275, 6576, 6949]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 134
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 134, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3208, 6275, 6576, 6949])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 134
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3208, 6275, 6576, 6949]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3208, 6275, 6576, 6949]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Приложение"
--   Total rows: 5  →  canonical: 3210
--   Categories with canonical: 8
--   Products with canonical:   232
--   Non-canonical ids: ['3216', '3267', '6144', '3956']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3210
  WHERE parameter_id = ANY(ARRAY[3216, 3267, 6144, 3956])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3210
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3210,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3210
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3216, 3267, 6144, 3956]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3210
  WHERE parameter_id = ANY(ARRAY[3216, 3267, 6144, 3956]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3210
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3210, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3216, 3267, 6144, 3956])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3210
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3216, 3267, 6144, 3956]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3216, 3267, 6144, 3956]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Екстри"
--   Total rows: 5  →  canonical: 3899
--   Categories with canonical: 3
--   Products with canonical:   528
--   Non-canonical ids: ['6185', '6376', '6946', '6950']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3899
  WHERE parameter_id = ANY(ARRAY[6185, 6376, 6946, 6950])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3899
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3899,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3899
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6185, 6376, 6946, 6950]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3899
  WHERE parameter_id = ANY(ARRAY[6185, 6376, 6946, 6950]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3899
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3899, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6185, 6376, 6946, 6950])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3899
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6185, 6376, 6946, 6950]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6185, 6376, 6946, 6950]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Употреба"
--   Total rows: 5  →  canonical: 3716
--   Categories with canonical: 5
--   Products with canonical:   252
--   Non-canonical ids: ['6234', '3575', '3594', '6728']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3716
  WHERE parameter_id = ANY(ARRAY[6234, 3575, 3594, 6728])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3716
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3716,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3716
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6234, 3575, 3594, 6728]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3716
  WHERE parameter_id = ANY(ARRAY[6234, 3575, 3594, 6728]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3716
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3716, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6234, 3575, 3594, 6728])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3716
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6234, 3575, 3594, 6728]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6234, 3575, 3594, 6728]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Комплектът включва"
--   Total rows: 5  →  canonical: 1139
--   Categories with canonical: 7
--   Products with canonical:   0
--   Non-canonical ids: ['3611', '6821', '6822', '6843']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 1139
  WHERE parameter_id = ANY(ARRAY[3611, 6821, 6822, 6843])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 1139
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 1139,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 1139
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3611, 6821, 6822, 6843]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 1139
  WHERE parameter_id = ANY(ARRAY[3611, 6821, 6822, 6843]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 1139
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 1139, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3611, 6821, 6822, 6843])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 1139
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3611, 6821, 6822, 6843]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3611, 6821, 6822, 6843]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Трансфер на данни"
--   Total rows: 5  →  canonical: 3711
--   Categories with canonical: 3
--   Products with canonical:   102
--   Non-canonical ids: ['6257', '6638', '6854', '6947']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3711
  WHERE parameter_id = ANY(ARRAY[6257, 6638, 6854, 6947])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3711
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3711,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3711
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6257, 6638, 6854, 6947]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3711
  WHERE parameter_id = ANY(ARRAY[6257, 6638, 6854, 6947]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3711
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3711, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6257, 6638, 6854, 6947])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3711
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6257, 6638, 6854, 6947]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6257, 6638, 6854, 6947]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "За"
--   Total rows: 5  →  canonical: 7007
--   Categories with canonical: 3
--   Products with canonical:   49
--   Non-canonical ids: ['3761', '6524', '6776', '7018']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 7007
  WHERE parameter_id = ANY(ARRAY[3761, 6524, 6776, 7018])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 7007
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 7007,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 7007
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3761, 6524, 6776, 7018]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 7007
  WHERE parameter_id = ANY(ARRAY[3761, 6524, 6776, 7018]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 7007
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 7007, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3761, 6524, 6776, 7018])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 7007
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3761, 6524, 6776, 7018]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3761, 6524, 6776, 7018]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Резба"
--   Total rows: 5  →  canonical: 6979
--   Categories with canonical: 1
--   Products with canonical:   304
--   Non-canonical ids: ['6929', '6933', '6970', '6976']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6979
  WHERE parameter_id = ANY(ARRAY[6929, 6933, 6970, 6976])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6979
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6979,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6979
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6929, 6933, 6970, 6976]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6979
  WHERE parameter_id = ANY(ARRAY[6929, 6933, 6970, 6976]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6979
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6979, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6929, 6933, 6970, 6976])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6979
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6929, 6933, 6970, 6976]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6929, 6933, 6970, 6976]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Волтаж"
--   Total rows: 4  →  canonical: 3179
--   Categories with canonical: 5
--   Products with canonical:   1724
--   Non-canonical ids: ['5989', '3627', '6658']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3179
  WHERE parameter_id = ANY(ARRAY[5989, 3627, 6658])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3179
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3179,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3179
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[5989, 3627, 6658]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3179
  WHERE parameter_id = ANY(ARRAY[5989, 3627, 6658]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3179
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3179, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5989, 3627, 6658])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3179
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5989, 3627, 6658]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[5989, 3627, 6658]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Яркост"
--   Total rows: 4  →  canonical: 3857
--   Categories with canonical: 15
--   Products with canonical:   29
--   Non-canonical ids: ['5990', '3171', '6337']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3857
  WHERE parameter_id = ANY(ARRAY[5990, 3171, 6337])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3857
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3857,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3857
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[5990, 3171, 6337]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3857
  WHERE parameter_id = ANY(ARRAY[5990, 3171, 6337]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3857
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3857, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5990, 3171, 6337])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3857
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5990, 3171, 6337]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[5990, 3171, 6337]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Брой цветове"
--   Total rows: 4  →  canonical: 6708
--   Categories with canonical: 5
--   Products with canonical:   1772
--   Non-canonical ids: ['6011', '6051', '6750']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6708
  WHERE parameter_id = ANY(ARRAY[6011, 6051, 6750])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6708
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6708,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6708
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6011, 6051, 6750]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6708
  WHERE parameter_id = ANY(ARRAY[6011, 6051, 6750]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6708
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6708, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6011, 6051, 6750])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6708
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6011, 6051, 6750]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6011, 6051, 6750]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Аудио"
--   Total rows: 4  →  canonical: 3414
--   Categories with canonical: 4
--   Products with canonical:   192
--   Non-canonical ids: ['6042', '3877', '6540']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3414
  WHERE parameter_id = ANY(ARRAY[6042, 3877, 6540])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3414
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3414,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3414
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6042, 3877, 6540]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3414
  WHERE parameter_id = ANY(ARRAY[6042, 3877, 6540]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3414
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3414, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6042, 3877, 6540])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3414
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6042, 3877, 6540]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6042, 3877, 6540]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Свързаност."
--   Total rows: 4  →  canonical: 3464
--   Categories with canonical: 5
--   Products with canonical:   78
--   Non-canonical ids: ['6086', '6432', '6898']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3464
  WHERE parameter_id = ANY(ARRAY[6086, 6432, 6898])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3464
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3464,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3464
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6086, 6432, 6898]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3464
  WHERE parameter_id = ANY(ARRAY[6086, 6432, 6898]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3464
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3464, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6086, 6432, 6898])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3464
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6086, 6432, 6898]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6086, 6432, 6898]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Скорост"
--   Total rows: 4  →  canonical: 6190
--   Categories with canonical: 1
--   Products with canonical:   390
--   Non-canonical ids: ['6864', '6897', '7051']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6190
  WHERE parameter_id = ANY(ARRAY[6864, 6897, 7051])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6190
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6190,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6190
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6864, 6897, 7051]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6190
  WHERE parameter_id = ANY(ARRAY[6864, 6897, 7051]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6190
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6190, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6864, 6897, 7051])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6190
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6864, 6897, 7051]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6864, 6897, 7051]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Честота (GHz)"
--   Total rows: 4  →  canonical: 6478
--   Categories with canonical: 1
--   Products with canonical:   358
--   Non-canonical ids: ['6290', '6702', '6896']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6478
  WHERE parameter_id = ANY(ARRAY[6290, 6702, 6896])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6478
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6478,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6478
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6290, 6702, 6896]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6478
  WHERE parameter_id = ANY(ARRAY[6290, 6702, 6896]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6478
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6478, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6290, 6702, 6896])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6478
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6290, 6702, 6896]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6290, 6702, 6896]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Изходно напрежение"
--   Total rows: 4  →  canonical: 325
--   Categories with canonical: 49
--   Products with canonical:   310
--   Non-canonical ids: ['6330', '3953', '7000']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 325
  WHERE parameter_id = ANY(ARRAY[6330, 3953, 7000])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 325
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 325,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 325
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6330, 3953, 7000]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 325
  WHERE parameter_id = ANY(ARRAY[6330, 3953, 7000]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 325
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 325, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6330, 3953, 7000])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 325
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6330, 3953, 7000]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6330, 3953, 7000]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Съдържание"
--   Total rows: 4  →  canonical: 3842
--   Categories with canonical: 3
--   Products with canonical:   320
--   Non-canonical ids: ['3844', '6880', '6890']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3842
  WHERE parameter_id = ANY(ARRAY[3844, 6880, 6890])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3842
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3842,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3842
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3844, 6880, 6890]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3842
  WHERE parameter_id = ANY(ARRAY[3844, 6880, 6890]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3842
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3842, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3844, 6880, 6890])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3842
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3844, 6880, 6890]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3844, 6880, 6890]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Количество"
--   Total rows: 4  →  canonical: 627
--   Categories with canonical: 4
--   Products with canonical:   328
--   Non-canonical ids: ['3957', '6491', '6991']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 627
  WHERE parameter_id = ANY(ARRAY[3957, 6491, 6991])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 627
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 627,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 627
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3957, 6491, 6991]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 627
  WHERE parameter_id = ANY(ARRAY[3957, 6491, 6991]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 627
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 627, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3957, 6491, 6991])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 627
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3957, 6491, 6991]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3957, 6491, 6991]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Размер"
--   Total rows: 4  →  canonical: 6990
--   Categories with canonical: 1
--   Products with canonical:   8
--   Non-canonical ids: ['3959', '6762', '7058']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6990
  WHERE parameter_id = ANY(ARRAY[3959, 6762, 7058])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6990
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6990,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6990
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3959, 6762, 7058]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6990
  WHERE parameter_id = ANY(ARRAY[3959, 6762, 7058]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6990
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6990, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3959, 6762, 7058])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6990
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3959, 6762, 7058]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3959, 6762, 7058]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Диаметър"
--   Total rows: 4  →  canonical: 985
--   Categories with canonical: 40
--   Products with canonical:   56
--   Non-canonical ids: ['6548', '6616', '6732']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 985
  WHERE parameter_id = ANY(ARRAY[6548, 6616, 6732])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 985
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 985,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 985
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6548, 6616, 6732]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 985
  WHERE parameter_id = ANY(ARRAY[6548, 6616, 6732]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 985
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 985, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6548, 6616, 6732])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 985
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6548, 6616, 6732]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6548, 6616, 6732]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Конектор 1"
--   Total rows: 4  →  canonical: 6846
--   Categories with canonical: 2
--   Products with canonical:   0
--   Non-canonical ids: ['6851', '6861', '6866']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6846
  WHERE parameter_id = ANY(ARRAY[6851, 6861, 6866])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6846
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6846,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6846
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6851, 6861, 6866]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6846
  WHERE parameter_id = ANY(ARRAY[6851, 6861, 6866]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6846
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6846, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6851, 6861, 6866])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6846
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6851, 6861, 6866]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6851, 6861, 6866]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Конектор 2"
--   Total rows: 4  →  canonical: 6847
--   Categories with canonical: 2
--   Products with canonical:   0
--   Non-canonical ids: ['6852', '6862', '6867']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6847
  WHERE parameter_id = ANY(ARRAY[6852, 6862, 6867])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6847
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6847,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6847
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6852, 6862, 6867]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6847
  WHERE parameter_id = ANY(ARRAY[6852, 6862, 6867]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6847
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6847, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6852, 6862, 6867])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6847
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6852, 6862, 6867]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6852, 6862, 6867]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Консумирана мощност"
--   Total rows: 3  →  canonical: 3164
--   Categories with canonical: 5
--   Products with canonical:   216
--   Non-canonical ids: ['3140', '6008']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3164
  WHERE parameter_id = ANY(ARRAY[3140, 6008])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3164
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3164,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3164
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3140, 6008]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3164
  WHERE parameter_id = ANY(ARRAY[3140, 6008]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3164
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3164, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3140, 6008])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3164
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3140, 6008]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3140, 6008]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Размер на екрана"
--   Total rows: 3  →  canonical: 3409
--   Categories with canonical: 6
--   Products with canonical:   1820
--   Non-canonical ids: ['6000', '6047']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3409
  WHERE parameter_id = ANY(ARRAY[6000, 6047])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3409
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3409,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3409
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6000, 6047]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3409
  WHERE parameter_id = ANY(ARRAY[6000, 6047]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3409
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3409, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6000, 6047])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3409
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6000, 6047]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6000, 6047]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Регулиране височина"
--   Total rows: 3  →  canonical: 6020
--   Categories with canonical: 2
--   Products with canonical:   819
--   Non-canonical ids: ['6090', '6592']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6020
  WHERE parameter_id = ANY(ARRAY[6090, 6592])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6020
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6020,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6020
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6090, 6592]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6020
  WHERE parameter_id = ANY(ARRAY[6090, 6592]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6020
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6020, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6090, 6592])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6020
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6090, 6592]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6090, 6592]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Ниво на шум"
--   Total rows: 3  →  canonical: 3217
--   Categories with canonical: 2
--   Products with canonical:   596
--   Non-canonical ids: ['6147', '3833']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3217
  WHERE parameter_id = ANY(ARRAY[6147, 3833])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3217
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3217,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3217
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6147, 3833]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3217
  WHERE parameter_id = ANY(ARRAY[6147, 3833]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3217
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3217, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6147, 3833])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3217
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6147, 3833]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6147, 3833]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Височина"
--   Total rows: 3  →  canonical: 6833
--   Categories with canonical: 258
--   Products with canonical:   1136
--   Non-canonical ids: ['6040', '6727']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6833
  WHERE parameter_id = ANY(ARRAY[6040, 6727])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6833
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6833,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6833
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6040, 6727]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6833
  WHERE parameter_id = ANY(ARRAY[6040, 6727]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6833
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6833, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6040, 6727])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6833
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6040, 6727]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6040, 6727]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Мощност"
--   Total rows: 3  →  canonical: 317
--   Categories with canonical: 5
--   Products with canonical:   0
--   Non-canonical ids: ['3249', '3834']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 317
  WHERE parameter_id = ANY(ARRAY[3249, 3834])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 317
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 317,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 317
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3249, 3834]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 317
  WHERE parameter_id = ANY(ARRAY[3249, 3834]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 317
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 317, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3249, 3834])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 317
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3249, 3834]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3249, 3834]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Входно напрежение"
--   Total rows: 3  →  canonical: 324
--   Categories with canonical: 8
--   Products with canonical:   216
--   Non-canonical ids: ['3251', '6999']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 324
  WHERE parameter_id = ANY(ARRAY[3251, 6999])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 324
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 324,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 324
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3251, 6999]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 324
  WHERE parameter_id = ANY(ARRAY[3251, 6999]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 324
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 324, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3251, 6999])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 324
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3251, 6999]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3251, 6999]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Чувствителност"
--   Total rows: 3  →  canonical: 6993
--   Categories with canonical: 12
--   Products with canonical:   1
--   Non-canonical ids: ['3265', '6641']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6993
  WHERE parameter_id = ANY(ARRAY[3265, 6641])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6993
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6993,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6993
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3265, 6641]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6993
  WHERE parameter_id = ANY(ARRAY[3265, 6641]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6993
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6993, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3265, 6641])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6993
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3265, 6641]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3265, 6641]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Сензор"
--   Total rows: 3  →  canonical: 3334
--   Categories with canonical: 4
--   Products with canonical:   200
--   Non-canonical ids: ['3270', '6823']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3334
  WHERE parameter_id = ANY(ARRAY[3270, 6823])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3334
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3334,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3334
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3270, 6823]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3334
  WHERE parameter_id = ANY(ARRAY[3270, 6823]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3334
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3334, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3270, 6823])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3334
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3270, 6823]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3270, 6823]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Аларма"
--   Total rows: 3  →  canonical: 610
--   Categories with canonical: 7
--   Products with canonical:   178
--   Non-canonical ids: ['3362', '3395']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 610
  WHERE parameter_id = ANY(ARRAY[3362, 3395])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 610
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 610,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 610
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3362, 3395]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 610
  WHERE parameter_id = ANY(ARRAY[3362, 3395]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 610
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 610, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3362, 3395])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 610
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3362, 3395]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3362, 3395]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Химически състав"
--   Total rows: 3  →  canonical: 244
--   Categories with canonical: 10
--   Products with canonical:   1840
--   Non-canonical ids: ['3370', '3569']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 244
  WHERE parameter_id = ANY(ARRAY[3370, 3569])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 244
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 244,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 244
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3370, 3569]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 244
  WHERE parameter_id = ANY(ARRAY[3370, 3569]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 244
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 244, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3370, 3569])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 244
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3370, 3569]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3370, 3569]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Напрежение (V)"
--   Total rows: 3  →  canonical: 3567
--   Categories with canonical: 4
--   Products with canonical:   220
--   Non-canonical ids: ['3371', '6325']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3567
  WHERE parameter_id = ANY(ARRAY[3371, 6325])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3567
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3567,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3567
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3371, 6325]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3567
  WHERE parameter_id = ANY(ARRAY[3371, 6325]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3567
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3567, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3371, 6325])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3567
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3371, 6325]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3371, 6325]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "RAM памет"
--   Total rows: 3  →  canonical: 3472
--   Categories with canonical: 6
--   Products with canonical:   82
--   Non-canonical ids: ['3408', '6737']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3472
  WHERE parameter_id = ANY(ARRAY[3408, 6737])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3472
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3472,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3472
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3408, 6737]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3472
  WHERE parameter_id = ANY(ARRAY[3408, 6737]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3472
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3472, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3408, 6737])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3472
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3408, 6737]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3408, 6737]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Брой ядра"
--   Total rows: 3  →  canonical: 737
--   Categories with canonical: 12
--   Products with canonical:   211
--   Non-canonical ids: ['3468', '6769']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 737
  WHERE parameter_id = ANY(ARRAY[3468, 6769])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 737
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 737,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 737
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3468, 6769]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 737
  WHERE parameter_id = ANY(ARRAY[3468, 6769]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 737
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 737, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3468, 6769])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 737
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3468, 6769]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3468, 6769]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Видео чипсет"
--   Total rows: 3  →  canonical: 848
--   Categories with canonical: 5
--   Products with canonical:   110
--   Non-canonical ids: ['3474', '3835']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 848
  WHERE parameter_id = ANY(ARRAY[3474, 3835])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 848
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 848,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 848
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3474, 3835]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 848
  WHERE parameter_id = ANY(ARRAY[3474, 3835]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 848
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 848, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3474, 3835])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 848
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3474, 3835]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3474, 3835]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Мрежа"
--   Total rows: 3  →  canonical: 750
--   Categories with canonical: 16
--   Products with canonical:   113
--   Non-canonical ids: ['3477', '3516']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 750
  WHERE parameter_id = ANY(ARRAY[3477, 3516])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 750
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 750,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 750
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3477, 3516]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 750
  WHERE parameter_id = ANY(ARRAY[3477, 3516]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 750
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 750, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3477, 3516])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 750
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3477, 3516]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3477, 3516]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Скорост на въртене"
--   Total rows: 3  →  canonical: 6945
--   Categories with canonical: 14
--   Products with canonical:   118
--   Non-canonical ids: ['6215', '3832']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6945
  WHERE parameter_id = ANY(ARRAY[6215, 3832])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6945
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6945,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6945
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6215, 3832]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6945
  WHERE parameter_id = ANY(ARRAY[6215, 3832]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6945
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6945, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6215, 3832])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6945
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6215, 3832]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6215, 3832]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Говорители"
--   Total rows: 3  →  canonical: 196
--   Categories with canonical: 4
--   Products with canonical:   66
--   Non-canonical ids: ['3556', '6909']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 196
  WHERE parameter_id = ANY(ARRAY[3556, 6909])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 196
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 196,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 196
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3556, 6909]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 196
  WHERE parameter_id = ANY(ARRAY[3556, 6909]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 196
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 196, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3556, 6909])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 196
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3556, 6909]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3556, 6909]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Макс. резолюция"
--   Total rows: 3  →  canonical: 6269
--   Categories with canonical: 2
--   Products with canonical:   205
--   Non-canonical ids: ['6667', '6853']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6269
  WHERE parameter_id = ANY(ARRAY[6667, 6853])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6269
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6269,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6269
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6667, 6853]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6269
  WHERE parameter_id = ANY(ARRAY[6667, 6853]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6269
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6269, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6667, 6853])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6269
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6667, 6853]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6667, 6853]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип батерия"
--   Total rows: 3  →  canonical: 6916
--   Categories with canonical: 5
--   Products with canonical:   28
--   Non-canonical ids: ['3695', '6451']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6916
  WHERE parameter_id = ANY(ARRAY[3695, 6451])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6916
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6916,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6916
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3695, 6451]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6916
  WHERE parameter_id = ANY(ARRAY[3695, 6451]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6916
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6916, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3695, 6451])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6916
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3695, 6451]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3695, 6451]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Дължина (м)"
--   Total rows: 3  →  canonical: 6358
--   Categories with canonical: 1
--   Products with canonical:   4
--   Non-canonical ids: ['6397', '6430']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6358
  WHERE parameter_id = ANY(ARRAY[6397, 6430])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6358
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6358,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6358
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6397, 6430]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6358
  WHERE parameter_id = ANY(ARRAY[6397, 6430]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6358
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6358, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6397, 6430])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6358
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6397, 6430]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6397, 6430]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Честота"
--   Total rows: 3  →  canonical: 6656
--   Categories with canonical: 1
--   Products with canonical:   56
--   Non-canonical ids: ['6509', '6584']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6656
  WHERE parameter_id = ANY(ARRAY[6509, 6584])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6656
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6656,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6656
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6509, 6584]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6656
  WHERE parameter_id = ANY(ARRAY[6509, 6584]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6656
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6656, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6509, 6584])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6656
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6509, 6584]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6509, 6584]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Импеданс"
--   Total rows: 3  →  canonical: 437
--   Categories with canonical: 20
--   Products with canonical:   95
--   Non-canonical ids: ['6586', '6860']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 437
  WHERE parameter_id = ANY(ARRAY[6586, 6860])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 437
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 437,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 437
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6586, 6860]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 437
  WHERE parameter_id = ANY(ARRAY[6586, 6860]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 437
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 437, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6586, 6860])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 437
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6586, 6860]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6586, 6860]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Възраст."
--   Total rows: 3  →  canonical: 1340
--   Categories with canonical: 3
--   Products with canonical:   4
--   Non-canonical ids: ['6774', '6903']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 1340
  WHERE parameter_id = ANY(ARRAY[6774, 6903])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 1340
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 1340,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 1340
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6774, 6903]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 1340
  WHERE parameter_id = ANY(ARRAY[6774, 6903]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 1340
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 1340, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6774, 6903])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 1340
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6774, 6903]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6774, 6903]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "RGB"
--   Total rows: 3  →  canonical: 6935
--   Categories with canonical: 1
--   Products with canonical:   20
--   Non-canonical ids: ['6931', '6941']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6935
  WHERE parameter_id = ANY(ARRAY[6931, 6941])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6935
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6935,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6935
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6931, 6941]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6935
  WHERE parameter_id = ANY(ARRAY[6931, 6941]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6935
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6935, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6931, 6941])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6935
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6931, 6941]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6931, 6941]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Софтуер"
--   Total rows: 3  →  canonical: 1554
--   Categories with canonical: 13
--   Products with canonical:   8
--   Non-canonical ids: ['7028', '7054']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 1554
  WHERE parameter_id = ANY(ARRAY[7028, 7054])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 1554
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 1554,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 1554
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[7028, 7054]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 1554
  WHERE parameter_id = ANY(ARRAY[7028, 7054]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 1554
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 1554, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[7028, 7054])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 1554
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[7028, 7054]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[7028, 7054]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Кеш памет"
--   Total rows: 2  →  canonical: 5979
--   Categories with canonical: 1
--   Products with canonical:   145
--   Non-canonical ids: ['5973']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 5979
  WHERE parameter_id = ANY(ARRAY[5973])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 5979
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 5979,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 5979
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[5973]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 5979
  WHERE parameter_id = ANY(ARRAY[5973]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 5979
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5979, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5973])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 5979
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5973]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[5973]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Серия процесори"
--   Total rows: 2  →  canonical: 5974
--   Categories with canonical: 2
--   Products with canonical:   153
--   Non-canonical ids: ['6470']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 5974
  WHERE parameter_id = ANY(ARRAY[6470])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 5974
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 5974,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 5974
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6470]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 5974
  WHERE parameter_id = ANY(ARRAY[6470]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 5974
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5974, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6470])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 5974
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6470]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6470]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Сокет"
--   Total rows: 2  →  canonical: 6125
--   Categories with canonical: 1
--   Products with canonical:   216
--   Non-canonical ids: ['5976']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6125
  WHERE parameter_id = ANY(ARRAY[5976])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6125
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6125,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6125
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[5976]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6125
  WHERE parameter_id = ANY(ARRAY[5976]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6125
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6125, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5976])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6125
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5976]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[5976]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Логически ядра"
--   Total rows: 2  →  canonical: 3772
--   Categories with canonical: 3
--   Products with canonical:   0
--   Non-canonical ids: ['5978']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3772
  WHERE parameter_id = ANY(ARRAY[5978])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3772
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3772,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3772
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[5978]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3772
  WHERE parameter_id = ANY(ARRAY[5978]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3772
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3772, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[5978])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3772
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[5978]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[5978]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Видео изходи"
--   Total rows: 2  →  canonical: 5987
--   Categories with canonical: 1
--   Products with canonical:   382
--   Non-canonical ids: ['6681']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 5987
  WHERE parameter_id = ANY(ARRAY[6681])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 5987
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 5987,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 5987
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6681]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 5987
  WHERE parameter_id = ANY(ARRAY[6681]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 5987
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 5987, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6681])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 5987
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6681]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6681]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип стойка"
--   Total rows: 2  →  canonical: 6005
--   Categories with canonical: 4
--   Products with canonical:   85
--   Non-canonical ids: ['6957']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6005
  WHERE parameter_id = ANY(ARRAY[6957])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6005
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6005,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6005
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6957]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6005
  WHERE parameter_id = ANY(ARRAY[6957]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6005
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6005, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6957])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6005
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6957]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6957]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Капацитет."
--   Total rows: 2  →  canonical: 6007
--   Categories with canonical: 2
--   Products with canonical:   6
--   Non-canonical ids: ['6113']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6007
  WHERE parameter_id = ANY(ARRAY[6113])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6007
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6007,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6007
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6113]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6007
  WHERE parameter_id = ANY(ARRAY[6113]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6007
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6007, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6113])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6007
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6113]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6113]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Динамичен контраст"
--   Total rows: 2  →  canonical: 6336
--   Categories with canonical: 7
--   Products with canonical:   20
--   Non-canonical ids: ['6013']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6336
  WHERE parameter_id = ANY(ARRAY[6013])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6336
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6336,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6336
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6013]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6336
  WHERE parameter_id = ANY(ARRAY[6013]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6336
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6336, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6013])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6336
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6013]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6013]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Сензори"
--   Total rows: 2  →  canonical: 6742
--   Categories with canonical: 13
--   Products with canonical:   0
--   Non-canonical ids: ['6028']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6742
  WHERE parameter_id = ANY(ARRAY[6028])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6742
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6742,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6742
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6028]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6742
  WHERE parameter_id = ANY(ARRAY[6028]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6742
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6742, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6028])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6742
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6028]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6028]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип консуматив"
--   Total rows: 2  →  canonical: 6031
--   Categories with canonical: 7
--   Products with canonical:   2966
--   Non-canonical ids: ['3462']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6031
  WHERE parameter_id = ANY(ARRAY[3462])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6031
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6031,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6031
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3462]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6031
  WHERE parameter_id = ANY(ARRAY[3462]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6031
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6031, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3462])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6031
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3462]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3462]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Wireless"
--   Total rows: 2  →  canonical: 3416
--   Categories with canonical: 8
--   Products with canonical:   575
--   Non-canonical ids: ['6037']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3416
  WHERE parameter_id = ANY(ARRAY[6037])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3416
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3416,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3416
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6037]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3416
  WHERE parameter_id = ANY(ARRAY[6037]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3416
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3416, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6037])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3416
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6037]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6037]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Вътрешна памет"
--   Total rows: 2  →  canonical: 6041
--   Categories with canonical: 1
--   Products with canonical:   90
--   Non-canonical ids: ['6738']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6041
  WHERE parameter_id = ANY(ARRAY[6738])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6041
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6041,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6041
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6738]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6041
  WHERE parameter_id = ANY(ARRAY[6738]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6041
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6041, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6738])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6041
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6738]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6738]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "RAM"
--   Total rows: 2  →  canonical: 6381
--   Categories with canonical: 18
--   Products with canonical:   9
--   Non-canonical ids: ['6045']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6381
  WHERE parameter_id = ANY(ARRAY[6045])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6381
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6381,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6381
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6045]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6381
  WHERE parameter_id = ANY(ARRAY[6045]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6381
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6381, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6045])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6381
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6045]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6045]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Честотен диапазон"
--   Total rows: 2  →  canonical: 6869
--   Categories with canonical: 41
--   Products with canonical:   15
--   Non-canonical ids: ['3264']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6869
  WHERE parameter_id = ANY(ARRAY[3264])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6869
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6869,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6869
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3264]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6869
  WHERE parameter_id = ANY(ARRAY[3264]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6869
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6869, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3264])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6869
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3264]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3264]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Вертикален наклон (градуси)"
--   Total rows: 2  →  canonical: 6062
--   Categories with canonical: 2
--   Products with canonical:   70
--   Non-canonical ids: ['6070']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6062
  WHERE parameter_id = ANY(ARRAY[6070])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6062
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6062,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6062
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6070]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6062
  WHERE parameter_id = ANY(ARRAY[6070]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6062
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6062, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6070])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6062
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6070]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6070]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Брой батерии"
--   Total rows: 2  →  canonical: 326
--   Categories with canonical: 6
--   Products with canonical:   176
--   Non-canonical ids: ['3348']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 326
  WHERE parameter_id = ANY(ARRAY[3348])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 326
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 326,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 326
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3348]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 326
  WHERE parameter_id = ANY(ARRAY[3348]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 326
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 326, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3348])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 326
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3348]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3348]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Време за зареждане"
--   Total rows: 2  →  canonical: 330
--   Categories with canonical: 6
--   Products with canonical:   176
--   Non-canonical ids: ['3349']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 330
  WHERE parameter_id = ANY(ARRAY[3349])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 330
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 330,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 330
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3349]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 330
  WHERE parameter_id = ANY(ARRAY[3349]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 330
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 330, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3349])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 330
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3349]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3349]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Скорост при четене"
--   Total rows: 2  →  canonical: 6720
--   Categories with canonical: 2
--   Products with canonical:   2
--   Non-canonical ids: ['6116']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6720
  WHERE parameter_id = ANY(ARRAY[6116])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6720
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6720,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6720
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6116]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6720
  WHERE parameter_id = ANY(ARRAY[6116]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6720
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6720, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6116])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6720
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6116]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6116]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Скорост при запис"
--   Total rows: 2  →  canonical: 6721
--   Categories with canonical: 2
--   Products with canonical:   2
--   Non-canonical ids: ['6117']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6721
  WHERE parameter_id = ANY(ARRAY[6117])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6721
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6721,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6721
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6117]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6721
  WHERE parameter_id = ANY(ARRAY[6117]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6721
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6721, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6117])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6721
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6117]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6117]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Вграден LAN"
--   Total rows: 2  →  canonical: 6920
--   Categories with canonical: 2
--   Products with canonical:   0
--   Non-canonical ids: ['6119']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6920
  WHERE parameter_id = ANY(ARRAY[6119])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6920
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6920,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6920
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6119]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6920
  WHERE parameter_id = ANY(ARRAY[6119]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6920
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6920, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6119])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6920
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6119]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6119]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Други характеристики"
--   Total rows: 2  →  canonical: 6284
--   Categories with canonical: 1
--   Products with canonical:   324
--   Non-canonical ids: ['6130']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6284
  WHERE parameter_id = ANY(ARRAY[6130])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6284
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6284,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6284
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6130]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6284
  WHERE parameter_id = ANY(ARRAY[6130]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6284
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6284, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6130])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6284
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6130]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6130]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип аксесоар."
--   Total rows: 2  →  canonical: 3522
--   Categories with canonical: 3
--   Products with canonical:   574
--   Non-canonical ids: ['6132']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3522
  WHERE parameter_id = ANY(ARRAY[6132])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3522
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3522,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3522
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6132]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3522
  WHERE parameter_id = ANY(ARRAY[6132]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3522
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3522, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6132])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3522
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6132]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6132]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Радиочасовник"
--   Total rows: 2  →  canonical: 653
--   Categories with canonical: 2
--   Products with canonical:   72
--   Non-canonical ids: ['3400']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 653
  WHERE parameter_id = ANY(ARRAY[3400])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 653
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 653,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 653
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3400]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 653
  WHERE parameter_id = ANY(ARRAY[3400]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 653
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 653, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3400])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 653
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3400]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3400]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Предназначение"
--   Total rows: 2  →  canonical: 233
--   Categories with canonical: 9
--   Products with canonical:   85
--   Non-canonical ids: ['3412']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 233
  WHERE parameter_id = ANY(ARRAY[3412])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 233
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 233,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 233
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3412]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 233
  WHERE parameter_id = ANY(ARRAY[3412]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 233
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 233, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3412])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 233
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3412]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3412]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Поддържани карти"
--   Total rows: 2  →  canonical: 3415
--   Categories with canonical: 3
--   Products with canonical:   10
--   Non-canonical ids: ['6259']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3415
  WHERE parameter_id = ANY(ARRAY[6259])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3415
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3415,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3415
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6259]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3415
  WHERE parameter_id = ANY(ARRAY[6259]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3415
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3415, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6259])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3415
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6259]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6259]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Аспект изображение"
--   Total rows: 2  →  canonical: 193
--   Categories with canonical: 5
--   Products with canonical:   65
--   Non-canonical ids: ['3421']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 193
  WHERE parameter_id = ANY(ARRAY[3421])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 193
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 193,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 193
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3421]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 193
  WHERE parameter_id = ANY(ARRAY[3421]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 193
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 193, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3421])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 193
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3421]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3421]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Стандарт Wi-Fi."
--   Total rows: 2  →  canonical: 6161
--   Categories with canonical: 2
--   Products with canonical:   650
--   Non-canonical ids: ['6498']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6161
  WHERE parameter_id = ANY(ARRAY[6498])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6161
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6161,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6161
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6498]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6161
  WHERE parameter_id = ANY(ARRAY[6498]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6161
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6161, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6498])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6161
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6498]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6498]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Сигурност."
--   Total rows: 2  →  canonical: 6169
--   Categories with canonical: 2
--   Products with canonical:   1592
--   Non-canonical ids: ['6503']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6169
  WHERE parameter_id = ANY(ARRAY[6503])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6169
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6169,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6169
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6503]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6169
  WHERE parameter_id = ANY(ARRAY[6503]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6169
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6169, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6503])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6169
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6503]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6503]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Антена."
--   Total rows: 2  →  canonical: 6172
--   Categories with canonical: 2
--   Products with canonical:   442
--   Non-canonical ids: ['6501']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6172
  WHERE parameter_id = ANY(ARRAY[6501])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6172
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6172,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6172
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6501]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6172
  WHERE parameter_id = ANY(ARRAY[6501]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6172
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6172, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6501])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6172
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6501]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6501]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Видео формати"
--   Total rows: 2  →  canonical: 6366
--   Categories with canonical: 2
--   Products with canonical:   220
--   Non-canonical ids: ['3452']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6366
  WHERE parameter_id = ANY(ARRAY[3452])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6366
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6366,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6366
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3452]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6366
  WHERE parameter_id = ANY(ARRAY[3452]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6366
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6366, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3452])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6366
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3452]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3452]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "MAC адреси"
--   Total rows: 2  →  canonical: 6173
--   Categories with canonical: 1
--   Products with canonical:   143
--   Non-canonical ids: ['7023']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6173
  WHERE parameter_id = ANY(ARRAY[7023])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6173
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6173,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6173
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[7023]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6173
  WHERE parameter_id = ANY(ARRAY[7023]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6173
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6173, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[7023])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6173
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[7023]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[7023]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Работна температура"
--   Total rows: 2  →  canonical: 358
--   Categories with canonical: 9
--   Products with canonical:   1
--   Non-canonical ids: ['6174']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 358
  WHERE parameter_id = ANY(ARRAY[6174])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 358
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 358,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 358
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6174]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 358
  WHERE parameter_id = ANY(ARRAY[6174]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 358
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 358, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6174])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 358
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6174]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6174]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Технология."
--   Total rows: 2  →  canonical: 3465
--   Categories with canonical: 3
--   Products with canonical:   32
--   Non-canonical ids: ['6446']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3465
  WHERE parameter_id = ANY(ARRAY[6446])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3465
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3465,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3465
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6446]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3465
  WHERE parameter_id = ANY(ARRAY[6446]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3465
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3465, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6446])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3465
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6446]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6446]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Чипсет"
--   Total rows: 2  →  canonical: 3469
--   Categories with canonical: 3
--   Products with canonical:   0
--   Non-canonical ids: ['6265']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3469
  WHERE parameter_id = ANY(ARRAY[6265])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3469
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3469,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3469
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6265]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3469
  WHERE parameter_id = ANY(ARRAY[6265]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3469
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3469, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6265])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3469
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6265]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6265]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Честота памет"
--   Total rows: 2  →  canonical: 742
--   Categories with canonical: 7
--   Products with canonical:   84
--   Non-canonical ids: ['3471']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 742
  WHERE parameter_id = ANY(ARRAY[3471])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 742
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 742,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 742
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3471]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 742
  WHERE parameter_id = ANY(ARRAY[3471]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 742
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 742, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3471])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 742
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3471]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3471]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Скорост на въртене (об/мин)"
--   Total rows: 2  →  canonical: 3473
--   Categories with canonical: 3
--   Products with canonical:   0
--   Non-canonical ids: ['6320']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3473
  WHERE parameter_id = ANY(ARRAY[6320])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3473
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3473,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3473
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6320]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3473
  WHERE parameter_id = ANY(ARRAY[6320]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3473
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3473, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6320])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3473
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6320]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6320]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Капацитет видео памет"
--   Total rows: 2  →  canonical: 743
--   Categories with canonical: 4
--   Products with canonical:   68
--   Non-canonical ids: ['3475']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 743
  WHERE parameter_id = ANY(ARRAY[3475])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 743
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 743,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 743
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3475]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 743
  WHERE parameter_id = ANY(ARRAY[3475]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 743
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 743, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3475])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 743
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3475]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3475]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип видео памет"
--   Total rows: 2  →  canonical: 744
--   Categories with canonical: 29
--   Products with canonical:   379
--   Non-canonical ids: ['3476']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 744
  WHERE parameter_id = ANY(ARRAY[3476])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 744
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 744,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 744
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3476]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 744
  WHERE parameter_id = ANY(ARRAY[3476]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 744
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 744, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3476])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 744
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3476]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3476]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Клавиатура"
--   Total rows: 2  →  canonical: 736
--   Categories with canonical: 4
--   Products with canonical:   227
--   Non-canonical ids: ['3478']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 736
  WHERE parameter_id = ANY(ARRAY[3478])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 736
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 736,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 736
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3478]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 736
  WHERE parameter_id = ANY(ARRAY[3478]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 736
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 736, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3478])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 736
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3478]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3478]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Време за четене"
--   Total rows: 2  →  canonical: 6192
--   Categories with canonical: 1
--   Products with canonical:   0
--   Non-canonical ids: ['6220']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6192
  WHERE parameter_id = ANY(ARRAY[6220])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6192
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6192,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6192
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6220]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6192
  WHERE parameter_id = ANY(ARRAY[6220]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6192
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6192, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6220])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6192
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6220]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6220]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Време за запис"
--   Total rows: 2  →  canonical: 6193
--   Categories with canonical: 1
--   Products with canonical:   0
--   Non-canonical ids: ['6221']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6193
  WHERE parameter_id = ANY(ARRAY[6221])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6193
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6193,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6193
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6221]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6193
  WHERE parameter_id = ANY(ARRAY[6221]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6193
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6193, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6221])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6193
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6221]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6221]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Скорост на трансфер SATA"
--   Total rows: 2  →  canonical: 6196
--   Categories with canonical: 1
--   Products with canonical:   34
--   Non-canonical ids: ['6218']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6196
  WHERE parameter_id = ANY(ARRAY[6218])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6196
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6196,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6196
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6218]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6196
  WHERE parameter_id = ANY(ARRAY[6218]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6196
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6196, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6218])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6196
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6218]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6218]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Захранване."
--   Total rows: 2  →  canonical: 6201
--   Categories with canonical: 2
--   Products with canonical:   368
--   Non-canonical ids: ['6760']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6201
  WHERE parameter_id = ANY(ARRAY[6760])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6201
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6201,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6201
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6760]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6201
  WHERE parameter_id = ANY(ARRAY[6760]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6201
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6201, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6760])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6201
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6760]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6760]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "RAID"
--   Total rows: 2  →  canonical: 36
--   Categories with canonical: 2
--   Products with canonical:   6
--   Non-canonical ids: ['3520']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 36
  WHERE parameter_id = ANY(ARRAY[3520])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 36
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 36,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 36
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3520]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 36
  WHERE parameter_id = ANY(ARRAY[3520]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 36
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 36, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3520])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 36
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3520]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3520]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Буфер (MB)"
--   Total rows: 2  →  canonical: 6216
--   Categories with canonical: 1
--   Products with canonical:   24
--   Non-canonical ids: ['6292']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6216
  WHERE parameter_id = ANY(ARRAY[6292])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6216
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6216,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6216
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6292]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6216
  WHERE parameter_id = ANY(ARRAY[6292]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6216
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6216, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6292])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6216
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6292]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6292]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Формат"
--   Total rows: 2  →  canonical: 6239
--   Categories with canonical: 1
--   Products with canonical:   0
--   Non-canonical ids: ['6409']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6239
  WHERE parameter_id = ANY(ARRAY[6409])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6239
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6239,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6239
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6409]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6239
  WHERE parameter_id = ANY(ARRAY[6409]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6239
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6239, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6409])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6239
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6409]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6409]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Капацитет (mAh)"
--   Total rows: 2  →  canonical: 669
--   Categories with canonical: 6
--   Products with canonical:   947
--   Non-canonical ids: ['3568']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 669
  WHERE parameter_id = ANY(ARRAY[3568])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 669
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 669,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 669
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3568]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 669
  WHERE parameter_id = ANY(ARRAY[3568]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 669
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 669, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3568])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 669
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3568]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3568]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Монтаж в шкаф"
--   Total rows: 2  →  canonical: 6377
--   Categories with canonical: 2
--   Products with canonical:   76
--   Non-canonical ids: ['6250']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6377
  WHERE parameter_id = ANY(ARRAY[6250])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6377
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6377,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6377
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6250]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6377
  WHERE parameter_id = ANY(ARRAY[6250]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6377
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6377, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6250])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6377
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6250]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6250]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Обхват"
--   Total rows: 2  →  canonical: 647
--   Categories with canonical: 2
--   Products with canonical:   24
--   Non-canonical ids: ['3593']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 647
  WHERE parameter_id = ANY(ARRAY[3593])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 647
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 647,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 647
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3593]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 647
  WHERE parameter_id = ANY(ARRAY[3593]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 647
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 647, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3593])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 647
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3593]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3593]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Кабели"
--   Total rows: 2  →  canonical: 6539
--   Categories with canonical: 1
--   Products with canonical:   208
--   Non-canonical ids: ['6264']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6539
  WHERE parameter_id = ANY(ARRAY[6264])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6539
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6539,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6539
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6264]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6539
  WHERE parameter_id = ANY(ARRAY[6264]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6539
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6539, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6264])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6539
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6264]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6264]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Брой слотове"
--   Total rows: 2  →  canonical: 3647
--   Categories with canonical: 3
--   Products with canonical:   110
--   Non-canonical ids: ['6267']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3647
  WHERE parameter_id = ANY(ARRAY[6267])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3647
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3647,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3647
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6267]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3647
  WHERE parameter_id = ANY(ARRAY[6267]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3647
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3647, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6267])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3647
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6267]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6267]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип дисплей"
--   Total rows: 2  →  canonical: 3621
--   Categories with canonical: 2
--   Products with canonical:   40
--   Non-canonical ids: ['6747']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3621
  WHERE parameter_id = ANY(ARRAY[6747])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3621
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3621,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3621
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6747]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3621
  WHERE parameter_id = ANY(ARRAY[6747]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3621
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3621, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6747])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3621
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6747]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6747]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Максимална мощност"
--   Total rows: 2  →  canonical: 1067
--   Categories with canonical: 4
--   Products with canonical:   16
--   Non-canonical ids: ['3626']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 1067
  WHERE parameter_id = ANY(ARRAY[3626])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 1067
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 1067,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 1067
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3626]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 1067
  WHERE parameter_id = ANY(ARRAY[3626]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 1067
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 1067, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3626])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 1067
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3626]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3626]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Стандарт Wi-Fi"
--   Total rows: 2  →  canonical: 6477
--   Categories with canonical: 1
--   Products with canonical:   288
--   Non-canonical ids: ['6274']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6477
  WHERE parameter_id = ANY(ARRAY[6274])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6477
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6477,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6477
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6274]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6477
  WHERE parameter_id = ANY(ARRAY[6274]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6477
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6477, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6274])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6477
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6274]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6274]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Модули"
--   Total rows: 2  →  canonical: 6278
--   Categories with canonical: 1
--   Products with canonical:   137
--   Non-canonical ids: ['6659']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6278
  WHERE parameter_id = ANY(ARRAY[6659])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6278
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6278,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6278
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6659]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6278
  WHERE parameter_id = ANY(ARRAY[6659]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6278
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6278, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6659])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6278
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6659]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6659]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Латентност"
--   Total rows: 2  →  canonical: 6280
--   Categories with canonical: 1
--   Products with canonical:   139
--   Non-canonical ids: ['6661']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6280
  WHERE parameter_id = ANY(ARRAY[6661])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6280
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6280,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6280
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6661]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6280
  WHERE parameter_id = ANY(ARRAY[6661]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6280
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6280, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6661])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6280
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6661]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6661]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Радиатор"
--   Total rows: 2  →  canonical: 6281
--   Categories with canonical: 1
--   Products with canonical:   137
--   Non-canonical ids: ['6969']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6281
  WHERE parameter_id = ANY(ARRAY[6969])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6281
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6281,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6281
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6969]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6281
  WHERE parameter_id = ANY(ARRAY[6969]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6281
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6281, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6969])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6281
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6969]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6969]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип съхранение"
--   Total rows: 2  →  canonical: 732
--   Categories with canonical: 6
--   Products with canonical:   227
--   Non-canonical ids: ['3640']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 732
  WHERE parameter_id = ANY(ARRAY[3640])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 732
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 732,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 732
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3640]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 732
  WHERE parameter_id = ANY(ARRAY[3640]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 732
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 732, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3640])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 732
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3640]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3640]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Производител процесор"
--   Total rows: 2  →  canonical: 726
--   Categories with canonical: 6
--   Products with canonical:   167
--   Non-canonical ids: ['3641']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 726
  WHERE parameter_id = ANY(ARRAY[3641])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 726
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 726,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 726
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3641]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 726
  WHERE parameter_id = ANY(ARRAY[3641]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 726
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 726, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3641])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 726
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3641]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3641]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Интерфейси и портове"
--   Total rows: 2  →  canonical: 855
--   Categories with canonical: 6
--   Products with canonical:   204
--   Non-canonical ids: ['3648']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 855
  WHERE parameter_id = ANY(ARRAY[3648])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 855
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 855,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 855
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3648]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 855
  WHERE parameter_id = ANY(ARRAY[3648]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 855
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 855, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3648])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 855
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3648]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3648]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Функции."
--   Total rows: 2  →  canonical: 704
--   Categories with canonical: 5
--   Products with canonical:   332
--   Non-canonical ids: ['3733']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 704
  WHERE parameter_id = ANY(ARRAY[3733])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 704
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 704,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 704
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3733]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 704
  WHERE parameter_id = ANY(ARRAY[3733]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 704
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 704, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3733])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 704
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3733]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3733]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Ниво на шум (dB)"
--   Total rows: 2  →  canonical: 199
--   Categories with canonical: 3
--   Products with canonical:   51
--   Non-canonical ids: ['6318']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 199
  WHERE parameter_id = ANY(ARRAY[6318])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 199
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 199,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 199
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6318]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 199
  WHERE parameter_id = ANY(ARRAY[6318]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 199
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 199, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6318])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 199
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6318]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6318]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Входни конектори"
--   Total rows: 2  →  canonical: 336
--   Categories with canonical: 6
--   Products with canonical:   154
--   Non-canonical ids: ['6329']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 336
  WHERE parameter_id = ANY(ARRAY[6329])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 336
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 336,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 336
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6329]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 336
  WHERE parameter_id = ANY(ARRAY[6329]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 336
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 336, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6329])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 336
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6329]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6329]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "VESA"
--   Total rows: 2  →  canonical: 6959
--   Categories with canonical: 1
--   Products with canonical:   550
--   Non-canonical ids: ['6334']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6959
  WHERE parameter_id = ANY(ARRAY[6334])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6959
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6959,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6959
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6334]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6959
  WHERE parameter_id = ANY(ARRAY[6334]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6959
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6959, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6334])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6959
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6334]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6334]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Съдържание пакет"
--   Total rows: 2  →  canonical: 1136
--   Categories with canonical: 5
--   Products with canonical:   8
--   Non-canonical ids: ['3756']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 1136
  WHERE parameter_id = ANY(ARRAY[3756])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 1136
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 1136,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 1136
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3756]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 1136
  WHERE parameter_id = ANY(ARRAY[3756]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 1136
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 1136, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3756])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 1136
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3756]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3756]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип кабел"
--   Total rows: 2  →  canonical: 6359
--   Categories with canonical: 1
--   Products with canonical:   2
--   Non-canonical ids: ['6828']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6359
  WHERE parameter_id = ANY(ARRAY[6828])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6359
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6359,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6359
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6828]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6359
  WHERE parameter_id = ANY(ARRAY[6828]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6359
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6359, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6828])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6359
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6828]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6828]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип рамка"
--   Total rows: 2  →  canonical: 3760
--   Categories with canonical: 1
--   Products with canonical:   52
--   Non-canonical ids: ['6552']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3760
  WHERE parameter_id = ANY(ARRAY[6552])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3760
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3760,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3760
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6552]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3760
  WHERE parameter_id = ANY(ARRAY[6552]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3760
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3760, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6552])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3760
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6552]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6552]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Качество"
--   Total rows: 2  →  canonical: 6416
--   Categories with canonical: 3
--   Products with canonical:   2
--   Non-canonical ids: ['6361']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6416
  WHERE parameter_id = ANY(ARRAY[6361])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6416
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6416,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6416
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6361]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6416
  WHERE parameter_id = ANY(ARRAY[6361]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6416
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6416, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6361])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6416
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6361]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6361]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Мрежова връзка"
--   Total rows: 2  →  canonical: 236
--   Categories with canonical: 3
--   Products with canonical:   33
--   Non-canonical ids: ['6363']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 236
  WHERE parameter_id = ANY(ARRAY[6363])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 236
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 236,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 236
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6363]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 236
  WHERE parameter_id = ANY(ARRAY[6363]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 236
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 236, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6363])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 236
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6363]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6363]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Чувствителност (dB)"
--   Total rows: 2  →  canonical: 6434
--   Categories with canonical: 1
--   Products with canonical:   82
--   Non-canonical ids: ['6375']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6434
  WHERE parameter_id = ANY(ARRAY[6375])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6434
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6434,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6434
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6375]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6434
  WHERE parameter_id = ANY(ARRAY[6375]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6434
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6434, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6375])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6434
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6375]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6375]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип."
--   Total rows: 2  →  canonical: 6649
--   Categories with canonical: 3
--   Products with canonical:   6
--   Non-canonical ids: ['3785']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6649
  WHERE parameter_id = ANY(ARRAY[3785])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6649
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6649,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6649
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3785]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6649
  WHERE parameter_id = ANY(ARRAY[3785]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6649
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6649, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3785])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6649
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3785]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3785]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип охлаждане"
--   Total rows: 2  →  canonical: 6629
--   Categories with canonical: 1
--   Products with canonical:   167
--   Non-canonical ids: ['3831']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6629
  WHERE parameter_id = ANY(ARRAY[3831])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6629
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6629,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6629
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3831]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6629
  WHERE parameter_id = ANY(ARRAY[3831]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6629
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6629, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3831])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6629
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3831]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3831]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Модел видео карта"
--   Total rows: 2  →  canonical: 872
--   Categories with canonical: 5
--   Products with canonical:   70
--   Non-canonical ids: ['3836']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 872
  WHERE parameter_id = ANY(ARRAY[3836])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 872
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 872,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 872
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3836]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 872
  WHERE parameter_id = ANY(ARRAY[3836]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 872
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 872, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3836])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 872
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3836]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3836]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип лиценз"
--   Total rows: 2  →  canonical: 284
--   Categories with canonical: 9
--   Products with canonical:   72
--   Non-canonical ids: ['3837']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 284
  WHERE parameter_id = ANY(ARRAY[3837])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 284
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 284,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 284
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3837]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 284
  WHERE parameter_id = ANY(ARRAY[3837]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 284
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 284, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3837])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 284
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3837]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3837]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Bluetooth версия"
--   Total rows: 2  →  canonical: 6639
--   Categories with canonical: 2
--   Products with canonical:   24
--   Non-canonical ids: ['6441']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6639
  WHERE parameter_id = ANY(ARRAY[6441])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6639
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6639,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6639
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6441]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6639
  WHERE parameter_id = ANY(ARRAY[6441]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6639
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6639, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6441])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6639
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6441]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6441]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Максимална памет"
--   Total rows: 2  →  canonical: 741
--   Categories with canonical: 5
--   Products with canonical:   80
--   Non-canonical ids: ['6456']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 741
  WHERE parameter_id = ANY(ARRAY[6456])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 741
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 741,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 741
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6456]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 741
  WHERE parameter_id = ANY(ARRAY[6456]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 741
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 741, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6456])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 741
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6456]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6456]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Ефективност"
--   Total rows: 2  →  canonical: 3909
--   Categories with canonical: 1
--   Products with canonical:   574
--   Non-canonical ids: ['1010']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3909
  WHERE parameter_id = ANY(ARRAY[1010])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3909
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3909,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3909
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[1010]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3909
  WHERE parameter_id = ANY(ARRAY[1010]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3909
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3909, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[1010])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3909
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[1010]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[1010]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Скорост на обмен на данни"
--   Total rows: 2  →  canonical: 6496
--   Categories with canonical: 1
--   Products with canonical:   0
--   Non-canonical ids: ['6842']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6496
  WHERE parameter_id = ANY(ARRAY[6842])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6496
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6496,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6496
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6842]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6496
  WHERE parameter_id = ANY(ARRAY[6842]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6496
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6496, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6842])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6496
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6842]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6842]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Поддържана резолюция"
--   Total rows: 2  →  canonical: 6923
--   Categories with canonical: 2
--   Products with canonical:   0
--   Non-canonical ids: ['3976']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6923
  WHERE parameter_id = ANY(ARRAY[3976])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6923
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6923,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6923
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3976]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6923
  WHERE parameter_id = ANY(ARRAY[3976]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6923
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6923, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3976])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6923
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3976]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3976]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Интерфейс."
--   Total rows: 2  →  canonical: 597
--   Categories with canonical: 5
--   Products with canonical:   298
--   Non-canonical ids: ['6502']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 597
  WHERE parameter_id = ANY(ARRAY[6502])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 597
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 597,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 597
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6502]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 597
  WHERE parameter_id = ANY(ARRAY[6502]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 597
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 597, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6502])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 597
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6502]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6502]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Съвместимо устройство"
--   Total rows: 2  →  canonical: 1168
--   Categories with canonical: 4
--   Products with canonical:   4
--   Non-canonical ids: ['3987']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 1168
  WHERE parameter_id = ANY(ARRAY[3987])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 1168
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 1168,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 1168
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3987]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 1168
  WHERE parameter_id = ANY(ARRAY[3987]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 1168
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 1168, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3987])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 1168
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3987]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3987]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Източник на светлина"
--   Total rows: 2  →  canonical: 1741
--   Categories with canonical: 4
--   Products with canonical:   59
--   Non-canonical ids: ['3988']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 1741
  WHERE parameter_id = ANY(ARRAY[3988])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 1741
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 1741,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 1741
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3988]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 1741
  WHERE parameter_id = ANY(ARRAY[3988]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 1741
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 1741, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3988])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 1741
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3988]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3988]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Поддържани файлови формати"
--   Total rows: 2  →  canonical: 3989
--   Categories with canonical: 3
--   Products with canonical:   152
--   Non-canonical ids: ['7055']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3989
  WHERE parameter_id = ANY(ARRAY[7055])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3989
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3989,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3989
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[7055]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3989
  WHERE parameter_id = ANY(ARRAY[7055]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3989
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3989, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[7055])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3989
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[7055]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[7055]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Софтуер за сканиране"
--   Total rows: 2  →  canonical: 841
--   Categories with canonical: 4
--   Products with canonical:   0
--   Non-canonical ids: ['3990']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 841
  WHERE parameter_id = ANY(ARRAY[3990])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 841
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 841,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 841
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[3990]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 841
  WHERE parameter_id = ANY(ARRAY[3990]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 841
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 841, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[3990])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 841
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[3990]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[3990]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Дебелина"
--   Total rows: 2  →  canonical: 6968
--   Categories with canonical: 1
--   Products with canonical:   32
--   Non-canonical ids: ['6518']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6968
  WHERE parameter_id = ANY(ARRAY[6518])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6968
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6968,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6968
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6518]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6968
  WHERE parameter_id = ANY(ARRAY[6518]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6968
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6968, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6518])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6968
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6518]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6518]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Вид дисплей"
--   Total rows: 2  →  canonical: 729
--   Categories with canonical: 6
--   Products with canonical:   84
--   Non-canonical ids: ['6523']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 729
  WHERE parameter_id = ANY(ARRAY[6523])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 729
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 729,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 729
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6523]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 729
  WHERE parameter_id = ANY(ARRAY[6523]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 729
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 729, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6523])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 729
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6523]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6523]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Съвместима марка"
--   Total rows: 2  →  canonical: 6900
--   Categories with canonical: 3
--   Products with canonical:   215
--   Non-canonical ids: ['6528']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6900
  WHERE parameter_id = ANY(ARRAY[6528])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6900
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6900,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6900
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6528]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6900
  WHERE parameter_id = ANY(ARRAY[6528]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6900
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6900, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6528])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6900
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6528]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6528]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Съвместим модел"
--   Total rows: 2  →  canonical: 6901
--   Categories with canonical: 3
--   Products with canonical:   312
--   Non-canonical ids: ['6529']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6901
  WHERE parameter_id = ANY(ARRAY[6529])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6901
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6901,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6901
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6529]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6901
  WHERE parameter_id = ANY(ARRAY[6529]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6901
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6901, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6529])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6901
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6529]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6529]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Стил"
--   Total rows: 2  →  canonical: 6530
--   Categories with canonical: 2
--   Products with canonical:   0
--   Non-canonical ids: ['6789']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6530
  WHERE parameter_id = ANY(ARRAY[6789])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6530
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6530,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6530
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6789]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6530
  WHERE parameter_id = ANY(ARRAY[6789]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6530
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6530, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6789])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6530
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6789]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6789]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Водоустойчив"
--   Total rows: 2  →  canonical: 215
--   Categories with canonical: 3
--   Products with canonical:   20
--   Non-canonical ids: ['6534']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 215
  WHERE parameter_id = ANY(ARRAY[6534])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 215
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 215,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 215
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6534]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 215
  WHERE parameter_id = ANY(ARRAY[6534]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 215
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 215, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6534])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 215
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6534]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6534]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Брой портове"
--   Total rows: 2  →  canonical: 6955
--   Categories with canonical: 21
--   Products with canonical:   0
--   Non-canonical ids: ['6537']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6955
  WHERE parameter_id = ANY(ARRAY[6537])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6955
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6955,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6955
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6537]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6955
  WHERE parameter_id = ANY(ARRAY[6537]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6955
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6955, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6537])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6955
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6537]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6537]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "VPN"
--   Total rows: 2  →  canonical: 6764
--   Categories with canonical: 1
--   Products with canonical:   728
--   Non-canonical ids: ['6544']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6764
  WHERE parameter_id = ANY(ARRAY[6544])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6764
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6764,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6764
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6544]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6764
  WHERE parameter_id = ANY(ARRAY[6544]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6764
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6764, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6544])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6764
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6544]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6544]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Брой снимки"
--   Total rows: 2  →  canonical: 6795
--   Categories with canonical: 1
--   Products with canonical:   2
--   Non-canonical ids: ['6554']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6795
  WHERE parameter_id = ANY(ARRAY[6554])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6795
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6795,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6795
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6554]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6795
  WHERE parameter_id = ANY(ARRAY[6554]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6795
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6795, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6554])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6795
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6554]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6554]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Wi-Fi"
--   Total rows: 2  →  canonical: 6560
--   Categories with canonical: 1
--   Products with canonical:   0
--   Non-canonical ids: ['6744']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6560
  WHERE parameter_id = ANY(ARRAY[6744])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6560
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6560,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6560
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6744]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6560
  WHERE parameter_id = ANY(ARRAY[6744]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6560
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6560, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6744])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6560
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6744]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6744]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Конектор 1."
--   Total rows: 2  →  canonical: 6568
--   Categories with canonical: 4
--   Products with canonical:   8
--   Non-canonical ids: ['6856']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6568
  WHERE parameter_id = ANY(ARRAY[6856])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6568
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6568,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6568
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6856]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6568
  WHERE parameter_id = ANY(ARRAY[6856]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6568
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6568, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6856])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6568
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6856]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6856]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Конектор 2."
--   Total rows: 2  →  canonical: 6569
--   Categories with canonical: 4
--   Products with canonical:   8
--   Non-canonical ids: ['6857']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6569
  WHERE parameter_id = ANY(ARRAY[6857])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6569
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6569,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6569
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6857]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6569
  WHERE parameter_id = ANY(ARRAY[6857]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6569
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6569, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6857])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6569
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6857]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6857]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Поддръжка на RAID"
--   Total rows: 2  →  canonical: 6571
--   Categories with canonical: 1
--   Products with canonical:   209
--   Non-canonical ids: ['6679']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6571
  WHERE parameter_id = ANY(ARRAY[6679])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6571
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6571,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6571
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6679]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6571
  WHERE parameter_id = ANY(ARRAY[6679]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6571
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6571, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6679])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6571
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6679]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6679]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "USB Портове"
--   Total rows: 2  →  canonical: 6580
--   Categories with canonical: 2
--   Products with canonical:   1037
--   Non-canonical ids: ['6922']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6580
  WHERE parameter_id = ANY(ARRAY[6922])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6580
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6580,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6580
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6922]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6580
  WHERE parameter_id = ANY(ARRAY[6922]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6580
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6580, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6922])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6580
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6922]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6922]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "HDD"
--   Total rows: 2  →  canonical: 123
--   Categories with canonical: 9
--   Products with canonical:   170
--   Non-canonical ids: ['6647']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 123
  WHERE parameter_id = ANY(ARRAY[6647])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 123
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 123,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 123
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6647]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 123
  WHERE parameter_id = ANY(ARRAY[6647]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 123
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 123, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6647])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 123
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6647]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6647]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Монтиране"
--   Total rows: 2  →  canonical: 6631
--   Categories with canonical: 3
--   Products with canonical:   36
--   Non-canonical ids: ['6618']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6631
  WHERE parameter_id = ANY(ARRAY[6618])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6631
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6631,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6631
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6618]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6631
  WHERE parameter_id = ANY(ARRAY[6618]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6631
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6631, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6618])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6631
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6618]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6618]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Вид аксесоар."
--   Total rows: 2  →  canonical: 984
--   Categories with canonical: 4
--   Products with canonical:   108
--   Non-canonical ids: ['6626']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 984
  WHERE parameter_id = ANY(ARRAY[6626])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 984
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 984,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 984
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6626]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 984
  WHERE parameter_id = ANY(ARRAY[6626]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 984
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 984, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6626])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 984
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6626]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6626]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Оптично устройство DVD+/-RW"
--   Total rows: 2  →  canonical: 868
--   Categories with canonical: 5
--   Products with canonical:   60
--   Non-canonical ids: ['6648']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 868
  WHERE parameter_id = ANY(ARRAY[6648])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 868
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 868,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 868
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6648]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 868
  WHERE parameter_id = ANY(ARRAY[6648]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 868
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 868, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6648])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 868
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6648]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6648]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Тип ламиниране"
--   Total rows: 2  →  canonical: 6674
--   Categories with canonical: 1
--   Products with canonical:   0
--   Non-canonical ids: ['6827']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6674
  WHERE parameter_id = ANY(ARRAY[6827])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6674
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6674,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6674
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6827]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6674
  WHERE parameter_id = ANY(ARRAY[6827]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6674
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6674, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6827])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6674
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6827]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6827]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Разширителни слотове"
--   Total rows: 2  →  canonical: 132
--   Categories with canonical: 3
--   Products with canonical:   871
--   Non-canonical ids: ['6680']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 132
  WHERE parameter_id = ANY(ARRAY[6680])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 132
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 132,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 132
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6680]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 132
  WHERE parameter_id = ANY(ARRAY[6680]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 132
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 132, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6680])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 132
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6680]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6680]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Bluetooth"
--   Total rows: 2  →  canonical: 751
--   Categories with canonical: 55
--   Products with canonical:   2182
--   Non-canonical ids: ['6743']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 751
  WHERE parameter_id = ANY(ARRAY[6743])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 751
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 751,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 751
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6743]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 751
  WHERE parameter_id = ANY(ARRAY[6743]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 751
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 751, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6743])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 751
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6743]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6743]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Капацитет съхранение"
--   Total rows: 2  →  canonical: 70
--   Categories with canonical: 4
--   Products with canonical:   8
--   Non-canonical ids: ['6770']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 70
  WHERE parameter_id = ANY(ARRAY[6770])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 70
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 70,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 70
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6770]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 70
  WHERE parameter_id = ANY(ARRAY[6770]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 70
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 70, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6770])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 70
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6770]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6770]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Място за съхранение"
--   Total rows: 2  →  canonical: 1580
--   Categories with canonical: 3
--   Products with canonical:   10
--   Non-canonical ids: ['6944']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 1580
  WHERE parameter_id = ANY(ARRAY[6944])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 1580
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 1580,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 1580
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6944]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 1580
  WHERE parameter_id = ANY(ARRAY[6944]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 1580
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 1580, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6944])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 1580
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6944]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6944]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Оптично устройство"
--   Total rows: 2  →  canonical: 847
--   Categories with canonical: 8
--   Products with canonical:   137
--   Non-canonical ids: ['6782']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 847
  WHERE parameter_id = ANY(ARRAY[6782])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 847
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 847,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 847
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6782]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 847
  WHERE parameter_id = ANY(ARRAY[6782]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 847
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 847, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6782])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 847
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6782]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6782]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Енергиен клас"
--   Total rows: 2  →  canonical: 6814
--   Categories with canonical: 2
--   Products with canonical:   755
--   Non-canonical ids: ['6906']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6814
  WHERE parameter_id = ANY(ARRAY[6906])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6814
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6814,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6814
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6906]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6814
  WHERE parameter_id = ANY(ARRAY[6906]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6814
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6814, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6906])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6814
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6906]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6906]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Екраниран"
--   Total rows: 2  →  canonical: 6841
--   Categories with canonical: 1
--   Products with canonical:   0
--   Non-canonical ids: ['6859']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6841
  WHERE parameter_id = ANY(ARRAY[6859])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6841
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6841,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6841
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6859]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6841
  WHERE parameter_id = ANY(ARRAY[6859]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6841
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6841, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6859])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6841
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6859]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6859]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Дължина."
--   Total rows: 2  →  canonical: 6881
--   Categories with canonical: 2
--   Products with canonical:   0
--   Non-canonical ids: ['6858']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6881
  WHERE parameter_id = ANY(ARRAY[6858])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6881
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6881,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6881
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6858]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6881
  WHERE parameter_id = ANY(ARRAY[6858]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6881
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6881, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6858])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6881
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6858]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6858]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Конектор"
--   Total rows: 2  →  canonical: 356
--   Categories with canonical: 6
--   Products with canonical:   1206
--   Non-canonical ids: ['6952']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 356
  WHERE parameter_id = ANY(ARRAY[6952])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 356
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 356,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 356
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[6952]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 356
  WHERE parameter_id = ANY(ARRAY[6952]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 356
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 356, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[6952])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 356
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[6952]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[6952]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Форма на изходното напрежение"
--   Total rows: 2  →  canonical: 349
--   Categories with canonical: 6
--   Products with canonical:   182
--   Non-canonical ids: ['7005']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 349
  WHERE parameter_id = ANY(ARRAY[7005])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 349
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 349,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 349
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[7005]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 349
  WHERE parameter_id = ANY(ARRAY[7005]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 349
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 349, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[7005])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 349
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[7005]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[7005]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Допълнителни портове"
--   Total rows: 2  →  canonical: 6965
--   Categories with canonical: 2
--   Products with canonical:   339
--   Non-canonical ids: ['7006']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 6965
  WHERE parameter_id = ANY(ARRAY[7006])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 6965
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 6965,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 6965
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[7006]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 6965
  WHERE parameter_id = ANY(ARRAY[7006]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 6965
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 6965, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[7006])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 6965
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[7006]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[7006]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Защита"
--   Total rows: 2  →  canonical: 1455
--   Categories with canonical: 3
--   Products with canonical:   460
--   Non-canonical ids: ['7003']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 1455
  WHERE parameter_id = ANY(ARRAY[7003])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 1455
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 1455,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 1455
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[7003]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 1455
  WHERE parameter_id = ANY(ARRAY[7003]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 1455
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 1455, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[7003])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 1455
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[7003]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[7003]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Модел процесор"
--   Total rows: 2  →  canonical: 3466
--   Categories with canonical: 8
--   Products with canonical:   1752
--   Non-canonical ids: ['758']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 3466
  WHERE parameter_id = ANY(ARRAY[758])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 3466
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 3466,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 3466
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[758]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 3466
  WHERE parameter_id = ANY(ARRAY[758]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 3466
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 3466, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[758])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 3466
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[758]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[758]);

END $$;

-- ─────────────────────────────────────────────────────────────────────
-- GROUP: "Механизъм"
--   Total rows: 2  →  canonical: 1380
--   Categories with canonical: 2
--   Products with canonical:   64
--   Non-canonical ids: ['7042']
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN

  -- 1. Re-parent options not present in canonical (add to canonical param)
  UPDATE parameter_options
  SET parameter_id = 1380
  WHERE parameter_id = ANY(ARRAY[7042])
    AND NOT EXISTS (
      SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = 1380
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg))
    );

  -- 2. Redirect product_parameters to canonical option (where match by name exists)
  UPDATE product_parameters pp
  SET parameter_id        = 1380,
      parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c
    ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
   AND c.parameter_id = 1380
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY(ARRAY[7042]);

  -- 3. Remaining product_parameters (options re-parented in step 1)
  UPDATE product_parameters
  SET parameter_id = 1380
  WHERE parameter_id = ANY(ARRAY[7042]);

  -- 4. Remove duplicate product_parameters for canonical param
  DELETE FROM product_parameters
  WHERE id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id
      ) AS rn
      FROM product_parameters WHERE parameter_id = 1380
    ) t WHERE rn > 1
  );

  -- 5. Add canonical to categories not yet linked
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, 1380, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY(ARRAY[7042])
    AND NOT EXISTS (
      SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = 1380
    )
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Remove non-canonical from category_parameters
  DELETE FROM category_parameters WHERE parameter_id = ANY(ARRAY[7042]);

  -- 7. Delete non-canonical parameters (CASCADE removes their options)
  DELETE FROM parameters WHERE id = ANY(ARRAY[7042]);

END $$;

-- =============================================================================
-- VERIFICATION (run BEFORE COMMIT):
-- Expected parameter rows eliminated: 810
-- SELECT COUNT(*) FROM parameters WHERE platform = 'VALI';
-- SELECT COUNT(*) FROM parameter_options po JOIN parameters p ON p.id = po.parameter_id WHERE p.platform = 'VALI';
-- SELECT COUNT(*) FROM product_parameters pp JOIN parameters p ON p.id = pp.parameter_id WHERE p.platform = 'VALI';
--
-- After verification run: COMMIT;
-- On issue run: ROLLBACK;
-- =============================================================================

-- COMMIT;