ALTER TABLE subscriptions ADD COLUMN customer_public_id UUID;
ALTER TABLE subscriptions ADD COLUMN tariff_public_id UUID;
ALTER TABLE subscriptions ALTER COLUMN customer_id DROP NOT NULL;

CREATE INDEX idx_subscriptions_customer_public_id ON subscriptions(customer_public_id);
CREATE INDEX idx_subscriptions_tariff_public_id ON subscriptions(tariff_public_id);
