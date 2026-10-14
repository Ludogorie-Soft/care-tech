-- ============================================================
-- Script 16: Fix products assigned to hidden categories
-- Root cause: products are linked to categories with show_flag=false
-- which causes the frontend Category page to redirect to "/" because
-- the category is not returned by the API (findByShowTrue filter).
-- ============================================================

-- ============================================================
-- STEP 0: DIAGNOSIS — which hidden categories have products?
-- ============================================================

SELECT
    c.id             AS category_id,
    c.name_bg        AS category_name,
    c.slug           AS category_slug,
    c.show_flag,
    c.platform,
    pc.name_bg       AS parent_name,
    pc.show_flag     AS parent_show_flag,
    COUNT(p.id)      AS product_count
FROM categories c
JOIN products p ON p.category_id = c.id
LEFT JOIN categories pc ON pc.id = c.parent_id
WHERE c.show_flag = false
  AND p.show_flag = true
GROUP BY c.id, c.name_bg, c.slug, c.show_flag, c.platform, pc.name_bg, pc.show_flag
ORDER BY product_count DESC;

-- ============================================================
-- STEP 1: Find the correct target category (the visible one)
-- Run this to see the visible categories and their IDs
-- so you can pick the right one for reassignment.
-- ============================================================

-- Example: find all visible categories related to "диск" or "hdd"
SELECT id, name_bg, slug, parent_id, show_flag
FROM categories
WHERE show_flag = true
  AND (LOWER(name_bg) LIKE '%диск%' OR LOWER(name_bg) LIKE '%hdd%' OR LOWER(name_bg) LIKE '%хард%')
ORDER BY name_bg;

-- ============================================================
-- STEP 2: REASSIGN products from a hidden category to a visible one
-- Replace :hidden_category_id and :target_category_id with real values
-- from the queries above.
-- ============================================================

/*
BEGIN;

UPDATE products
SET
    category_id = :target_category_id,   -- e.g. ID of "ХАРД ДИСКОВЕ - 3.5""
    updated_at  = CURRENT_TIMESTAMP
WHERE category_id = :hidden_category_id  -- e.g. ID of "Твърди дискове"
  AND show_flag = true;

-- Verify
SELECT COUNT(*) FROM products WHERE category_id = :target_category_id AND show_flag = true;

ROLLBACK; -- Replace with COMMIT after verification
*/

-- ============================================================
-- STEP 3: Hide the now-empty hidden category (optional cleanup)
-- Only if you want to clean up the category itself too.
-- ============================================================

/*
-- Check it's empty first
SELECT COUNT(*) FROM products WHERE category_id = :hidden_category_id;

-- Then delete or leave it hidden:
-- DELETE FROM categories WHERE id = :hidden_category_id AND (SELECT COUNT(*) FROM products WHERE category_id = :hidden_category_id) = 0;
*/
