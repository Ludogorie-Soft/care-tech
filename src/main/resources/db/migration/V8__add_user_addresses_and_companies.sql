-- User delivery addresses
CREATE TABLE user_addresses (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label       VARCHAR(100),
    type        VARCHAR(20) NOT NULL DEFAULT 'address', -- 'address' | 'speedy_office'
    address_line TEXT,
    city        VARCHAR(100),
    postal_code VARCHAR(20),
    is_default  BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_addresses_user_id ON user_addresses(user_id);

-- User company data (multiple companies per user)
CREATE TABLE user_companies (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(200) NOT NULL,
    eik          VARCHAR(50),
    vat_number   VARCHAR(50),
    mol          VARCHAR(100),
    address      TEXT,
    city         VARCHAR(100),
    is_default   BOOLEAN NOT NULL DEFAULT false,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_companies_user_id ON user_companies(user_id);
