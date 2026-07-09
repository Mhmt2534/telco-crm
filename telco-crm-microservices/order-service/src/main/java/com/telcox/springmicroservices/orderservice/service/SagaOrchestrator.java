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

import com.telcox.springmicroservices.orderservice.client.CustomerServiceClient;
import com.telcox.springmicroservices.orderservice.client.ProductCatalogServiceClient;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final CustomerServiceClient customerServiceClient;
    private final ProductCatalogServiceClient productCatalogServiceClient;

    @Transactional
    public void startSaga(Order order) {
        log.info("Starting saga for order ID: {}", order.getId());

        // KART 17 - Synchronous customer and catalog checks using Resilience4j
        try {
            log.info("Checking customer ID: {}", order.getCustomerId());
            customerServiceClient.getCustomerById(order.getCustomerId());

            List<String> productCodes = order.getItems().stream()
                    .map(item -> item.getProductCode())
                    .toList();
            log.info("Checking product codes: {}", productCodes);
            if (!productCodes.isEmpty()) {
                productCatalogServiceClient.getProductsByCodes(productCodes);
            }
        } catch (com.telcox.common.core.exception.ResourceNotFoundException e) {
            log.warn("Downstream resource not found for order ID: {}", order.getId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Failed synchronous checks for order ID: {}", order.getId(), e);
            throw e;
        }
        
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
