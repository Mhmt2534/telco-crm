package com.telcox.springmicroservices.productcatalog.repository;

import com.telcox.springmicroservices.productcatalog.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
}
