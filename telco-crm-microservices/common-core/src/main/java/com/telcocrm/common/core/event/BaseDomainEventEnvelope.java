package com.telcocrm.common.core.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Standard envelope wrapping every domain event published on the bus. The envelope decouples
 * transport/metadata from the business {@code payload} and carries {@code eventVersion} so
 * consumers can evolve schemas safely.
 *
 * <p>Serialization format, topic mapping and the outbox/publisher wiring are an architecture
 * decision; this type only fixes the metadata contract.</p>
 *
 * @param <T> the business payload type
 */
public record BaseDomainEventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        String aggregateType,
        String aggregateId,
        OffsetDateTime occurredAt,
        String correlationId,
        T payload) {

    public static final int CURRENT_VERSION = 1;

    /** Creates a new envelope with a fresh eventId and {@code occurredAt = now (UTC)}. */
    public static <T> BaseDomainEventEnvelope<T> create(
            String eventType,
            String aggregateType,
            String aggregateId,
            String correlationId,
            T payload) {
        return new BaseDomainEventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                CURRENT_VERSION,
                aggregateType,
                aggregateId,
                OffsetDateTime.now(),
                correlationId,
                payload);
    }
}
