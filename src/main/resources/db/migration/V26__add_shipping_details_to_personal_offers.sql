ALTER TABLE personal_offers
    ADD COLUMN IF NOT EXISTS shipping_details jsonb;
