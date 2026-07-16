package com.telcox.springmicroservices.subscription.dto;

import java.util.UUID;

public record SubscriptionActivationFailedEvent(
        UUID orderId,
        String reason
) {}
