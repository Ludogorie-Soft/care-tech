-- V6: Add per-category is_filter flag to category_parameters junction table
--
-- This replaces the global parameters.is_filter as the source-of-truth for
-- which parameters are shown as filters.  The global flag remains but is no
-- longer used by the filter queries after this migration.
--
-- Default = TRUE so all existing category↔parameter relationships keep working
-- as before.  V7 will then do the data-driven cleanup.

ALTER TABLE category_parameters
    ADD COLUMN is_filter BOOLEAN NOT NULL DEFAULT TRUE;

-- Index to speed up the WHERE cp.is_filter = true filter in the query
CREATE INDEX idx_category_parameters_is_filter
    ON category_parameters (category_id, is_filter);
