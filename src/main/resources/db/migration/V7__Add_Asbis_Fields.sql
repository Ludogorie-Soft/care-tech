-- Migration to add Asbis integration fields
-- Filename: src/main/resources/db/migration/V7__Add_Asbis_Fields.sql

-- Add Asbis fields to categories table
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS asbis_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS asbis_code VARCHAR(100);

-- Add index for faster lookups
CREATE INDEX IF NOT EXISTS idx_categories_asbis_id ON categories(asbis_id);
CREATE INDEX IF NOT EXISTS idx_categories_asbis_code ON categories(asbis_code);

-- Add Asbis fields to manufacturers table
ALTER TABLE manufacturers
    ADD COLUMN IF NOT EXISTS asbis_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS asbis_code VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_manufacturers_asbis_id ON manufacturers(asbis_id);
CREATE INDEX IF NOT EXISTS idx_manufacturers_asbis_code ON manufacturers(asbis_code);

-- Add Asbis fields to products table
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS asbis_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS asbis_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS asbis_part_number VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_products_asbis_id ON products(asbis_id);
CREATE INDEX IF NOT EXISTS idx_products_asbis_code ON products(asbis_code);
CREATE INDEX IF NOT EXISTS idx_products_asbis_part_number ON products(asbis_part_number);

-- Add Asbis fields to parameters table
ALTER TABLE parameters
    ADD COLUMN IF NOT EXISTS asbis_key VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_parameters_asbis_key ON parameters(asbis_key);

-- Add comments for documentation
COMMENT ON COLUMN categories.asbis_id IS 'Asbis category ID';
COMMENT ON COLUMN categories.asbis_code IS 'Asbis category code';
COMMENT ON COLUMN manufacturers.asbis_id IS 'Asbis manufacturer ID';
COMMENT ON COLUMN manufacturers.asbis_code IS 'Asbis manufacturer code';
COMMENT ON COLUMN products.asbis_id IS 'Asbis product ID';
COMMENT ON COLUMN products.asbis_code IS 'Asbis product code';
COMMENT ON COLUMN products.asbis_part_number IS 'Asbis part number for unique identification';
COMMENT ON COLUMN parameters.asbis_key IS 'Asbis parameter key for mapping';