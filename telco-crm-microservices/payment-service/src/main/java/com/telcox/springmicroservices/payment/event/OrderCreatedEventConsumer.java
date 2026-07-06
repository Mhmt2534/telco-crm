package com.telcox.springmicroservices.payment.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.payment.dto.OrderCreatedEvent;
import com.telcox.springmicroservices.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public OrderCreatedEventConsumer(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telcox.Order.events", groupId = "payment-service-group")
    public void consume(String message) {
        log.info("Received raw event message from telcox.Order.events: {}", message);
        
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

        // 2. First attempt: Parse directly as OrderCreatedEvent DTO
        try {
            OrderCreatedEvent event = objectMapper.readValue(jsonPayload, OrderCreatedEvent.class);
            if (event.getOrderId() != null) {
                paymentService.processPayment(event);
                return;
            }
        } catch (Exception e) {
            log.warn("Direct DTO parsing failed, trying Debezium schema envelope parsing fallback...");
        }

        // 3. Fallback: Parse Debezium envelope structured payload
        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            
            // Navigate through potential Debezium JSON envelope structures
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
    }
}
