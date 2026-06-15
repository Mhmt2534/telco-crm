package com.telcocrm.common.persistence.idempotency;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository over the processed-events table. OPT-IN: only active when the owning service
 * includes this package in {@code @EnableJpaRepositories} / {@code @EntityScan}.
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventIdAndConsumer(UUID eventId, String consumer);
}
