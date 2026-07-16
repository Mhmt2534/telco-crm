package com.telcox.springmicroservices.orderservice.controller;

import com.telcox.springmicroservices.orderservice.dto.OrderRequest;
import com.telcox.springmicroservices.orderservice.dto.OrderResponse;
import com.telcox.springmicroservices.orderservice.service.OrderService;
import com.telcox.springmicroservices.orderservice.service.AuthenticatedCustomerResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AuthenticatedCustomerResolver authenticatedCustomerResolver;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader("X-User-Id") String keycloakUserId,
            @Valid @RequestBody OrderRequest request) {

        // IdempotencyKey is accepted but ignored for now as per requirements
        request.setCustomerId(authenticatedCustomerResolver.resolve(keycloakUserId));
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String keycloakUserId) {
        OrderResponse response = orderService.getOrderById(id);
        assertCustomerOwnsOrder(response, authenticatedCustomerResolver.resolve(keycloakUserId));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrdersByCustomerId(
            @RequestHeader("X-User-Id") String keycloakUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID customerId = authenticatedCustomerResolver.resolve(keycloakUserId);
        Page<OrderResponse> response = orderService.getOrdersByCustomerId(customerId, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String keycloakUserId) {
        UUID customerId = authenticatedCustomerResolver.resolve(keycloakUserId);
        assertCustomerOwnsOrder(orderService.getOrderById(id), customerId);
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/addons")
    public ResponseEntity<OrderResponse> createAddonOrder(
            @RequestHeader("X-User-Id") String keycloakUserId,
            @Valid @RequestBody com.telcox.springmicroservices.orderservice.dto.AddonRequest request) {
        request.setCustomerId(authenticatedCustomerResolver.resolve(keycloakUserId));
        OrderResponse response = orderService.createAddonOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/tariff-change")
    public ResponseEntity<OrderResponse> createTariffChangeOrder(
            @RequestHeader("X-User-Id") String keycloakUserId,
            @Valid @RequestBody com.telcox.springmicroservices.orderservice.dto.TariffChangeRequest request) {
        request.setCustomerId(authenticatedCustomerResolver.resolve(keycloakUserId));
        OrderResponse response = orderService.createTariffChangeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/compensate")
    public ResponseEntity<OrderResponse> compensateOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.compensateOrder(id);
        return ResponseEntity.ok(response);
    }

    private void assertCustomerOwnsOrder(OrderResponse order, UUID authenticatedCustomerId) {
        if (!authenticatedCustomerId.equals(order.getCustomerId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Order does not belong to the authenticated customer");
        }
    }
}
