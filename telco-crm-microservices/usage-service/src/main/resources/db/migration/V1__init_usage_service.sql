-- ─────────────────────────────────────────────────────────────
-- usage-service  –  V1 Initial Schema
-- Tables: outbox_event, quotas, usage_records
-- ─────────────────────────────────────────────────────────────

-- Outbox table for CDC / Debezium (QuotaWarning80Event, QuotaExceeded100Event vb.)
CREATE TABLE outbox_event (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(255)             NOT NULL,
    aggregate_id    VARCHAR(255)             NOT NULL,
    event_type      VARCHAR(255)             NOT NULL,
    payload         JSONB                    NOT NULL,
    is_processed    BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at    TIMESTAMP WITH TIME ZONE
);

-- Quota: her aboneliğin aktif dönem kota sayacı
-- total_* alanları: %80/%100 eşik hesabı için başlangıç değerleri
CREATE TABLE quotas (
    id                  UUID    PRIMARY KEY,
    subscription_id     UUID    NOT NULL UNIQUE,

    -- Dönem bilgisi
    period_start        TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end          TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Başlangıç (toplam) haklar  →  eşik hesabı için
    total_minutes       INT     NOT NULL,
    total_sms           INT     NOT NULL,
    total_mb            BIGINT  NOT NULL,

    -- Anlık kalan haklar
    minutes_remaining   INT     NOT NULL,
    sms_remaining       INT     NOT NULL,
    mb_remaining        BIGINT  NOT NULL,

    -- Audit
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0
);

-- UsageRecord: Kafka'dan gelen ham CDR kayıtlarının logu
-- cdr_ref: Kafka mesajındaki benzersiz CDR referansı (idempotency için)
CREATE TABLE usage_records (
    id              UUID    PRIMARY KEY,
    subscription_id UUID    NOT NULL,
    msisdn          VARCHAR(20)              NOT NULL,   -- Partition key, sorgu kolaylığı için tutulur
    type            VARCHAR(20)              NOT NULL,   -- VOICE | SMS | DATA
    quantity        DOUBLE PRECISION         NOT NULL,   -- Tüketilen miktar
    cdr_ref         VARCHAR(100)             UNIQUE,     -- Kafka CDR referansı (idempotency)
    recorded_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

-- ─── İndeksler ────────────────────────────────────────────────
CREATE INDEX idx_quotas_subscription         ON quotas(subscription_id);
CREATE INDEX idx_usage_records_subscription  ON usage_records(subscription_id);
CREATE INDEX idx_usage_records_msisdn        ON usage_records(msisdn);
CREATE INDEX idx_usage_records_recorded_at   ON usage_records(recorded_at);
CREATE INDEX idx_outbox_event_is_processed   ON outbox_event(is_processed);
