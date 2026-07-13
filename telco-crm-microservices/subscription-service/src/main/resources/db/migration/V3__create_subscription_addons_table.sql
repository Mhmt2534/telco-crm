CREATE TABLE subscription_addons (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    addon_code VARCHAR(100) NOT NULL,
    activated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_subscription_addon FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);
