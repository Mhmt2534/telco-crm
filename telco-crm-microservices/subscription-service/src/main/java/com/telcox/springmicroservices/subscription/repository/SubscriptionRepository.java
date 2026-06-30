package com.telcox.springmicroservices.subscription.repository;

import java.util.UUID;

import com.telcox.springmicroservices.subscription.entity.Subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
}
