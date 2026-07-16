CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE invoice
    ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE invoice
    ADD CONSTRAINT uq_invoice_public_id UNIQUE (public_id);
