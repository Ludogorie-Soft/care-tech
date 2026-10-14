-- V5__update_fts_combined_index.sql
-- Replace language-specific FTS indexes with a single combined index
-- covering both name_bg, name_en, description_bg, description_en, model, reference_number.
-- This matches the tsvector used in ProductSearchRepository.searchProducts().

DROP INDEX IF EXISTS idx_products_fts_bg;
DROP INDEX IF EXISTS idx_products_fts_en;

CREATE INDEX idx_products_fts_combined ON products USING gin(
    to_tsvector('simple',
        coalesce(name_bg, '') || ' ' ||
        coalesce(name_en, '') || ' ' ||
        coalesce(description_bg, '') || ' ' ||
        coalesce(description_en, '') || ' ' ||
        coalesce(model, '') || ' ' ||
        coalesce(reference_number, '')
    )
);

ANALYZE products;
