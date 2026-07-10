-- V2: Update tables for KART 19 requirements

ALTER TABLE payments 
    ADD COLUMN customer_id VARCHAR(255),
    ADD COLUMN currency VARCHAR(10) DEFAULT 'TRY' NOT NULL;

ALTER TABLE payments 
    RENAME COLUMN payment_request_id TO idempotency_key;

ALTER TABLE payment_attempts
    ADD COLUMN response_code VARCHAR(50),
    ADD COLUMN attempted_at TIMESTAMPTZ;

ALTER TABLE payment_attempts
    RENAME COLUMN response TO response_message;
