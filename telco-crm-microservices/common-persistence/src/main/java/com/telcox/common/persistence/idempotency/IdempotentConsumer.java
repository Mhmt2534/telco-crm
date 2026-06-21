package com.telcox.common.persistence.idempotency;

import java.util.UUID;

/**
 * Helper contract for idempotent Kafka consumption: check whether an event was already handled
 * by this consumer, and mark it handled once processing succeeds (inside the same transaction).
 *
 * <p>A DB-backed default implementation can be provided by a service using
 * {@link ProcessedEventRepository}; the wiring is left to the messaging architecture decision.</p>
 */
public interface IdempotentConsumer {

    boolean isAlreadyProcessed(UUID eventId, String consumer);

    void markProcessed(UUID eventId, String consumer);
}
