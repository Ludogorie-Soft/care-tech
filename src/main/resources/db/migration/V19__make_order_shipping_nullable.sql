-- Allow shipping address fields to be null for leasing orders
-- (shipping is confirmed after TBI approves the loan, not at order creation)
ALTER TABLE orders ALTER COLUMN shipping_address DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN shipping_city    DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN shipping_method  DROP NOT NULL;
