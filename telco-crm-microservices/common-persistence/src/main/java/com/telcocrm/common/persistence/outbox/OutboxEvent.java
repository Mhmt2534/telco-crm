package com.telcocrm.common.persistence.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transactional Outbox record. Persist this in the SAME transaction as the domain change; a
 * separate publisher worker later reads PENDING rows and ships them to the broker.
 *
 * <p>OPT-IN: to use it, the owning service must include this package in its entity/repository
 * scan and add the matching Flyway migration (see {@code db/migration-templates/}).</p>
 */
@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Logical event id (matches the envelope's eventId) for downstream idempotency. */
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    private OutboxEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.aggregateType = builder.aggregateType;
        this.aggregateId = builder.aggregateId;
        this.eventType = builder.eventType;
        this.payload = builder.payload;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Minimal builder for the fields a producer supplies; the rest default at persist time. */
    public static final class Builder {
        private UUID eventId;
        private String aggregateType;
        private String aggregateId;
        private String eventType;
        private String payload;

        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public OutboxEvent build() {
            return new OutboxEvent(this);
        }
    }
}
