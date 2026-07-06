package com.telcox.springmicroservices.payment.repository;

import com.telcox.springmicroservices.payment.domain.entity.Payment;
import com.telcox.springmicroservices.payment.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPaymentRequestId(String paymentRequestId);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
