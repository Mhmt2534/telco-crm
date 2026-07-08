package com.telcox.springmicroservices.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import com.telcox.springmicroservices.orderservice.domain.entity.OutboxEvent;
import com.telcox.springmicroservices.orderservice.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publishOrderCreated(Order order) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", order.getId());
            payload.put("customerId", order.getCustomerId());
            payload.put("totalAmount", order.getTotalAmount());
            payload.put("currency", order.getCurrency());
            
            if (order.getItems() != null) {
                payload.put("items", order.getItems().stream().map(item -> {
                    Map<String, Object> itemMap = new LinkedHashMap<>();
                    itemMap.put("productCode", item.getProductCode());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("unitPrice", item.getUnitPrice());
                    return itemMap;
                }).collect(Collectors.toList()));
            }

            payload.put("occurredAt", Instant.now().toString());

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .eventType("OrderCreated")
                    .payload(objectMapper.valueToTree(payload))
                    .build();

            outboxEventRepository.save(event);
            log.info("OrderCreated event published to outbox for order ID: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to serialize OrderCreated event payload", e);
            throw new RuntimeException("Error processing JSON for outbox event", e);
        }
    }

    @Transactional
    public void publishOrderConfirmed(Order order) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", order.getId());
            payload.put("newStatus", "PAID");
            payload.put("occurredAt", Instant.now().toString());

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .eventType("OrderConfirmed")
                    .payload(objectMapper.valueToTree(payload))
                    .build();

            outboxEventRepository.save(event);
            log.info("OrderConfirmed event published to outbox for order ID: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to serialize OrderConfirmed event payload", e);
            throw new RuntimeException("Error processing JSON for outbox event", e);
        }
    }

    @Transactional
    public void publishPaymentRefundRequested(Long orderId, String paymentId, java.math.BigDecimal amount) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", orderId);
            payload.put("paymentId", paymentId);
            payload.put("amount", amount);
            payload.put("occurredAt", Instant.now().toString());

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(orderId.toString())
                    .eventType("PaymentRefundRequested")
                    .payload(objectMapper.valueToTree(payload))
                    .build();

            outboxEventRepository.save(event);
            log.info("PaymentRefundRequested event published to outbox for order ID: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to serialize PaymentRefundRequested event payload", e);
            throw new RuntimeException("Error processing JSON for outbox event", e);
        }
    }
}
