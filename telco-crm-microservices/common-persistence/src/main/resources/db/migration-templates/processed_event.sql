-- TEMPLATE (not auto-applied). Copy into your service's src/main/resources/db/migration/
-- as a versioned Flyway script if the service consumes events idempotently.
-- Backs com.telcocrm.common.persistence.idempotency.ProcessedEvent.

CREATE TABLE processed_event (
    id           UUID PRIMARY KEY,
    event_id     UUID         NOT NULL,
    consumer     VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_processed_event UNIQUE (event_id, consumer)
);
