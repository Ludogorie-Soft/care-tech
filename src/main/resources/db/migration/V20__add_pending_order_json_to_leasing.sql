ALTER TABLE leasing_applications
    ADD COLUMN IF NOT EXISTS pending_order_json TEXT;
