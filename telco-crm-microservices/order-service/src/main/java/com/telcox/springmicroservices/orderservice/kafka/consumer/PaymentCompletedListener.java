package com.telcox.springmicroservices.orderservice.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import com.telcox.springmicroservices.orderservice.domain.entity.SagaState;
import com.telcox.springmicroservices.orderservice.domain.enums.OrderStatus;
import com.telcox.springmicroservices.orderservice.domain.enums.SagaStatus;
import com.telcox.springmicroservices.orderservice.dto.PaymentCompletedPayload;
import com.telcox.springmicroservices.orderservice.repository.OrderRepository;
import com.telcox.springmicroservices.orderservice.repository.SagaStateRepository;
import com.telcox.springmicroservices.orderservice.service.OutboxEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedListener {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.payment-completed}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onPaymentCompleted(String message) {
        try {
            log.info("Received PaymentCompleted message: {}", message);
            PaymentCompletedPayload payload = objectMapper.readValue(message, PaymentCompletedPayload.class);
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
                sagaStateRepository.save(sagaState);
            }

            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            outboxEventPublisher.publishOrderConfirmed(order);
            log.info("Processed PaymentCompleted message for order ID {}: status updated to PAID", orderId);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse PaymentCompleted message", e);
        } catch (Exception e) {
            log.error("Error processing PaymentCompleted message", e);
            throw new RuntimeException("Error processing PaymentCompleted message", e);
        }
    }
}
