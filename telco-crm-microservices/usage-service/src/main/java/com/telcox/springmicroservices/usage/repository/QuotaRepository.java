package com.telcox.springmicroservices.usage.repository;

import com.telcox.springmicroservices.usage.entity.Quota;
import org.springframework.data.jpa.repository.JpaRepository;

import com.telcox.springmicroservices.usage.entity.QuotaStatus;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface QuotaRepository extends JpaRepository<Quota, UUID> {

    Optional<Quota> findBySubscriptionId(UUID subscriptionId);
    
    Optional<Quota> findBySubscriptionIdAndStatus(UUID subscriptionId, QuotaStatus status);
    
    List<Quota> findByStatusAndPeriodStartLessThanEqual(QuotaStatus status, java.time.OffsetDateTime date);
}
