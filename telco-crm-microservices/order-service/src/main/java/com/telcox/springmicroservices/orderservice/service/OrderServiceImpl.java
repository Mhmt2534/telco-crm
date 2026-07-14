package com.telcox.springmicroservices.orderservice.service;

import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import com.telcox.springmicroservices.orderservice.domain.entity.OrderItem;
import com.telcox.springmicroservices.orderservice.domain.entity.SagaState;
import com.telcox.springmicroservices.orderservice.domain.enums.OrderStatus;
import com.telcox.springmicroservices.orderservice.dto.OrderRequest;
import com.telcox.springmicroservices.orderservice.dto.OrderResponse;
import com.telcox.springmicroservices.orderservice.mapper.OrderMapper;
import com.telcox.springmicroservices.orderservice.repository.OrderRepository;
import com.telcox.springmicroservices.orderservice.repository.SagaStateRepository;
import com.telcox.common.core.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.telcox.springmicroservices.orderservice.client.ProductCatalogServiceClient;
import com.telcox.springmicroservices.orderservice.client.SubscriptionServiceClient;
import com.telcox.springmicroservices.orderservice.client.dto.ProductDto;
import com.telcox.springmicroservices.orderservice.client.dto.SubscriptionDto;
import com.telcox.springmicroservices.orderservice.domain.enums.ProductType;
import com.telcox.springmicroservices.orderservice.dto.AddonRequest;
import com.telcox.springmicroservices.orderservice.dto.TariffChangeRequest;
import com.telcox.springmicroservices.orderservice.domain.enums.SagaStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final SagaOrchestrator sagaOrchestrator;
    private final SubscriptionServiceClient subscriptionServiceClient;
    private final ProductCatalogServiceClient productCatalogServiceClient;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ObjectMapper objectMapper;
    private final OrderMapper orderMapper = org.mapstruct.factory.Mappers.getMapper(OrderMapper.class);

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        Order order = orderMapper.toEntity(request);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCurrency("TRY");

        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (request.getItems() != null) {
            for (var itemReq : request.getItems()) {
                OrderItem item = orderMapper.toEntity(itemReq);
                BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(lineTotal);
                order.addItem(item);
            }
        }
        order.setTotalAmount(totalAmount);

        order = orderRepository.save(order);
        log.info("Created order with ID: {}", order.getId());

        sagaOrchestrator.startSaga(order);

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order with ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        SagaState sagaState = sagaStateRepository.findByOrderId(id).orElse(null);
        return orderMapper.toResponse(order, sagaState);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomerId(Long customerId, Pageable pageable) {
        log.info("Fetching orders for customer ID: {}", customerId);
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(order -> {
                    SagaState sagaState = sagaStateRepository.findByOrderId(order.getId()).orElse(null);
                    return orderMapper.toResponse(order, sagaState);
                });
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        log.info("Cancelling order with ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Order cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        SagaState sagaState = sagaStateRepository.findByOrderId(id).orElse(null);
        return orderMapper.toResponse(order, sagaState);
    }

    @Override
    @Transactional
    public OrderResponse createAddonOrder(AddonRequest request) {
        log.info("Creating addon order for subscription: {}", request.getSubscriptionId());

        // 1. Fetch subscription details to get the current tariffCode
        SubscriptionDto subscription = subscriptionServiceClient.getSubscription(request.getSubscriptionId());
        if (subscription == null) {
            throw new ResourceNotFoundException("Subscription not found: " + request.getSubscriptionId());
        }
        String tariffCode = subscription.getTariffCode();

        // 2. Fetch allowed addons for this tariff
        String addonsJsonResponse = productCatalogServiceClient.getActiveAddons(tariffCode, 0, 100);
        boolean isValidAddon = false;
        
        if (addonsJsonResponse != null && !addonsJsonResponse.isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode addonsResponse = objectMapper.readTree(addonsJsonResponse);
                if (addonsResponse.has("content")) {
                    for (com.fasterxml.jackson.databind.JsonNode node : addonsResponse.get("content")) {
                        if (node.has("code") && request.getAddonCode().equals(node.get("code").asText())) {
                            isValidAddon = true;
                            break;
                        }
                    }
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("Failed to parse addons response for tariff {}: {}", tariffCode, e.getMessage());
            }
        }
        
        // 3. Validate
        if (!isValidAddon) {
            log.warn("Validation failed: Addon {} is not compatible with tariff {}", request.getAddonCode(), tariffCode);
            throw new com.telcox.common.core.exception.BusinessRuleException("The requested addon " + request.getAddonCode() + " is not compatible with the subscriber's current tariff " + tariffCode);
        }

        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setSubscriptionId(request.getSubscriptionId());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCurrency("TRY");

        // We fetch the product price
        List<ProductDto> products = productCatalogServiceClient.getProductsByCodes(List.of(request.getAddonCode()));
        if (products == null || products.isEmpty()) {
            throw new ResourceNotFoundException("Addon product not found: " + request.getAddonCode());
        }
        ProductDto addonProduct = products.get(0);

        OrderItem item = new OrderItem();
        item.setProductCode(request.getAddonCode());
        item.setProductType(ProductType.ADDON);
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(addonProduct.getPrice());
        
        order.addItem(item);
        
        BigDecimal totalAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        order.setTotalAmount(totalAmount);

        order = orderRepository.save(order);
        log.info("Created addon order with ID: {}", order.getId());

        sagaOrchestrator.startSaga(order);

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse createTariffChangeOrder(TariffChangeRequest request) {
        log.info("Creating tariff change order for subscription: {}", request.getSubscriptionId());

        // 1. Get current subscription to find old tariff code
        SubscriptionDto subscription = subscriptionServiceClient.getSubscription(request.getSubscriptionId());
        if (subscription == null) {
            throw new ResourceNotFoundException("Subscription not found: " + request.getSubscriptionId());
        }
        String oldTariffCode = subscription.getTariffCode();
        String newTariffCode = request.getNewTariffCode();

        // 2. Get product details for old and new tariff
        List<ProductDto> products = productCatalogServiceClient.getProductsByCodes(List.of(oldTariffCode, newTariffCode));
        ProductDto oldTariffProduct = products.stream().filter(p -> p.getProductCode().equals(oldTariffCode)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Old tariff product not found: " + oldTariffCode));
        ProductDto newTariffProduct = products.stream().filter(p -> p.getProductCode().equals(newTariffCode)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("New tariff product not found: " + newTariffCode));

        // 3. Calculate price diff
        BigDecimal priceDiff = newTariffProduct.getPrice().subtract(oldTariffProduct.getPrice());

        // 4. Create Order and OrderItem
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setSubscriptionId(request.getSubscriptionId());
        order.setStatus(OrderStatus.FULFILLED); // Per user feedback
        order.setCurrency("TRY");
        order.setTotalAmount(BigDecimal.ZERO); // It's just a change, billed next cycle

        OrderItem item = new OrderItem();
        item.setProductCode(newTariffCode);
        item.setProductType(ProductType.TARIFF_CHANGE);
        item.setQuantity(1);
        item.setUnitPrice(priceDiff);
        
        order.addItem(item);
        order = orderRepository.save(order);

        // 5. Publish event directly in the same transaction
        outboxEventPublisher.publishTariffChangeRequested(order, oldTariffCode, newTariffCode, priceDiff);

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse compensateOrder(Long id) {
        log.info("Manual compensation triggered for order ID: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        SagaState sagaState = sagaStateRepository.findByOrderId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Saga state not found for order ID: " + id));

        if (sagaState.getStatus() == SagaStatus.COMPENSATED || sagaState.getStatus() == SagaStatus.COMPLETED) {
            log.warn("Saga is already in a terminal state: {}", sagaState.getStatus());
            return orderMapper.toResponse(order, sagaState);
        }

        // Trigger compensation logic
        sagaState.setStatus(SagaStatus.COMPENSATED);
        sagaStateRepository.save(sagaState);

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        // We assume paymentId is null here since we might not have it in this simple flow, or we'd fetch it from payload.
        // For manual compensate, we just publish the event with a dummy paymentId if needed, or extract from payload if exists.
        String paymentId = "MANUAL_COMPENSATE"; 
        if (sagaState.getPayload() != null && sagaState.getPayload().has("paymentId")) {
            paymentId = sagaState.getPayload().get("paymentId").asText();
        }
        
        outboxEventPublisher.publishPaymentRefundRequested(order.getId(), paymentId, order.getTotalAmount());
        
        return orderMapper.toResponse(order, sagaState);
    }
}
