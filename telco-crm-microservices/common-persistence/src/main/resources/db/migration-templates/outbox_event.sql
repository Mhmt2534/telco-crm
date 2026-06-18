-- TEMPLATE (not auto-applied). Copy into your service's src/main/resources/db/migration/
-- as a versioned Flyway script (e.g. V2__create_outbox_event.sql) if the service publishes events.
-- Backs com.telcocrm.common.persistence.outbox.OutboxEvent.

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

-- Publisher worker scans pending rows oldest-first.
CREATE INDEX idx_outbox_event_status_created_at ON outbox_event (status, created_at);
