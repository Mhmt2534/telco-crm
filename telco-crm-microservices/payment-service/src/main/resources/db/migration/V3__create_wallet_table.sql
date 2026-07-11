-- Flyway Database Migration V3: Create Wallet Table and Add Columns to Payments Table

CREATE TABLE wallets (
    id            UUID PRIMARY KEY,
    customer_id   VARCHAR(255) NOT NULL UNIQUE,
    balance       NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    balance_hash  VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    version       BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE payments ADD COLUMN wallet_amount NUMERIC(19, 2) DEFAULT 0.00;
ALTER TABLE payments ADD COLUMN card_amount NUMERIC(19, 2) DEFAULT 0.00;
