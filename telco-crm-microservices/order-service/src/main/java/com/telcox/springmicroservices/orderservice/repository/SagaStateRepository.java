package com.telcox.springmicroservices.orderservice.repository;

import com.telcox.springmicroservices.orderservice.domain.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, Long> {
    Optional<SagaState> findBySagaId(String sagaId);
    Optional<SagaState> findByOrderId(Long orderId);
}
