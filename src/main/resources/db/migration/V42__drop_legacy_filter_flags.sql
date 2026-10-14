-- V42: Drop the raw-layer filter flags.
--
-- Filters come only from the canonical layer (V38). Since the phase 6 cleanup nothing reads
-- parameters.is_filter, parameters.filter_order or category_parameters.is_filter; the syncs only kept
-- writing them. Their indexes (V2, V6) go with the columns.
ALTER TABLE category_parameters DROP COLUMN IF EXISTS is_filter;
ALTER TABLE parameters DROP COLUMN IF EXISTS is_filter;
ALTER TABLE parameters DROP COLUMN IF EXISTS filter_order;
