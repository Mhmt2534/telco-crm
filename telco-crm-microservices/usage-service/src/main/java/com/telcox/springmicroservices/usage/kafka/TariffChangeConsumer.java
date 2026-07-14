package com.telcox.springmicroservices.usage.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.usage.dto.TariffChangeRequestedEvent;
import com.telcox.springmicroservices.usage.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TariffChangeConsumer {

    private static final Logger log = LoggerFactory.getLogger(TariffChangeConsumer.class);

    private final QuotaService quotaService;
    private final ObjectMapper objectMapper;

    public TariffChangeConsumer(QuotaService quotaService, ObjectMapper objectMapper) {
        this.quotaService = quotaService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telcox.Subscription.events", groupId = "usage-service-tariff-change-group", containerFactory = "stringKafkaListenerContainerFactory")
    public void consumeTariffChangeEvents(String message,
            @org.springframework.messaging.handler.annotation.Header(name = "eventType", required = false) String eventTypeHeader) {
        log.info("Received raw event message from telcox.Subscription.events in usage-service: {} | Header eventType: {}", message, eventTypeHeader);
        String jsonPayload = message;

        try {
            JsonNode parsedNode = objectMapper.readTree(message);
            if (parsedNode.isTextual()) {
                jsonPayload = parsedNode.asText();
            }
        } catch (Exception e) {
            log.debug("Message is not a JSON TextNode, treating as raw JSON.");
        }

        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            JsonNode payloadNode = root.has("payload") ? root.get("payload") : root;
            JsonNode afterNode = payloadNode.has("after") ? payloadNode.get("after") : payloadNode;

            String eventType = eventTypeHeader != null ? eventTypeHeader : "";

            if (eventType.isEmpty()) {
                if (afterNode.has("event_type")) {
                    eventType = afterNode.get("event_type").asText();
                } else if (afterNode.has("oldTariffCode") && afterNode.has("newTariffCode")) {
                    eventType = "TariffChangeRequested";
                }
            }

            if ("TariffChangeRequested".equals(eventType)) {
                String payloadStr = "";
                if (afterNode.has("payload")) {
                    JsonNode innerPayload = afterNode.get("payload");
                    payloadStr = innerPayload.isTextual() ? innerPayload.asText() : innerPayload.toString();
                } else {
                    payloadStr = afterNode.toString();
                }

                TariffChangeRequestedEvent event = objectMapper.readValue(payloadStr, TariffChangeRequestedEvent.class);
                log.info("Processing TariffChangeRequested for subscription: {}", event.getSubscriptionId());
                quotaService.processTariffChange(event);
            } else {
                log.info("Ignoring event type outside usage-service's interest (eventType={})", eventType);
            }

        } catch (Exception ex) {
            log.error("Failed to process telcox.Subscription.events Kafka message in usage-service: {}", message, ex);
            throw new RuntimeException("Failed to process event: " + message, ex); // Throw exception to trigger DLQ
        }
    }
}
