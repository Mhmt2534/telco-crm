package com.telcox.springmicroservices.orderservice.service;

import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import com.telcox.springmicroservices.orderservice.domain.entity.SagaState;
import com.telcox.springmicroservices.orderservice.domain.enums.OrderStatus;
import com.telcox.springmicroservices.orderservice.domain.enums.SagaStatus;
import com.telcox.springmicroservices.orderservice.repository.OrderRepository;
import com.telcox.springmicroservices.orderservice.repository.SagaStateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public void startSaga(Order order) {
        log.info("Starting saga for order ID: {}", order.getId());

        // TODO: KART 17 - Synchronous customer and catalog checks using Resilience4j
        // will be added here. (e.g., check customer limits, reserve inventory)
        
        SagaState sagaState = SagaState.builder()
                .sagaId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .currentStep("STEP_1")
                .status(SagaStatus.STARTED)
                .build();

        sagaStateRepository.save(sagaState);
        log.info("SagaState saved with status STARTED and STEP_1 for order ID: {}", order.getId());

        outboxEventPublisher.publishOrderCreated(order);
    }

    @Transactional
    public void handleSubscriptionActivated(Long orderId) {
        log.info("Handling SubscriptionActivated for order ID: {}", orderId);
        
        sagaStateRepository.findByOrderId(orderId).ifPresent(sagaState -> {
            sagaState.setCurrentStep("STEP_3");
            sagaState.setStatus(SagaStatus.COMPLETED);
            sagaStateRepository.save(sagaState);
        });

        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(OrderStatus.FULFILLED);
            orderRepository.save(order);
        });
    }
}
