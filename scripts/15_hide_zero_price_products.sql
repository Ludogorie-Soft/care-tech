-- ============================================================
-- Script 15: Hide products with zero or null final_price
-- Fixes existing bad data left by sync services that did not
-- check price before setting show_flag = true.
-- ============================================================

BEGIN;

-- Preview: how many products will be hidden
SELECT
    COUNT(*) FILTER (WHERE final_price IS NULL)  AS null_price,
    COUNT(*) FILTER (WHERE final_price = 0)      AS zero_price,
    COUNT(*)                                     AS total_to_hide
FROM products
WHERE active = true
  AND show_flag = true
  AND (final_price IS NULL OR final_price = 0);

-- Breakdown by platform
SELECT platform, COUNT(*) AS products_to_hide
FROM products
WHERE active = true
  AND show_flag = true
  AND (final_price IS NULL OR final_price = 0)
GROUP BY platform
ORDER BY products_to_hide DESC;

/*
-- Uncomment to execute
UPDATE products
SET
    show_flag  = false,
    status     = 'NOT_AVAILABLE',
    updated_at = CURRENT_TIMESTAMP
WHERE active = true
  AND show_flag = true
  AND (final_price IS NULL OR final_price = 0);
*/

ROLLBACK; -- Replace with COMMIT after verifying the preview
