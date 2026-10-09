-- V43: Promotions can end automatically.
--
-- promo_ends_at is Europe/Sofia local time, like every other timestamp in this schema;
-- PromotionExpiryService removes the discount once it has passed.
ALTER TABLE products ADD COLUMN promo_ends_at TIMESTAMP;

CREATE INDEX idx_products_promo_ends_at ON products (promo_ends_at) WHERE promo_ends_at IS NOT NULL;

-- Categories and brands keep the percentage of their promotion so it can be edited
ALTER TABLE categories
    ADD COLUMN promo_discount NUMERIC(10, 6),
    ADD COLUMN promo_ends_at  TIMESTAMP;

ALTER TABLE manufacturers
    ADD COLUMN promo_discount NUMERIC(10, 6),
    ADD COLUMN promo_ends_at  TIMESTAMP;

-- Promotions started before this migration stored no percentage: use the most common one among the products
UPDATE categories c
SET promo_discount = (SELECT p.discount
                      FROM products p
                      WHERE p.category_id = c.id AND p.deleted = false AND p.discount > 0
                      GROUP BY p.discount
                      ORDER BY count(*) DESC, p.discount DESC
                      LIMIT 1)
WHERE c.is_promo_active = true;

UPDATE manufacturers m
SET promo_discount = (SELECT p.discount
                      FROM products p
                      WHERE p.manufacturer_id = m.id AND p.deleted = false AND p.discount > 0
                      GROUP BY p.discount
                      ORDER BY count(*) DESC, p.discount DESC
                      LIMIT 1)
WHERE m.is_promo_active = true;
