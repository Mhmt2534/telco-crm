package com.telcox.springmicroservices.notification.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuotaThresholdReachedEvent {
    private UUID subscriptionId;
    private String msisdn;
    private String usageType; // VOICE, SMS, DATA
    private String limitType; // 80_PERCENT
    private String thresholdReachedAt;
}
