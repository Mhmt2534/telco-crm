package com.telcox.springmicroservices.usage.repository;

import com.telcox.springmicroservices.usage.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
