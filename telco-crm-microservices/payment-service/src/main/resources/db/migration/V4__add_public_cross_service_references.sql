ALTER TABLE payments ADD COLUMN order_public_id UUID;
ALTER TABLE payments ADD COLUMN customer_public_id UUID;
ALTER TABLE payments ADD COLUMN actor_id VARCHAR(255);

ALTER TABLE wallets ADD COLUMN customer_public_id UUID;
ALTER TABLE wallets ALTER COLUMN customer_id DROP NOT NULL;

CREATE INDEX idx_payments_order_public_id ON payments(order_public_id);
CREATE INDEX idx_payments_customer_public_id ON payments(customer_public_id);
CREATE UNIQUE INDEX uk_wallets_customer_public_id ON wallets(customer_public_id);
