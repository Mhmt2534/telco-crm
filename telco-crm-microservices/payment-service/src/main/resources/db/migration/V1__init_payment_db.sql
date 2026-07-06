-- Flyway Database Migration V1: Initialize Payment DB
-- Every table mapping an entity extending BaseEntity includes standard audit columns.

CREATE TABLE payments (
    id                 UUID PRIMARY KEY,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255),
    version            BIGINT NOT NULL,
    invoice_id         VARCHAR(100),
    order_id           BIGINT,
    amount             NUMERIC(19, 2) NOT NULL,
    method             VARCHAR(50) NOT NULL,
    status             VARCHAR(50) NOT NULL,
    external_ref       VARCHAR(100),
    payment_request_id VARCHAR(100) UNIQUE,
    paid_at            TIMESTAMPTZ
);

CREATE TABLE payment_attempts (
    id           UUID PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),
    version      BIGINT NOT NULL,
    payment_id   UUID NOT NULL,
    attempt_no   INTEGER NOT NULL,
    response     TEXT,
    CONSTRAINT fk_payment_attempts_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

CREATE TABLE outbox_event (
    id             UUID PRIMARY KEY,
    event_id       UUID        NOT NULL UNIQUE,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    payload        TEXT        NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count    INTEGER     NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_event_status_created_at ON outbox_event (status, created_at);

CREATE TABLE audit_log (
    id          UUID PRIMARY KEY,
    entity_name VARCHAR(100) NOT NULL,
    entity_id   VARCHAR(100) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    old_state   TEXT,
    new_state   TEXT,
    changed_by  VARCHAR(100),
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
