CREATE TABLE personal_offers (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    message         TEXT,
    offer_items     JSONB NOT NULL DEFAULT '[]',
    discount_percent NUMERIC(5, 2),
    expires_at      TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(200),
    updated_by      VARCHAR(200)
);

CREATE INDEX idx_personal_offers_user_id ON personal_offers(user_id);
CREATE INDEX idx_personal_offers_status  ON personal_offers(status);
