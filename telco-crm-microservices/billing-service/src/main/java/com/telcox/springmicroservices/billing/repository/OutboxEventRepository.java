package com.telcox.springmicroservices.billing.repository;

import com.telcox.springmicroservices.billing.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
