-- =============================================================================
-- Script 40a: Pre-flight dedup — run BEFORE (re-)running script 40
-- =============================================================================
-- Problem: some products have BOTH a non-canonical parameter option AND the
-- canonical parameter option with the same name_bg. When script 40 tries to
-- UPDATE the non-canonical row to the canonical (parameter_id, option_id),
-- it hits the unique constraint uq_product_parameters_unique.
--
-- Fix: delete the non-canonical row when the canonical equivalent already
-- exists for the same product. The canonical row stays; the duplicate is gone.
-- Script 40 Step 4 handles any remaining duplicates after the merge.
-- =============================================================================

BEGIN;

-- Delete product_parameters rows where:
--   • the option belongs to a non-canonical parameter (nc_opt)
--   • a canonical option with identical name_bg exists (c_opt)
--   • the same product already has a row pointing to that canonical option
DELETE FROM product_parameters pp
USING parameter_options nc_opt,
      parameter_options c_opt
WHERE pp.parameter_option_id = nc_opt.id
  AND nc_opt.parameter_id     != c_opt.parameter_id   -- different parameters
  AND LOWER(TRIM(nc_opt.name_bg)) = LOWER(TRIM(c_opt.name_bg))
  AND EXISTS (
      SELECT 1
      FROM product_parameters pp2
      WHERE pp2.product_id          = pp.product_id
        AND pp2.parameter_option_id = c_opt.id
        AND pp2.parameter_id        = c_opt.parameter_id
  );

COMMIT;
