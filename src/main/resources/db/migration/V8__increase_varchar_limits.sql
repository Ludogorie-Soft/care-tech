-- Migration: Use TEXT for parameter options to handle very long Asbis values
-- Created: 2025-10-20
-- Reason: Asbis API returns VERY long parameter values (up to 1640+ chars observed)

-- ==================================================
-- CRITICAL: PARAMETER_OPTIONS TABLE
-- ==================================================
-- ⭐ THIS IS THE MAIN ISSUE - parameter option values can be VERY long
-- Example found: 1640 chars! "Индикатор за изтощена батерия <br/>Telescopic tube <br/>..."
-- Solution: Use TEXT instead of VARCHAR to allow unlimited length

ALTER TABLE parameter_options
    ALTER COLUMN name_bg TYPE TEXT,
    ALTER COLUMN name_en TYPE TEXT;

COMMENT ON COLUMN parameter_options.name_bg IS 'Parameter option value in Bulgarian (TEXT for unlimited length)';
COMMENT ON COLUMN parameter_options.name_en IS 'Parameter option value in English (TEXT for unlimited length)';

-- ==================================================
-- PARAMETERS TABLE
-- ==================================================
-- Increase parameter names to allow longer Asbis/Tekra keys
-- 500 chars should be sufficient for parameter names

ALTER TABLE parameters
    ALTER COLUMN name_bg TYPE VARCHAR(500),
    ALTER COLUMN name_en TYPE VARCHAR(500),
    ALTER COLUMN asbis_key TYPE VARCHAR(500),
    ALTER COLUMN tekra_key TYPE VARCHAR(500);

COMMENT ON COLUMN parameters.name_bg IS 'Parameter name in Bulgarian (increased to 500 chars)';
COMMENT ON COLUMN parameters.name_en IS 'Parameter name in English (increased to 500 chars)';
COMMENT ON COLUMN parameters.asbis_key IS 'Asbis parameter key (increased to 500 chars)';
COMMENT ON COLUMN parameters.tekra_key IS 'Tekra parameter key (increased to 500 chars)';

-- ==================================================
-- PRODUCTS TABLE (OPTIONAL)
-- ==================================================
-- Product model can sometimes be long
-- name_bg and name_en are already TEXT - no change needed
-- reference_number should stay at 255

ALTER TABLE products
    ALTER COLUMN model TYPE VARCHAR(500);

COMMENT ON COLUMN products.model IS 'Product model (increased to 500 chars)';

-- NOTE: Products.name_bg and Products.name_en are already TEXT (from entity: columnDefinition = "TEXT")
-- No migration needed for product names!

-- ==================================================
-- CATEGORIES TABLE
-- ==================================================
-- Asbis category IDs can be compound keys like "Level1|Level2|Level3"
-- category_path is already 500 chars (from entity: length = 500)

ALTER TABLE categories
    ALTER COLUMN asbis_id TYPE VARCHAR(500),
    ALTER COLUMN tekra_id TYPE VARCHAR(500),
    ALTER COLUMN tekra_slug TYPE VARCHAR(500);

COMMENT ON COLUMN categories.asbis_id IS 'Asbis category ID (can be compound key, increased to 500 chars)';
COMMENT ON COLUMN categories.tekra_id IS 'Tekra category ID (increased to 500 chars)';
COMMENT ON COLUMN categories.tekra_slug IS 'Tekra category slug (increased to 500 chars)';

-- NOTE: Categories.categoryPath is already 500 chars - no change needed!
-- NOTE: Categories.slug is already 200 chars - sufficient for slugs

-- ==================================================
-- MANUFACTURERS TABLE (KEEP AS IS)
-- ==================================================
-- Manufacturer names are typically short (< 255 chars)
-- No changes needed - VARCHAR(255) is sufficient

-- ==================================================
-- VERIFICATION QUERY
-- ==================================================
-- Run this to verify all changes were applied correctly

SELECT
    table_name,
    column_name,
    data_type,
    character_maximum_length,
    CASE
        WHEN data_type = 'text' THEN '✓ TEXT (unlimited)'
        WHEN character_maximum_length = 500 THEN '✓ INCREASED TO 500'
        WHEN character_maximum_length = 255 THEN '⚠ Still 255'
        ELSE CAST(character_maximum_length AS TEXT)
    END as status
FROM information_schema.columns
WHERE table_name IN ('parameters', 'parameter_options', 'products', 'categories')
    AND column_name IN ('name_bg', 'name_en', 'asbis_key', 'tekra_key', 'model', 'asbis_id', 'tekra_id', 'tekra_slug')
ORDER BY
    CASE table_name
        WHEN 'parameter_options' THEN 1
        WHEN 'parameters' THEN 2
        WHEN 'products' THEN 3
        WHEN 'categories' THEN 4
    END,
    column_name;

-- Expected results after migration:
-- parameter_options.name_bg → TEXT ✓ (unlimited)
-- parameter_options.name_en → TEXT ✓ (unlimited)
-- parameters.name_bg → VARCHAR(500) ✓
-- parameters.name_en → VARCHAR(500) ✓
-- parameters.asbis_key → VARCHAR(500) ✓
-- parameters.tekra_key → VARCHAR(500) ✓
-- products.model → VARCHAR(500) ✓
-- categories.asbis_id → VARCHAR(500) ✓
-- categories.tekra_id → VARCHAR(500) ✓
-- categories.tekra_slug → VARCHAR(500) ✓

-- ==================================================
-- PERFORMANCE NOTE
-- ==================================================
-- Converting VARCHAR to TEXT is very fast on PostgreSQL
-- It only updates the metadata, not the actual data
-- Expected execution time: < 1 second per table

-- TEXT vs VARCHAR in PostgreSQL:
-- - TEXT has NO performance penalty vs VARCHAR
-- - TEXT is actually preferred for variable-length strings
-- - No length limit, which is perfect for unpredictable API data

-- ==================================================
-- ROLLBACK SCRIPT (USE WITH CAUTION!)
-- ==================================================
/*
-- ⚠️ WARNING: Rolling back will FAIL if data longer than 255 chars exists!
-- Only use if you need to revert and have no long values in production

ALTER TABLE parameter_options
    ALTER COLUMN name_bg TYPE VARCHAR(255),
    ALTER COLUMN name_en TYPE VARCHAR(255);

ALTER TABLE parameters
    ALTER COLUMN name_bg TYPE VARCHAR(255),
    ALTER COLUMN name_en TYPE VARCHAR(255),
    ALTER COLUMN asbis_key TYPE VARCHAR(255),
    ALTER COLUMN tekra_key TYPE VARCHAR(255);

ALTER TABLE products
    ALTER COLUMN model TYPE VARCHAR(255);

ALTER TABLE categories
    ALTER COLUMN asbis_id TYPE VARCHAR(255),
    ALTER COLUMN tekra_id TYPE VARCHAR(255),
    ALTER COLUMN tekra_slug TYPE VARCHAR(255);
*/

-- ==================================================
-- PARAMETERS TABLE
-- ==================================================
-- Increase parameter names to allow longer Asbis/Tekra keys
-- Current: VARCHAR(255) → New: VARCHAR(500)

ALTER TABLE parameters
    ALTER COLUMN name_bg TYPE VARCHAR(500),
    ALTER COLUMN name_en TYPE VARCHAR(500),
    ALTER COLUMN asbis_key TYPE VARCHAR(500),
    ALTER COLUMN tekra_key TYPE VARCHAR(500);

COMMENT ON COLUMN parameters.name_bg IS 'Parameter name in Bulgarian (increased to 500 chars)';
COMMENT ON COLUMN parameters.name_en IS 'Parameter name in English (increased to 500 chars)';
COMMENT ON COLUMN parameters.asbis_key IS 'Asbis parameter key (increased to 500 chars)';
COMMENT ON COLUMN parameters.tekra_key IS 'Tekra parameter key (increased to 500 chars)';

-- ==================================================
-- PRODUCTS TABLE (OPTIONAL)
-- ==================================================
-- Product model can sometimes be long
-- name_bg and name_en are already TEXT - no change needed
-- reference_number should stay at 255
-- Current: VARCHAR(255) → New: VARCHAR(500)

ALTER TABLE products
    ALTER COLUMN model TYPE VARCHAR(500);

COMMENT ON COLUMN products.model IS 'Product model (increased to 500 chars)';

-- NOTE: Products.name_bg and Products.name_en are already TEXT (from entity: columnDefinition = "TEXT")
-- No migration needed for product names!

-- ==================================================
-- CATEGORIES TABLE
-- ==================================================
-- Asbis category IDs can be compound keys like "Level1|Level2|Level3"
-- category_path is already 500 chars (from entity: length = 500)
-- Current: VARCHAR(255) → New: VARCHAR(500)

ALTER TABLE categories
    ALTER COLUMN asbis_id TYPE VARCHAR(500),
    ALTER COLUMN tekra_id TYPE VARCHAR(500),
    ALTER COLUMN tekra_slug TYPE VARCHAR(500);

COMMENT ON COLUMN categories.asbis_id IS 'Asbis category ID (can be compound key, increased to 500 chars)';
COMMENT ON COLUMN categories.tekra_id IS 'Tekra category ID (increased to 500 chars)';
COMMENT ON COLUMN categories.tekra_slug IS 'Tekra category slug (increased to 500 chars)';

-- NOTE: Categories.categoryPath is already 500 chars - no change needed!
-- NOTE: Categories.slug is already 200 chars - sufficient for slugs

-- ==================================================
-- MANUFACTURERS TABLE (KEEP AS IS)
-- ==================================================
-- Manufacturer names are typically short (< 255 chars)
-- No changes needed - VARCHAR(255) is sufficient

-- ==================================================
-- VERIFICATION QUERY
-- ==================================================
-- Run this to verify all changes were applied correctly

SELECT
    table_name,
    column_name,
    data_type,
    character_maximum_length,
    CASE
        WHEN character_maximum_length = 1000 THEN '✓ INCREASED TO 1000'
        WHEN character_maximum_length = 500 THEN '✓ INCREASED TO 500'
        WHEN character_maximum_length = 255 THEN '⚠ Still 255'
        ELSE CAST(character_maximum_length AS TEXT)
    END as status
FROM information_schema.columns
WHERE table_name IN ('parameters', 'parameter_options', 'products', 'categories')
    AND column_name IN ('name_bg', 'name_en', 'asbis_key', 'tekra_key', 'model', 'asbis_id', 'tekra_id', 'tekra_slug')
ORDER BY
    CASE table_name
        WHEN 'parameter_options' THEN 1
        WHEN 'parameters' THEN 2
        WHEN 'products' THEN 3
        WHEN 'categories' THEN 4
    END,
    column_name;

-- Expected results after migration:
-- parameter_options.name_bg → VARCHAR(1000) ✓
-- parameter_options.name_en → VARCHAR(1000) ✓
-- parameters.name_bg → VARCHAR(500) ✓
-- parameters.name_en → VARCHAR(500) ✓
-- parameters.asbis_key → VARCHAR(500) ✓
-- parameters.tekra_key → VARCHAR(500) ✓
-- products.model → VARCHAR(500) ✓
-- categories.asbis_id → VARCHAR(500) ✓
-- categories.tekra_id → VARCHAR(500) ✓
-- categories.tekra_slug → VARCHAR(500) ✓

-- ==================================================
-- PERFORMANCE NOTE
-- ==================================================
-- These ALTER COLUMN operations are typically fast on PostgreSQL
-- They only update the metadata, not the actual data
-- Expected execution time: < 1 second per table

-- ==================================================
-- ROLLBACK SCRIPT (USE WITH CAUTION!)
-- ==================================================
/*
-- ⚠️ WARNING: Rolling back will TRUNCATE data longer than 255 chars!
-- Only use if you need to revert and have no long values in production

ALTER TABLE parameter_options
    ALTER COLUMN name_bg TYPE VARCHAR(255),
    ALTER COLUMN name_en TYPE VARCHAR(255);

ALTER TABLE parameters
    ALTER COLUMN name_bg TYPE VARCHAR(255),
    ALTER COLUMN name_en TYPE VARCHAR(255),
    ALTER COLUMN asbis_key TYPE VARCHAR(255),
    ALTER COLUMN tekra_key TYPE VARCHAR(255);

ALTER TABLE products
    ALTER COLUMN model TYPE VARCHAR(255);

ALTER TABLE categories
    ALTER COLUMN asbis_id TYPE VARCHAR(255),
    ALTER COLUMN tekra_id TYPE VARCHAR(255),
    ALTER COLUMN tekra_slug TYPE VARCHAR(255);
*/

-- ==================================================
-- POST-MIGRATION: Update code limits
-- ==================================================
-- After running this migration, update the following in AsbisSyncService.java:
--
-- validateStringLength(parameterKey, "Parameter key", 500);           // was 255
-- validateStringLength(value, "Parameter option", 1000);              // was 255
-- validateStringLength(productCategory, "Product category", 500);     // was 255
-- validateStringLength(asbisId, "Category Asbis ID", 500);           // was 255