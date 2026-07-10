package com.telcox.springmicroservices.notification.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionActivatedEvent {
    private UUID subscriptionId;
    private UUID customerId;
    private String msisdn;
    private String customerName;
    private String email;
    private String tariffName;
}
