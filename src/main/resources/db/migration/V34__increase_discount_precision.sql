-- Increase discount column precision to support fractional percentages
-- (e.g. 25.6673%) needed for exact target-price promotions
ALTER TABLE products ALTER COLUMN discount TYPE numeric(10, 6);
