CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE customer
    ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE customer
    ADD CONSTRAINT uq_customer_public_id UNIQUE (public_id);
