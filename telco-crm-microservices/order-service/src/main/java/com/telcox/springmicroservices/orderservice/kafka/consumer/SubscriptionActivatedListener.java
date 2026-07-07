package com.telcox.springmicroservices.orderservice.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import com.telcox.springmicroservices.orderservice.domain.entity.SagaState;
import com.telcox.springmicroservices.orderservice.domain.enums.OrderStatus;
import com.telcox.springmicroservices.orderservice.domain.enums.SagaStatus;
import com.telcox.springmicroservices.orderservice.dto.SubscriptionActivatedPayload;
import com.telcox.springmicroservices.orderservice.repository.OrderRepository;
import com.telcox.springmicroservices.orderservice.repository.SagaStateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionActivatedListener {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.subscription-activated}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onSubscriptionActivated(String message) {
        try {
            log.info("Received SubscriptionActivated message: {}", message);
            SubscriptionActivatedPayload payload = objectMapper.readValue(message, SubscriptionActivatedPayload.class);
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
        } catch (Exception e) {
            log.error("Error processing SubscriptionActivated message", e);
            throw new RuntimeException("Error processing SubscriptionActivated message", e);
        }
    }
}
