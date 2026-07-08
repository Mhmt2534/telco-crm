package com.telcox.springmicroservices.subscription.dto;

public record SubscriptionActivationFailedEvent(
        Long orderId,
        String reason
) {}
