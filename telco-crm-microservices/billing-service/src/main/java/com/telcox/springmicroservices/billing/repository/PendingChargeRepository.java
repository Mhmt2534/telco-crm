package com.telcox.springmicroservices.billing.repository;

import com.telcox.springmicroservices.billing.entity.PendingCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PendingChargeRepository extends JpaRepository<PendingCharge, Long> {
    boolean existsByOrderId(UUID orderId);
    List<PendingCharge> findBySubscriptionIdAndStatus(UUID subscriptionId, String status);
}
