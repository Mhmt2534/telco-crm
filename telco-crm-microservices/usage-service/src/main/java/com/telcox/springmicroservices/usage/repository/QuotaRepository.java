package com.telcox.springmicroservices.usage.repository;

import java.util.Optional;
import java.util.UUID;

import com.telcox.springmicroservices.usage.entity.Quota;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuotaRepository extends JpaRepository<Quota, UUID> {
    Optional<Quota> findBySubscriptionId(UUID subscriptionId);
}
