package com.telcox.springmicroservices.orderservice.service;

import com.telcox.springmicroservices.orderservice.dto.OrderRequest;
import com.telcox.springmicroservices.orderservice.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    OrderResponse createAddonOrder(com.telcox.springmicroservices.orderservice.dto.AddonRequest request);
    OrderResponse createTariffChangeOrder(com.telcox.springmicroservices.orderservice.dto.TariffChangeRequest request);
    OrderResponse getOrderById(Long id);
    Page<OrderResponse> getOrdersByCustomerId(Long customerId, Pageable pageable);
    OrderResponse cancelOrder(Long id);
    OrderResponse compensateOrder(Long id);
}
