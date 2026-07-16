CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE tariff
    ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE tariff
    ADD CONSTRAINT uq_tariff_public_id UNIQUE (public_id);

ALTER TABLE addon
    ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE addon
    ADD CONSTRAINT uq_addon_public_id UNIQUE (public_id);
