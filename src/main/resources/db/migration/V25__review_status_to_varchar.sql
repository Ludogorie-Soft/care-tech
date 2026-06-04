ALTER TABLE product_reviews
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE product_reviews
    ALTER COLUMN status TYPE VARCHAR(20) USING status::VARCHAR;

ALTER TABLE product_reviews
    ALTER COLUMN status SET DEFAULT 'PENDING';

DROP TYPE IF EXISTS review_status;
