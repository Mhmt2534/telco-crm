package com.telcox.springmicroservices.payment.repository;

import com.telcox.springmicroservices.payment.domain.entity.Payment;
import com.telcox.springmicroservices.payment.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p LEFT JOIN FETCH p.attempts WHERE p.status = 'PENDING' AND SIZE(p.attempts) <= :maxAttempts")
    List<Payment> findFailedPaymentsEligibleForRetry(@org.springframework.data.repository.query.Param("maxAttempts") int maxAttempts);
}
