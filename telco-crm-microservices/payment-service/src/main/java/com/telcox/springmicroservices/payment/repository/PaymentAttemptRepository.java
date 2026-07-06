package com.telcox.springmicroservices.payment.repository;

import com.telcox.springmicroservices.payment.domain.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
}
