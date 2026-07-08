package com.telcox.springmicroservices.orderservice.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import com.telcox.springmicroservices.orderservice.domain.entity.SagaState;
import com.telcox.springmicroservices.orderservice.domain.enums.OrderStatus;
import com.telcox.springmicroservices.orderservice.domain.enums.SagaStatus;
import com.telcox.springmicroservices.orderservice.dto.PaymentCompletedPayload;
import com.telcox.springmicroservices.orderservice.dto.PaymentRefundedPayload;
import com.telcox.springmicroservices.orderservice.repository.OrderRepository;
import com.telcox.springmicroservices.orderservice.repository.SagaStateRepository;
import com.telcox.springmicroservices.orderservice.service.OutboxEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventsConsumer {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.payment-events}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(String message, @Header(name = "eventType", required = false) String headerEventType) {
        log.info("Received raw event message from payment-events: {}, eventType header: {}", message, headerEventType);

        String jsonPayload = message;

        // Double-serialization protection
        try {
            JsonNode parsedNode = objectMapper.readTree(message);
            if (parsedNode.isTextual()) {
                jsonPayload = parsedNode.asText();
                log.info("Unescaped double-serialized JSON payload: {}", jsonPayload);
            }
        } catch (Exception e) {
            log.debug("Message is not a JSON TextNode, treating as raw JSON.");
        }

        String eventType = headerEventType;
        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            
            // Navigate through potential Debezium JSON envelope structures
            JsonNode payloadNode = root.has("payload") ? root.get("payload") : root;
            JsonNode afterNode = payloadNode.has("after") ? payloadNode.get("after") : payloadNode;

            if (eventType == null && root.has("eventType")) {
                eventType = root.get("eventType").asText();
            }

            // Heuristics if still null
            if (eventType == null) {
                if (afterNode.has("paymentId") && afterNode.has("orderId")) {
                    // Check if it's refunded or completed. 
                    // PaymentRefunded Payload and PaymentCompleted Payload both have paymentId and orderId now.
                    // But PaymentCompleted doesn't have amount in the order-service side, though it has it on payment-service side.
                    // To be safe, rely on the Debezium outbox table routing which usually includes event_type in headers.
                    log.warn("Could not determine eventType from header or payload root, attempting heuristic routing.");
                }
            }

            String targetPayloadStr = afterNode.isTextual() ? afterNode.asText() : afterNode.toString();

            if ("PaymentRefunded".equals(eventType)) {
                handlePaymentRefunded(targetPayloadStr);
            } else if ("PaymentCompleted".equals(eventType) || eventType == null) {
                // If eventType is null, default to PaymentCompleted for backward compatibility
                handlePaymentCompleted(targetPayloadStr);
            } else {
                log.warn("Ignored unknown eventType: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing message in PaymentEventsConsumer", e);
        }
    }

    private void handlePaymentCompleted(String payloadStr) {
        try {
            PaymentCompletedPayload payload = objectMapper.readValue(payloadStr, PaymentCompletedPayload.class);
            Long orderId = payload.getOrderId();

            if (orderId == null) {
                log.warn("Ignored PaymentCompleted message: orderId is null");
                return;
            }

            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.warn("Ignored PaymentCompleted message: Order not found with ID {}", orderId);
                return;
            }

            Order order = orderOpt.get();

            // Idempotency check: Ignore if status is already PAID, FULFILLED or CANCELLED
            if (order.getStatus().ordinal() >= OrderStatus.PAID.ordinal()) {
                log.info("Ignored PaymentCompleted message for order ID {}: Order status is already {}", orderId, order.getStatus());
                return;
            }

            Optional<SagaState> sagaOpt = sagaStateRepository.findByOrderId(orderId);
            if (sagaOpt.isPresent()) {
                SagaState sagaState = sagaOpt.get();
                sagaState.setCurrentStep("STEP_2");
                sagaState.setStatus(SagaStatus.IN_PROGRESS);

                ObjectNode payloadNode;
                if (sagaState.getPayload() != null && sagaState.getPayload().isObject()) {
                    payloadNode = (ObjectNode) sagaState.getPayload();
                } else {
                    payloadNode = objectMapper.createObjectNode();
                }
                if (payload.getPaymentId() != null) {
                    payloadNode.put("paymentId", payload.getPaymentId().toString());
                }
                sagaState.setPayload(payloadNode);

                sagaStateRepository.save(sagaState);
            }

            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            outboxEventPublisher.publishOrderConfirmed(order);
            log.info("Processed PaymentCompleted message for order ID {}: status updated to PAID", orderId);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse PaymentCompleted message", e);
        }
    }

    private void handlePaymentRefunded(String payloadStr) {
        try {
            PaymentRefundedPayload payload = objectMapper.readValue(payloadStr, PaymentRefundedPayload.class);
            Long orderId = payload.getOrderId();

            if (orderId == null) {
                log.warn("Ignored PaymentRefunded message: orderId is null");
                return;
            }

            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.warn("Ignored PaymentRefunded message: Order not found with ID {}", orderId);
                return;
            }

            Order order = orderOpt.get();

            // Idempotency check: Ignore if status is already CANCELLED
            if (order.getStatus() == OrderStatus.CANCELLED) {
                log.info("Ignored PaymentRefunded message for order ID {}: Order status is already CANCELLED", orderId);
                return;
            }

            Optional<SagaState> sagaOpt = sagaStateRepository.findByOrderId(orderId);
            if (sagaOpt.isPresent()) {
                SagaState sagaState = sagaOpt.get();
                
                // Idempotency (Guard Clause)
                if ("COMPENSATED".equals(sagaState.getCurrentStep())) {
                    log.info("Ignored PaymentRefunded message for order ID {}: Saga is already COMPENSATED", orderId);
                    return;
                }
                
                sagaState.setCurrentStep("COMPENSATED");
                sagaStateRepository.save(sagaState);
            }

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            log.info("Processed PaymentRefunded message for order ID {}: status updated to CANCELLED", orderId);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse PaymentRefunded message", e);
        }
    }
}
