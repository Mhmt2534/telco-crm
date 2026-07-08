package com.telcox.springmicroservices.billing.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.billing.dto.SubscriptionActivatedEvent;
import com.telcox.springmicroservices.billing.entity.BillCycle;
import com.telcox.springmicroservices.billing.repository.BillCycleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class SubscriptionActivatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionActivatedConsumer.class);

    private final BillCycleRepository billCycleRepository;
    private final ObjectMapper objectMapper;

    public SubscriptionActivatedConsumer(BillCycleRepository billCycleRepository, ObjectMapper objectMapper) {
        this.billCycleRepository = billCycleRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telcox.Subscription.events", groupId = "billing-service-group")
    public void consume(String message) {
        log.info("Received raw Subscription event: {}", message);
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

            String eventType = "";
            if (afterNode.has("event_type")) {
                eventType = afterNode.get("event_type").asText();
            } else if (afterNode.has("eventType")) {
                eventType = afterNode.get("eventType").asText();
            } else if (root.has("eventType")) {
                eventType = root.get("eventType").asText();
            }

            // Fallback
            if (eventType.isEmpty()) {
                if (afterNode.has("tariffCode")) {
                    eventType = "SubscriptionActivated";
                }
            }

            if (!"SubscriptionActivated".equals(eventType)) {
                log.debug("Skipping event type: {}", eventType);
                return;
            }

            String payloadStr = "";
            if (afterNode.has("payload")) {
                JsonNode innerPayload = afterNode.get("payload");
                payloadStr = innerPayload.isTextual() ? innerPayload.asText() : innerPayload.toString();
            } else {
                payloadStr = afterNode.toString();
            }

            SubscriptionActivatedEvent event = objectMapper.readValue(payloadStr, SubscriptionActivatedEvent.class);
            log.info("Processing SubscriptionActivated for subscription: {}", event.getId());

            // 1. Fatura kesim gununu belirle (Bugunun gunu olsun)
            int cutOffDay = LocalDate.now().getDayOfMonth();

            // 2. Sabit ucret (Mock olarak 100 TL atiyoruz)
            BigDecimal fixedAmount = new BigDecimal("100.00");

            // 3. BillCycle kaydi olustur
            BillCycle billCycle = new BillCycle();
            billCycle.setCustomerId(event.getCustomerId());
            billCycle.setSubscriptionId(event.getId());
            billCycle.setMsisdn(event.getMsisdn());
            billCycle.setCutOffDay(cutOffDay);
            billCycle.setFixedAmount(fixedAmount);

            billCycleRepository.save(billCycle);

            log.info("BillCycle successfully created for subscriptionId: {}, cutOffDay: {}", event.getId(), cutOffDay);

        } catch (Exception ex) {
            log.error("Failed to process SubscriptionActivated Kafka message: {}", message, ex);
        }
    }
}
