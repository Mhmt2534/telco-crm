package com.telcocrm.common.persistence.outbox;

/**
 * Contract for the worker that drains PENDING {@link OutboxEvent} rows and publishes them to the
 * message broker, then marks them PUBLISHED.
 *
 * <p>The concrete implementation (Kafka producer, polling/CDC strategy, batching, retry/backoff)
 * is intentionally left to the messaging architecture decision — only the contract is fixed here.</p>
 */
public interface OutboxPublisher {

    /** Publishes a batch of pending outbox events. Returns the number successfully published. */
    int publishPending();
}
