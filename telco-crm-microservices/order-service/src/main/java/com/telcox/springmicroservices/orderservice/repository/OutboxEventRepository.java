package com.telcox.springmicroservices.orderservice.repository;

import com.telcox.springmicroservices.orderservice.domain.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
}
