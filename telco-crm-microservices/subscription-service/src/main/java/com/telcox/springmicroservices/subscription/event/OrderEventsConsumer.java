package com.telcox.springmicroservices.subscription.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.subscription.dto.CreateSubscriptionRequest;
import com.telcox.springmicroservices.subscription.dto.OrderConfirmedEvent;
import com.telcox.springmicroservices.subscription.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsConsumer.class);

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    public OrderEventsConsumer(SubscriptionService subscriptionService, ObjectMapper objectMapper) {
        this.subscriptionService = subscriptionService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telcox.Order.events", groupId = "subscription-service-group")
    public void consume(String message,
            @org.springframework.messaging.handler.annotation.Header(name = "eventType", required = false) String eventTypeHeader) {
        log.info("Received raw event message from telcox.Order.events: {} | Header eventType: {}", message,
                eventTypeHeader);
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

            // Fallback: Debezium outbox router payload'u sadeleştirdiğinde header yoksa
            // payload içeriğinden çıkarım yap
            if (eventType.isEmpty()) {
                if (afterNode.has("tariffCode")) {
                    eventType = "OrderConfirmed";
                } else if (afterNode.has("event_type")) {
                    eventType = afterNode.get("event_type").asText();
                } else if (afterNode.has("totalAmount")) {
                    eventType = "OrderCreated";
                }
            }

            if ("OrderConfirmed".equals(eventType)) {
                // Process OrderConfirmed below
            } else if (eventType.isEmpty()) {
                log.warn("Could not determine event type for message, skipping: {}", message);
                return;
            } else {
                log.info("Ignoring event type outside subscription-service's interest (eventType={}), raw payload: {}", eventType, jsonPayload);
                return;
            }

            String payloadStr = "";
            if (afterNode.has("payload")) {
                JsonNode innerPayload = afterNode.get("payload");
                payloadStr = innerPayload.isTextual() ? innerPayload.asText() : innerPayload.toString();
            } else {
                payloadStr = afterNode.toString();
            }

            OrderConfirmedEvent event = objectMapper.readValue(payloadStr, OrderConfirmedEvent.class);
            log.info("Processing OrderConfirmed for customer: {}", event.getCustomerId());

            if (event.getTariffCode() == null || event.getTariffCode().isEmpty()) {
                log.warn("OrderConfirmed payload missing tariffCode! Cannot create subscription.");
                return;
            }

            CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                    event.getCustomerId(),
                    event.getTariffCode(),
                    event.getOrderId());
            
            try {
                subscriptionService.createSubscription(request);
                log.info("Successfully created subscription for OrderConfirmed event.");
            } catch (Exception createEx) {
                log.error("Failed to create subscription for OrderId: {}. Publishing ActivationFailed event.", request.orderId(), createEx);
                subscriptionService.publishActivationFailedEvent(request.orderId(), createEx.getMessage());
            }

        } catch (Exception ex) {
            log.error("Failed to process OrderConfirmed Kafka message: {}", message, ex);
        }
    }
}
