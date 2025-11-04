ALTER TABLE products
ADD COLUMN IF NOT EXISTS slug TEXT;

CREATE INDEX IF NOT EXISTS idx_products_slug ON products(slug);