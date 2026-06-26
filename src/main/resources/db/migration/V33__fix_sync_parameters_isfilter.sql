-- Phase 1a: Remove junk MOST parameters created before the properties-map fix.
-- Before the fix, syncMostParameters iterated over all top-level product fields
-- (name, price, category, partNumber, inStock, etc.) and treated them as specs.
-- These are metadata fields, not real parameters, and must be cleaned up.

DO $$
DECLARE
    junk_ids BIGINT[];
BEGIN
    SELECT ARRAY_AGG(id) INTO junk_ids
    FROM parameters
    WHERE most_key IN (
        'name', 'price', 'category', 'partnumber', 'instock', 'currency',
        'manufacturer', 'code', 'partnum', 'in_stock', 'general_description',
        'gallery', 'main_picture_url', 'web_link', 'in_stock_qty',
        'warehouse', 'ean', 'weight', 'width', 'height', 'depth',
        'url', 'link', 'image', 'images', 'description'
    );

    IF junk_ids IS NOT NULL AND array_length(junk_ids, 1) > 0 THEN
        DELETE FROM product_parameters  WHERE parameter_id = ANY(junk_ids);
        DELETE FROM parameter_options   WHERE parameter_id = ANY(junk_ids);
        DELETE FROM category_parameters WHERE parameter_id = ANY(junk_ids);
        DELETE FROM parameters          WHERE id           = ANY(junk_ids);
        RAISE NOTICE 'Deleted % junk MOST parameters', array_length(junk_ids, 1);
    ELSE
        RAISE NOTICE 'No junk MOST parameters found — nothing to delete';
    END IF;
END $$;

-- Phase 1b: Enable filtering for parameters that have 2–50 distinct options.
-- Entries with is_filter IS NULL were inserted without the flag being set explicitly.
-- Parameters with 2–50 options are meaningful facets (e.g. RAM, CPU Socket, Color).
-- Parameters with 1 option are useless as filters; those with >50 are usually free text.

UPDATE category_parameters
SET    is_filter = true
WHERE  is_filter IS NULL
  AND  parameter_id IN (
           SELECT   parameter_id
           FROM     parameter_options
           GROUP BY parameter_id
           HAVING   COUNT(*) BETWEEN 2 AND 50
       );
