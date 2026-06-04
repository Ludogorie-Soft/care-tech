CREATE TYPE review_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE product_reviews (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating            SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment           TEXT,
    status            review_status NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(100),
    last_modified_by  VARCHAR(100),
    CONSTRAINT uq_review_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_reviews_product_status ON product_reviews(product_id, status);
CREATE INDEX idx_reviews_status ON product_reviews(status);
