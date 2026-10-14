ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS warranty_file_path VARCHAR(500);
