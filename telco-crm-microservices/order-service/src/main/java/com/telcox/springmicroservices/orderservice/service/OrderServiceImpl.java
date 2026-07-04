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
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        Order order = orderMapper.toEntity(request);
        order.setStatus(OrderStatus.DRAFT);
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
