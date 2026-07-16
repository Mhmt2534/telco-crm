package com.telcox.springmicroservices.subscription.dto;

import java.util.UUID;

public record SubscriptionActivatedEvent(
        UUID orderId,
        UUID subscriptionId,
        UUID customerId,
        String msisdn,
        String status
) {}
