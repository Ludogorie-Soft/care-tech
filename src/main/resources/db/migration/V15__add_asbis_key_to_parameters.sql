-- V15: Add asbis_key column to parameters table
-- Used to link parameters to Asbis attribute names (attrlist keys),
-- enabling cross-platform parameter reuse without duplicates.

ALTER TABLE parameters
    ADD COLUMN IF NOT EXISTS asbis_key VARCHAR(255);
