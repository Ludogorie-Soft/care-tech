-- Soft-delete support for products.
-- When an admin "deletes" a product it is marked deleted=true rather than removed.
-- This preserves order_items history while making the product inaccessible everywhere else.
ALTER TABLE products ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
