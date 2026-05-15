-- Add missing BaseEntity audit columns to user_addresses
ALTER TABLE user_addresses
    ADD COLUMN IF NOT EXISTS created_by       VARCHAR(100) DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100) DEFAULT 'system';

-- Add missing BaseEntity audit columns to user_companies
ALTER TABLE user_companies
    ADD COLUMN IF NOT EXISTS created_by       VARCHAR(100) DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100) DEFAULT 'system';
