ALTER TABLE user_companies
    ADD COLUMN IF NOT EXISTS seat               TEXT,
    ADD COLUMN IF NOT EXISTS management_address TEXT;
