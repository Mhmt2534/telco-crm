package com.telcox.springmicroservices.billing.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.billing.dto.TariffChangeRequestedEvent;
import com.telcox.springmicroservices.billing.entity.PendingCharge;
import com.telcox.springmicroservices.billing.repository.PendingChargeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class TariffChangeConsumer {

    private static final Logger log = LoggerFactory.getLogger(TariffChangeConsumer.class);

    private final PendingChargeRepository pendingChargeRepository;
    private final ObjectMapper objectMapper;

    public TariffChangeConsumer(PendingChargeRepository pendingChargeRepository, ObjectMapper objectMapper) {
        this.pendingChargeRepository = pendingChargeRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telcox.Subscription.events", groupId = "billing-service-tariff-change-group")
    @Transactional
    public void consume(String message, @Header(name = "eventType", required = false) String eventTypeHeader) {
        log.info("Received TariffChange event: {}, eventType: {}", message, eventTypeHeader);
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

            if (!"TariffChangeRequested".equals(eventType)) {
                log.debug("Ignoring event type: {}", eventType);
                return;
            }

            String payloadStr = "";
            if (afterNode.has("payload")) {
                JsonNode innerPayload = afterNode.get("payload");
                payloadStr = innerPayload.isTextual() ? innerPayload.asText() : innerPayload.toString();
            } else {
                payloadStr = afterNode.toString();
            }

            TariffChangeRequestedEvent event = objectMapper.readValue(payloadStr, TariffChangeRequestedEvent.class);
            log.info("Processing TariffChangeRequested for subscription: {}, orderId: {}", event.getSubscriptionId(), event.getOrderId());

            // Idempotency check
            if (pendingChargeRepository.existsByOrderId(event.getOrderId())) {
                log.info("TariffChangeRequested event with orderId {} already processed (Idempotent). Skipping.", event.getOrderId());
                return;
            }

            PendingCharge pendingCharge = new PendingCharge();
            pendingCharge.setOrderId(event.getOrderId());
            pendingCharge.setSubscriptionId(event.getSubscriptionId());
            pendingCharge.setCustomerId(event.getCustomerId());
            pendingCharge.setDescription("Tariff Change: " + event.getOldTariffCode() + " -> " + event.getNewTariffCode());
            pendingCharge.setAmount(event.getPriceDiff() != null ? event.getPriceDiff() : BigDecimal.ZERO);
            pendingCharge.setEffectiveBillCycle(event.getEffectiveBillCycle());

            if ("NEXT_CYCLE".equals(event.getEffectiveBillCycle())) {
                pendingCharge.setStatus("PENDING_NEXT");
            } else {
                pendingCharge.setStatus("PENDING");
            }

            pendingChargeRepository.save(pendingCharge);
            log.info("PendingCharge saved successfully for subscriptionId: {}, orderId: {}, status: {}", 
                     event.getSubscriptionId(), event.getOrderId(), pendingCharge.getStatus());

        } catch (Exception ex) {
            log.error("Failed to process TariffChangeRequested Kafka message: {}", message, ex);
        }
    }
}
