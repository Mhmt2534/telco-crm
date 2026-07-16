CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE orders
    ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE orders
    ADD CONSTRAINT uq_orders_public_id UNIQUE (public_id);

ALTER TABLE order_item
    ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE order_item
    ADD CONSTRAINT uq_order_item_public_id UNIQUE (public_id);
