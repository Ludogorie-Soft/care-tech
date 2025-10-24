

ALTER TABLE products
ALTER COLUMN platform TYPE VARCHAR USING platform::text;

ALTER TABLE categories
ALTER COLUMN platform TYPE VARCHAR USING platform::text;

ALTER TABLE manufacturers
ALTER COLUMN platform TYPE VARCHAR USING platform::text;

ALTER TABLE parameters
ALTER COLUMN platform TYPE VARCHAR USING platform::text;

DROP TYPE platform_type;