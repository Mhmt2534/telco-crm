CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE msisdn_pool (
    msisdn VARCHAR(20) PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'FREE',
    reserved_until TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    tariff_code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    activated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    terminated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_subscription_msisdn FOREIGN KEY (msisdn) REFERENCES msisdn_pool(msisdn)
);

CREATE TABLE sim_cards (
    iccid VARCHAR(30) PRIMARY KEY,
    imsi VARCHAR(30) NOT NULL UNIQUE,
    msisdn VARCHAR(20) UNIQUE,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_sim_card_msisdn FOREIGN KEY (msisdn) REFERENCES msisdn_pool(msisdn)
);

-- Indexing for performance
CREATE INDEX idx_msisdn_pool_status ON msisdn_pool(status);
CREATE INDEX idx_subscriptions_customer ON subscriptions(customer_id);
CREATE INDEX idx_subscriptions_msisdn ON subscriptions(msisdn);
