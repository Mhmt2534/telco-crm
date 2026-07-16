package com.telcox.springmicroservices.orderservice.service;

import com.telcox.springmicroservices.orderservice.dto.OrderRequest;
import com.telcox.springmicroservices.orderservice.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    OrderResponse createAddonOrder(com.telcox.springmicroservices.orderservice.dto.AddonRequest request);
    OrderResponse createTariffChangeOrder(com.telcox.springmicroservices.orderservice.dto.TariffChangeRequest request);
    OrderResponse getOrderById(UUID id);
    Page<OrderResponse> getOrdersByCustomerId(UUID customerId, Pageable pageable);
    OrderResponse cancelOrder(UUID id);
    OrderResponse compensateOrder(UUID id);
}
