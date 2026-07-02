package com.telcox.springmicroservices.usage.kafka;

import com.telcox.springmicroservices.usage.dto.CdrRecordedEvent;
import com.telcox.springmicroservices.usage.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CdrUsageConsumer {

    private static final Logger log = LoggerFactory.getLogger(CdrUsageConsumer.class);

    private final QuotaService quotaService;

    public CdrUsageConsumer(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @KafkaListener(
            topics = "telco.usage.events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "cdrKafkaListenerContainerFactory"
    )
    public void consume(CdrRecordedEvent event) {
        log.info("Received CdrRecordedEvent for MSISDN: {} with CDR Ref: {}", event.getMsisdn(), event.getCdrRef());
        
        try {
            quotaService.processCdrEvent(event);
            log.info("Successfully processed CDR Ref: {}", event.getCdrRef());
        } catch (Exception e) {
            log.error("Failed to process CDR Ref: {} for MSISDN: {}", event.getCdrRef(), event.getMsisdn(), e);
            // İleride Dead Letter Queue (DLQ) veya Retry mantığı eklenebilir.
        }
    }
}
