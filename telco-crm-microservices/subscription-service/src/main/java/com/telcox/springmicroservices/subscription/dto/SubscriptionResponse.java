package com.telcox.springmicroservices.subscription.dto;

import java.time.Instant;
import java.util.UUID;

import com.telcox.springmicroservices.subscription.entity.SubscriptionStatus;

/**
 * Abonelik detaylarını dönen yanıt nesnesi.
 */
public record SubscriptionResponse(
        UUID id,
        UUID customerId,
        String msisdn,
        String tariffCode,
        UUID tariffId,
        SubscriptionStatus status,
        Instant activatedAt,
        Instant terminatedAt,
        Instant createdAt
) {}
