package com.telcox.springmicroservices.payment.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.payment.dto.OrderCreatedEvent;
import com.telcox.springmicroservices.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.telcox.springmicroservices.payment.dto.PaymentRefundRequestedEvent;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

@Component
public class OrderEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsConsumer.class);

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public OrderEventsConsumer(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telcox.Order.events", groupId = "payment-service-group")
    public void consume(String message, @Header(name = "eventType", required = false) String headerEventType) {
        log.info("Received raw event message from telcox.Order.events: {}, eventType header: {}", message, headerEventType);
        
        String jsonPayload = message;

        // 1. Double-serialization protection: Unescape if the outer wrapper is a JSON string
        try {
            JsonNode parsedNode = objectMapper.readTree(message);
            if (parsedNode.isTextual()) {
                jsonPayload = parsedNode.asText();
                log.info("Unescaped double-serialized JSON payload: {}", jsonPayload);
            }
        } catch (Exception e) {
            log.debug("Message is not a JSON TextNode, treating as raw JSON.");
        }

        // Try to extract event type from payload if header is missing
        String eventType = headerEventType;
        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            if (eventType == null && root.has("eventType")) {
                eventType = root.get("eventType").asText();
            }
            // Add a heuristic based on fields if eventType is still null
            if (eventType == null) {
                if (root.has("paymentId") && root.has("amount") && !root.has("customerId")) {
                    eventType = "PaymentRefundRequested";
                } else if (root.has("customerId") && root.has("totalAmount")) {
                    eventType = "OrderCreated";
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse eventType from payload");
        }

        log.info("Determined eventType: {}", eventType);

        if ("PaymentRefundRequested".equals(eventType)) {
            try {
                PaymentRefundRequestedEvent event = objectMapper.readValue(jsonPayload, PaymentRefundRequestedEvent.class);
                if (event.getPaymentId() != null) {
                    paymentService.refundPayment(event);
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to parse PaymentRefundRequestedEvent", e);
            }
        } else if ("OrderCreated".equals(eventType)) {
            // OrderCreated logic
            try {
                OrderCreatedEvent event = objectMapper.readValue(jsonPayload, OrderCreatedEvent.class);
                if (event.getOrderId() != null && event.getCustomerId() != null) {
                    paymentService.processPayment(event);
                    return;
                }
            } catch (Exception e) {
                log.warn("Direct DTO parsing failed for OrderCreatedEvent, trying Debezium schema envelope parsing fallback...");
            }

            // Fallback: Parse Debezium envelope structured payload
            try {
                JsonNode root = objectMapper.readTree(jsonPayload);
                JsonNode payloadNode = root.has("payload") ? root.get("payload") : root;
                JsonNode afterNode = payloadNode.has("after") ? payloadNode.get("after") : payloadNode;
                
                if (afterNode.has("payload")) {
                    JsonNode innerPayload = afterNode.get("payload");
                    String payloadStr = innerPayload.isTextual() ? innerPayload.asText() : innerPayload.toString();
                    
                    OrderCreatedEvent event = objectMapper.readValue(payloadStr, OrderCreatedEvent.class);
                    paymentService.processPayment(event);
                    log.info("Successfully parsed event via Debezium fallback router path.");
                } else {
                    log.error("Could not locate payload field in unescaped message: {}", jsonPayload);
                }
            } catch (Exception ex) {
                log.error("All parsing attempts failed for Kafka message: {}", message, ex);
            }
        } else {
            log.info("Ignoring event type outside payment-service's interest (eventType={}), raw payload: {}", eventType, jsonPayload);
        }
    }
}
