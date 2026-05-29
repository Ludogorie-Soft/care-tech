-- V12: TBI Fusion Pay leasing applications
-- Tracks every credit application initiated through TBI Bank.
-- order_id is nullable — an order may not exist at the time the application is created
-- (express-buy flow starts before checkout).

CREATE TABLE leasing_applications (
    id                   BIGSERIAL     PRIMARY KEY,

    -- Link to our orders table (set after order is confirmed)
    order_id             BIGINT        REFERENCES orders(id) ON DELETE SET NULL,

    -- TBI identifiers returned by RegisterApplication
    tbi_order_id         INTEGER,
    tbi_token            VARCHAR(200),

    -- CreditApplicationId received via webhook
    tbi_application_id   VARCHAR(200),

    -- Our order number/id that was sent to TBI (e.g. "0" for express-buy, or "ORD-2025-00001")
    store_order_id       VARCHAR(100),

    -- Current status — mirrors TBI status strings (Draft, Approved, Rejected, etc.)
    status               VARCHAR(100)  NOT NULL DEFAULT 'Draft',

    -- Financial details populated from webhook / GetApplicationData
    amount_eur           DECIMAL(10,2),
    installment          DECIMAL(10,2),
    downpayment          DECIMAL(10,2),
    period_months        INTEGER,

    -- Customer info (denormalised for fast admin display)
    customer_first_name  VARCHAR(100),
    customer_last_name   VARCHAR(100),
    customer_phone       VARCHAR(30),
    customer_email       VARCHAR(200),

    -- Product info (denormalised from the moment of application)
    product_id           BIGINT,
    product_name         VARCHAR(500),

    -- JSON payloads for audit / debugging
    items_json           TEXT,                    -- serialised items list sent to TBI
    status_history       TEXT,                    -- JSON array of {datetime, status} changes
    tbi_raw_response     TEXT,                    -- raw JSON from RegisterApplication response

    -- Timestamps
    webhook_received_at  TIMESTAMP,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,

    -- Audit (required by BaseEntity)
    created_by           VARCHAR(100) DEFAULT 'system',
    last_modified_by     VARCHAR(100) DEFAULT 'system'
);

-- Indexes for common admin queries
CREATE INDEX idx_leasing_status          ON leasing_applications(status);
CREATE INDEX idx_leasing_tbi_order_id    ON leasing_applications(tbi_order_id);
CREATE INDEX idx_leasing_store_order_id  ON leasing_applications(store_order_id);
CREATE INDEX idx_leasing_tbi_app_id      ON leasing_applications(tbi_application_id);
CREATE INDEX idx_leasing_order_id        ON leasing_applications(order_id);
CREATE INDEX idx_leasing_created_at      ON leasing_applications(created_at DESC);
