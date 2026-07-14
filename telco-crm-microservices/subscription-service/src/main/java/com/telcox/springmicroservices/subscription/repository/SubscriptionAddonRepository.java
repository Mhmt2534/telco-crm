package com.telcox.springmicroservices.subscription.repository;

import com.telcox.springmicroservices.subscription.entity.SubscriptionAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface SubscriptionAddonRepository extends JpaRepository<SubscriptionAddon, UUID> {
    List<SubscriptionAddon> findBySubscriptionId(UUID subscriptionId);
}
