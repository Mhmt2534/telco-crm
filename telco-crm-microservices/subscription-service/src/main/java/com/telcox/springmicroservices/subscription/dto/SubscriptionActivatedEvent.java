package com.telcox.springmicroservices.subscription.dto;

import java.util.UUID;

public record SubscriptionActivatedEvent(
        Long orderId,
        UUID subscriptionId,
        String msisdn,
        String status
) {}
