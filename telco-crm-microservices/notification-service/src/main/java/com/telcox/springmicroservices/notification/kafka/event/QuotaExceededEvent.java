package com.telcox.springmicroservices.notification.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuotaExceededEvent {
    private UUID subscriptionId;
    private String msisdn;
    private String usageType; // VOICE, SMS, DATA
    private String limitType; // 100_PERCENT
    private String exceededAt;
}
