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
import com.telcox.springmicroservices.orderservice.repository.OutboxEventRepository;
import com.telcox.springmicroservices.orderservice.domain.entity.OutboxEvent;
import com.telcox.common.core.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper = org.mapstruct.factory.Mappers.getMapper(OrderMapper.class);
    private final ObjectMapper objectMapper;

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

        // Create outbox event
        try {
            java.util.Map<String, Object> eventPayload = new java.util.HashMap<>();
            eventPayload.put("orderId", order.getId());
            eventPayload.put("customerId", order.getCustomerId());
            eventPayload.put("totalAmount", order.getTotalAmount());
            eventPayload.put("occurredAt", java.time.Instant.now().toString());

            JsonNode payloadNode = objectMapper.valueToTree(eventPayload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .eventType("OrderCreated")
                    .payload(payloadNode)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("OrderCreated outbox event saved for order ID: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to create outbox event for order ID: {}", order.getId(), e);
            throw new RuntimeException("Failed to save outbox event", e);
        }

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
}
