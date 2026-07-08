package com.telcox.springmicroservices.orderservice.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import com.telcox.springmicroservices.orderservice.domain.entity.SagaState;
import com.telcox.springmicroservices.orderservice.domain.enums.OrderStatus;
import com.telcox.springmicroservices.orderservice.domain.enums.SagaStatus;
import com.telcox.springmicroservices.orderservice.dto.SubscriptionActivatedPayload;
import com.telcox.springmicroservices.orderservice.dto.SubscriptionActivationFailedPayload;
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
public class SubscriptionEventsConsumer {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.subscription-events}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(String message, @Header(name = "eventType", required = false) String headerEventType) {
        log.info("Received raw event message from subscription-events: {}, eventType header: {}", message, headerEventType);

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
                if (afterNode.has("reason")) {
                    eventType = "SubscriptionActivationFailed";
                } else if (afterNode.has("orderId")) {
                    eventType = "SubscriptionActivated";
                }
            }

            String targetPayloadStr = afterNode.isTextual() ? afterNode.asText() : afterNode.toString();

            if ("SubscriptionActivationFailed".equals(eventType)) {
                handleSubscriptionActivationFailed(targetPayloadStr);
            } else if ("SubscriptionActivated".equals(eventType) || eventType == null) {
                handleSubscriptionActivated(targetPayloadStr);
            } else {
                log.warn("Ignored unknown eventType: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing message in SubscriptionEventsConsumer", e);
        }
    }

    private void handleSubscriptionActivated(String payloadStr) {
        try {
            SubscriptionActivatedPayload payload = objectMapper.readValue(payloadStr, SubscriptionActivatedPayload.class);
            Long orderId = payload.getOrderId();

            if (orderId == null) {
                log.warn("Ignored SubscriptionActivated message: orderId is null");
                return;
            }

            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.warn("Ignored SubscriptionActivated message: Order not found with ID {}", orderId);
                return;
            }

            Order order = orderOpt.get();

            // Idempotency check: Ignore if status is already FULFILLED or CANCELLED
            if (order.getStatus().ordinal() >= OrderStatus.FULFILLED.ordinal()) {
                log.info("Ignored SubscriptionActivated message for order ID {}: Order status is already {}", orderId, order.getStatus());
                return;
            }

            Optional<SagaState> sagaOpt = sagaStateRepository.findByOrderId(orderId);
            if (sagaOpt.isPresent()) {
                SagaState sagaState = sagaOpt.get();
                sagaState.setCurrentStep("STEP_3");
                sagaState.setStatus(SagaStatus.COMPLETED);
                sagaStateRepository.save(sagaState);
            }

            order.setStatus(OrderStatus.FULFILLED);
            orderRepository.save(order);

            log.info("Processed SubscriptionActivated message for order ID {}: status updated to FULFILLED, Saga COMPLETED", orderId);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse SubscriptionActivated message", e);
        }
    }

    private void handleSubscriptionActivationFailed(String payloadStr) {
        try {
            SubscriptionActivationFailedPayload payload = objectMapper.readValue(payloadStr, SubscriptionActivationFailedPayload.class);
            Long orderId = payload.getOrderId();

            if (orderId == null) {
                log.warn("Ignored SubscriptionActivationFailed message: orderId is null");
                return;
            }

            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.warn("Ignored SubscriptionActivationFailed message: Order not found with ID {}", orderId);
                return;
            }

            Order order = orderOpt.get();

            // Idempotency: If already COMPENSATING, COMPENSATED, or CANCELLED
            if (order.getStatus() == OrderStatus.CANCELLED) {
                log.info("Ignored message for order ID {}: Order is already CANCELLED", orderId);
                return;
            }

            Optional<SagaState> sagaOpt = sagaStateRepository.findByOrderId(orderId);
            if (sagaOpt.isPresent()) {
                SagaState sagaState = sagaOpt.get();
                
                if ("COMPENSATING".equals(sagaState.getCurrentStep()) || "COMPENSATED".equals(sagaState.getCurrentStep())) {
                    log.info("Ignored message for order ID {}: Saga is already {}", orderId, sagaState.getCurrentStep());
                    return;
                }

                sagaState.setCurrentStep("COMPENSATING");
                
                String paymentId = null;
                JsonNode sagaPayload = sagaState.getPayload();
                if (sagaPayload != null && sagaPayload.has("paymentId")) {
                    paymentId = sagaPayload.get("paymentId").asText();
                }

                if (paymentId == null) {
                    log.error("Cannot proceed with compensation for order ID {}: paymentId is missing from SagaState payload", orderId);
                    return;
                }

                sagaStateRepository.save(sagaState);
                
                // Publish PaymentRefundRequested
                outboxEventPublisher.publishPaymentRefundRequested(orderId, paymentId, order.getTotalAmount());
                log.info("Published PaymentRefundRequested for order ID {}, payment ID {}", orderId, paymentId);
            } else {
                 log.warn("SagaState not found for order ID {}", orderId);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse SubscriptionActivationFailed message", e);
        }
    }
}
