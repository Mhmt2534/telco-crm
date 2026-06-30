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

CREATE TABLE quotas (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL UNIQUE,
    minutes_remaining INT NOT NULL,
    sms_remaining INT NOT NULL,
    mb_remaining BIGINT NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE usage_records (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    type VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Performance Indexes
CREATE INDEX idx_quotas_subscription ON quotas(subscription_id);
CREATE INDEX idx_usage_records_subscription ON usage_records(subscription_id);
CREATE INDEX idx_usage_records_msisdn ON usage_records(msisdn);
CREATE INDEX idx_usage_records_recorded_at ON usage_records(recorded_at);
