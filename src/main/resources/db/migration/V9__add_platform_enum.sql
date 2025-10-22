-- V9__add_platform_enum.sql
-- Migration to add Platform enum field to support multiple data sources
-- Platform values: VALI, TEKRA, ASBIS

-- ==================================================
-- CREATE ENUM TYPE
-- ==================================================

-- Create the platform enum type
CREATE TYPE platform_type AS ENUM ('VALI', 'TEKRA', 'ASBIS');

COMMENT ON TYPE platform_type IS 'Platform/Source of data: VALI, TEKRA, or ASBIS';

-- ==================================================
-- ADD PLATFORM COLUMN TO CATEGORIES
-- ==================================================

ALTER TABLE categories
    ADD COLUMN platform platform_type;

-- Set default platform for existing Asbis categories (where asbis_id is not null)
UPDATE categories
SET platform = 'ASBIS'
WHERE asbis_id IS NOT NULL AND asbis_id != '';

-- Set default platform for existing Tekra categories (where tekra_id is not null)
UPDATE categories
SET platform = 'TEKRA'
WHERE tekra_id IS NOT NULL AND tekra_id != '' AND platform IS NULL;

-- Create index for faster lookups
CREATE INDEX idx_categories_platform ON categories(platform);

COMMENT ON COLUMN categories.platform IS 'Data source platform: VALI, TEKRA, or ASBIS';

-- ==================================================
-- ADD PLATFORM COLUMN TO MANUFACTURERS
-- ==================================================

ALTER TABLE manufacturers
    ADD COLUMN platform platform_type;

-- Set default platform for existing Asbis manufacturers
UPDATE manufacturers
SET platform = 'ASBIS'
WHERE asbis_id IS NOT NULL AND asbis_id != '';

-- Create index
CREATE INDEX idx_manufacturers_platform ON manufacturers(platform);

COMMENT ON COLUMN manufacturers.platform IS 'Data source platform: VALI, TEKRA, or ASBIS';

-- ==================================================
-- ADD PLATFORM COLUMN TO PRODUCTS
-- ==================================================

ALTER TABLE products
    ADD COLUMN platform platform_type;

-- Set default platform for existing Asbis products
UPDATE products
SET platform = 'ASBIS'
WHERE asbis_id IS NOT NULL AND asbis_id != '';

-- Set default platform for existing Tekra products
UPDATE products
SET platform = 'TEKRA'
WHERE tekra_id IS NOT NULL AND tekra_id != '' AND platform IS NULL;

-- Create index
CREATE INDEX idx_products_platform ON products(platform);

COMMENT ON COLUMN products.platform IS 'Data source platform: VALI, TEKRA, or ASBIS';

-- ==================================================
-- ADD PLATFORM COLUMN TO PARAMETERS
-- ==================================================

ALTER TABLE parameters
    ADD COLUMN platform platform_type;

-- Set default platform for existing Asbis parameters
UPDATE parameters
SET platform = 'ASBIS'
WHERE asbis_key IS NOT NULL AND asbis_key != '';

-- Set default platform for existing Tekra parameters
UPDATE parameters
SET platform = 'TEKRA'
WHERE tekra_key IS NOT NULL AND tekra_key != '' AND platform IS NULL;

-- Create index
CREATE INDEX idx_parameters_platform ON parameters(platform);

COMMENT ON COLUMN parameters.platform IS 'Data source platform: VALI, TEKRA, or ASBIS';

-- ==================================================
-- VERIFICATION QUERY
-- ==================================================

-- Verify the migration by counting records per platform
DO $$
DECLARE
    cat_vali_count INTEGER;
    cat_tekra_count INTEGER;
    cat_asbis_count INTEGER;
    cat_null_count INTEGER;

    mfr_vali_count INTEGER;
    mfr_tekra_count INTEGER;
    mfr_asbis_count INTEGER;
    mfr_null_count INTEGER;

    prod_vali_count INTEGER;
    prod_tekra_count INTEGER;
    prod_asbis_count INTEGER;
    prod_null_count INTEGER;

    param_vali_count INTEGER;
    param_tekra_count INTEGER;
    param_asbis_count INTEGER;
    param_null_count INTEGER;
BEGIN
    -- Categories counts
    SELECT COUNT(*) INTO cat_vali_count FROM categories WHERE platform = 'VALI';
    SELECT COUNT(*) INTO cat_tekra_count FROM categories WHERE platform = 'TEKRA';
    SELECT COUNT(*) INTO cat_asbis_count FROM categories WHERE platform = 'ASBIS';
    SELECT COUNT(*) INTO cat_null_count FROM categories WHERE platform IS NULL;

    -- Manufacturers counts
    SELECT COUNT(*) INTO mfr_vali_count FROM manufacturers WHERE platform = 'VALI';
    SELECT COUNT(*) INTO mfr_tekra_count FROM manufacturers WHERE platform = 'TEKRA';
    SELECT COUNT(*) INTO mfr_asbis_count FROM manufacturers WHERE platform = 'ASBIS';
    SELECT COUNT(*) INTO mfr_null_count FROM manufacturers WHERE platform IS NULL;

    -- Products counts
    SELECT COUNT(*) INTO prod_vali_count FROM products WHERE platform = 'VALI';
    SELECT COUNT(*) INTO prod_tekra_count FROM products WHERE platform = 'TEKRA';
    SELECT COUNT(*) INTO prod_asbis_count FROM products WHERE platform = 'ASBIS';
    SELECT COUNT(*) INTO prod_null_count FROM products WHERE platform IS NULL;

    -- Parameters counts
    SELECT COUNT(*) INTO param_vali_count FROM parameters WHERE platform = 'VALI';
    SELECT COUNT(*) INTO param_tekra_count FROM parameters WHERE platform = 'TEKRA';
    SELECT COUNT(*) INTO param_asbis_count FROM parameters WHERE platform = 'ASBIS';
    SELECT COUNT(*) INTO param_null_count FROM parameters WHERE platform IS NULL;

    -- Display results
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Platform Migration Completed Successfully';
    RAISE NOTICE '========================================';
    RAISE NOTICE '';
    RAISE NOTICE 'CATEGORIES:';
    RAISE NOTICE '  VALI:   %', cat_vali_count;
    RAISE NOTICE '  TEKRA:  %', cat_tekra_count;
    RAISE NOTICE '  ASBIS:  %', cat_asbis_count;
    RAISE NOTICE '  NULL:   %', cat_null_count;
    RAISE NOTICE '';
    RAISE NOTICE 'MANUFACTURERS:';
    RAISE NOTICE '  VALI:   %', mfr_vali_count;
    RAISE NOTICE '  TEKRA:  %', mfr_tekra_count;
    RAISE NOTICE '  ASBIS:  %', mfr_asbis_count;
    RAISE NOTICE '  NULL:   %', mfr_null_count;
    RAISE NOTICE '';
    RAISE NOTICE 'PRODUCTS:';
    RAISE NOTICE '  VALI:   %', prod_vali_count;
    RAISE NOTICE '  TEKRA:  %', prod_tekra_count;
    RAISE NOTICE '  ASBIS:  %', prod_asbis_count;
    RAISE NOTICE '  NULL:   %', prod_null_count;
    RAISE NOTICE '';
    RAISE NOTICE 'PARAMETERS:';
    RAISE NOTICE '  VALI:   %', param_vali_count;
    RAISE NOTICE '  TEKRA:  %', param_tekra_count;
    RAISE NOTICE '  ASBIS:  %', param_asbis_count;
    RAISE NOTICE '  NULL:   %', param_null_count;
    RAISE NOTICE '';
    RAISE NOTICE '========================================';
END $$;

-- ==================================================
-- HELPER QUERY: Show sample records by platform
-- ==================================================

-- Sample categories by platform
SELECT 'CATEGORIES' as table_name, platform, COUNT(*) as count
FROM categories
GROUP BY platform
ORDER BY count DESC;

-- Sample manufacturers by platform
SELECT 'MANUFACTURERS' as table_name, platform, COUNT(*) as count
FROM manufacturers
GROUP BY platform
ORDER BY count DESC;

-- Sample products by platform
SELECT 'PRODUCTS' as table_name, platform, COUNT(*) as count
FROM products
GROUP BY platform
ORDER BY count DESC;

-- Sample parameters by platform
SELECT 'PARAMETERS' as table_name, platform, COUNT(*) as count
FROM parameters
GROUP BY platform
ORDER BY count DESC;


