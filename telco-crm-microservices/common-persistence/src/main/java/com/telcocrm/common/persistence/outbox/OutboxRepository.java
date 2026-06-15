package com.telcocrm.common.persistence.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository over the outbox table. OPT-IN: only active when the owning service includes this
 * package in {@code @EnableJpaRepositories} / {@code @EntityScan}.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Limit limit);
}
