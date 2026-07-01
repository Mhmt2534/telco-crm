package com.telcox.springmicroservices.usage.repository;

import com.telcox.springmicroservices.usage.entity.Quota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuotaRepository extends JpaRepository<Quota, UUID> {

    Optional<Quota> findBySubscriptionId(UUID subscriptionId);
}
