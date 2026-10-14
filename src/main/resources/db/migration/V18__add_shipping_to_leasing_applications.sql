-- Add shipping address fields to leasing_applications table
-- These are collected from the customer before the TBI iframe is opened

ALTER TABLE leasing_applications
    ADD COLUMN IF NOT EXISTS shipping_address        VARCHAR(200),
    ADD COLUMN IF NOT EXISTS shipping_city           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS shipping_postal_code    VARCHAR(20),
    ADD COLUMN IF NOT EXISTS is_to_speedy_office     BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS shipping_speedy_site_id   BIGINT,
    ADD COLUMN IF NOT EXISTS shipping_speedy_office_id BIGINT,
    ADD COLUMN IF NOT EXISTS shipping_speedy_site_name   VARCHAR(200),
    ADD COLUMN IF NOT EXISTS shipping_speedy_office_name VARCHAR(200);
