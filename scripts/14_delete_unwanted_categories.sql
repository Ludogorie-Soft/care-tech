-- ============================================================
-- Script 14: Delete unwanted categories and their products
-- Categories are primarily from ASBIS and are not relevant
-- to the store's product range.
--
-- Strategy:
--   1. Collect all target category IDs (+ their subcategories recursively)
--   2. Products WITH order_items → soft-delete (active=false, show_flag=false, category_id=NULL)
--   3. Products WITHOUT order_items → hard-delete (cascades handle child tables)
--   4. Delete the categories (cascades handle category_parameters)
--
-- Run as a preview first (Step 0), then uncomment the DML block.
-- ============================================================

BEGIN;

-- ============================================================
-- STEP 0: PREVIEW — verify what will be affected
-- (safe to run anytime, makes no changes)
-- ============================================================

WITH RECURSIVE target_cats AS (
    SELECT id, name_bg
    FROM categories
    WHERE trim(name_bg) IN (
        'Digital signage за вътрешна употреба',
        'Таблети',
        'Телефони',
        'VoIP телефон',
        'Смартфони',
        'Несъответствие с Розета данните',
        'Аксесоари за сценична употреба',
        'Готово за носене',
        'Производствени материали',
        'Модни аксесоари',
        'Аксесоари за флексибилни устройства',
        'Удължена гаранция',
        'Други',
        'Кухненски робот',
        'Прахосмукачка прътова',
        'Аксесоари за вакуумзатваряне',
        'Иригатори',
        'Тенджери под налягане',
        'Блендер',
        'Прахосмукачка трансформер',
        'Робот прахосмукачка',
        'Реновиран лаптоп',
        'Едра домакинска техника',
        'LED осветление'
    )
    UNION ALL
    SELECT c.id, c.name_bg
    FROM categories c
    INNER JOIN target_cats tc ON c.parent_id = tc.id
)
SELECT
    (SELECT COUNT(*)                   FROM target_cats)                                          AS categories_to_delete,
    (SELECT COUNT(*)                   FROM products WHERE category_id IN (SELECT id FROM target_cats))  AS total_products,
    (SELECT COUNT(DISTINCT oi.product_id)
     FROM order_items oi
     WHERE oi.product_id IN (SELECT id FROM products WHERE category_id IN (SELECT id FROM target_cats))) AS products_with_orders_soft_delete,
    (SELECT COUNT(*)
     FROM products p
     WHERE p.category_id IN (SELECT id FROM target_cats)
       AND p.id NOT IN (SELECT DISTINCT product_id FROM order_items))                             AS products_to_hard_delete;

-- Uncomment below to see which categories were matched:
-- WITH RECURSIVE target_cats AS (
--     SELECT id, name_bg, parent_id FROM categories
--     WHERE trim(name_bg) IN ( ... same list ... )
--     UNION ALL
--     SELECT c.id, c.name_bg, c.parent_id FROM categories c
--     INNER JOIN target_cats tc ON c.parent_id = tc.id
-- )
-- SELECT id, name_bg FROM target_cats ORDER BY name_bg;


-- ============================================================
-- STEP 1–4: ACTUAL DELETION
-- Uncomment the block below when ready to execute.
-- ============================================================

/*

WITH RECURSIVE target_cats AS (
    SELECT id
    FROM categories
    WHERE trim(name_bg) IN (
        'Digital signage за вътрешна употреба',
        'Таблети',
        'Телефони',
        'VoIP телефон',
        'Смартфони',
        'Несъответствие с Розета данните',
        'Аксесоари за сценична употреба',
        'Готово за носене',
        'Производствени материали',
        'Модни аксесоари',
        'Аксесоари за флексибилни устройства',
        'Удължена гаранция',
        'Други',
        'Кухненски робот',
        'Прахосмукачка прътова',
        'Аксесоари за вакуумзатваряне',
        'Иригатори',
        'Тенджери под налягане',
        'Блендер',
        'Прахосмукачка трансформер',
        'Робот прахосмукачка',
        'Реновиран лаптоп',
        'Едра домакинска техника',
        'LED осветление'
    )
    UNION ALL
    SELECT c.id FROM categories c
    INNER JOIN target_cats tc ON c.parent_id = tc.id
),
target_product_ids AS (
    SELECT id FROM products
    WHERE category_id IN (SELECT id FROM target_cats)
),
ordered_product_ids AS (
    SELECT DISTINCT product_id AS id FROM order_items
    WHERE product_id IN (SELECT id FROM target_product_ids)
)

-- Step 1: Soft-delete products that are referenced in order_items
UPDATE products
SET
    active       = false,
    show_flag    = false,
    category_id  = NULL,
    updated_at   = CURRENT_TIMESTAMP
WHERE id IN (SELECT id FROM ordered_product_ids);

-- Step 2: Hard-delete products that are NOT referenced in order_items
-- (Cascades automatically remove: product_parameters, product_flags,
--  additional_images, cart_items, user_favorites)
DELETE FROM products
WHERE category_id IN (
    WITH RECURSIVE tc AS (
        SELECT id FROM categories
        WHERE trim(name_bg) IN (
            'Digital signage за вътрешна употреба',
            'Таблети',
            'Телефони',
            'VoIP телефон',
            'Смартфони',
            'Несъответствие с Розета данните',
            'Аксесоари за сценична употреба',
            'Готово за носене',
            'Производствени материали',
            'Модни аксесоари',
            'Аксесоари за флексибилни устройства',
            'Удължена гаранция',
            'Други',
            'Кухненски робот',
            'Прахосмукачка прътова',
            'Аксесоари за вакуумзатваряне',
            'Иригатори',
            'Тенджери под налягане',
            'Блендер',
            'Прахосмукачка трансформер',
            'Робот прахосмукачка',
            'Реновиран лаптоп',
            'Едра домакинска техника',
            'LED осветление'
        )
        UNION ALL
        SELECT c.id FROM categories c INNER JOIN tc ON c.parent_id = tc.id
    )
    SELECT id FROM tc
)
AND id NOT IN (SELECT DISTINCT product_id FROM order_items);

-- Step 3: Delete the categories
-- (Cascades automatically remove: category_parameters entries;
--  remaining products with orders get category_id = NULL via ON DELETE SET NULL)
DELETE FROM categories
WHERE id IN (
    WITH RECURSIVE tc AS (
        SELECT id FROM categories
        WHERE trim(name_bg) IN (
            'Digital signage за вътрешна употреба',
            'Таблети',
            'Телефони',
            'VoIP телефон',
            'Смартфони',
            'Несъответствие с Розета данните',
            'Аксесоари за сценична употреба',
            'Готово за носене',
            'Производствени материали',
            'Модни аксесоари',
            'Аксесоари за флексибилни устройства',
            'Удължена гаранция',
            'Други',
            'Кухненски робот',
            'Прахосмукачка прътова',
            'Аксесоари за вакуумзатваряне',
            'Иригатори',
            'Тенджери под налягане',
            'Блендер',
            'Прахосмукачка трансформер',
            'Робот прахосмукачка',
            'Реновиран лаптоп',
            'Едра домакинска техника',
            'LED осветление'
        )
        UNION ALL
        SELECT c.id FROM categories c INNER JOIN tc ON c.parent_id = tc.id
    )
    SELECT id FROM tc
);

*/

-- ============================================================
-- NOTE: To prevent ASBIS sync from recreating these categories,
-- add their asbis_id values to the VALI_EXCLUDED_CATEGORIES_IDS
-- environment variable, or handle exclusion in AsbisApiService.
--
-- To find their asbis_id values before deleting:
--   SELECT id, name_bg, asbis_id, asbis_code FROM categories
--   WHERE trim(name_bg) IN ('Таблети', 'Телефони', ...);
-- ============================================================

ROLLBACK; -- Replace with COMMIT; after verifying the preview output
