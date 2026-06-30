package com.telcox.springmicroservices.subscription.dto;

import java.time.OffsetDateTime;
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
        SubscriptionStatus status,
        OffsetDateTime activatedAt,
        OffsetDateTime terminatedAt,
        OffsetDateTime createdAt
) {}
