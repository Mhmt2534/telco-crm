package com.telcox.springmicroservices.orderservice.controller;

import com.telcox.springmicroservices.orderservice.dto.OrderRequest;
import com.telcox.springmicroservices.orderservice.dto.OrderResponse;
import com.telcox.springmicroservices.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {

        // IdempotencyKey is accepted but ignored for now as per requirements
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrdersByCustomerId(
            @RequestParam Long customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<OrderResponse> response = orderService.getOrdersByCustomerId(customerId, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/addons")
    public ResponseEntity<OrderResponse> createAddonOrder(
            @Valid @RequestBody com.telcox.springmicroservices.orderservice.dto.AddonRequest request) {
        OrderResponse response = orderService.createAddonOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/tariff-change")
    public ResponseEntity<OrderResponse> createTariffChangeOrder(
            @Valid @RequestBody com.telcox.springmicroservices.orderservice.dto.TariffChangeRequest request) {
        OrderResponse response = orderService.createTariffChangeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/compensate")
    public ResponseEntity<OrderResponse> compensateOrder(@PathVariable Long id) {
        OrderResponse response = orderService.compensateOrder(id);
        return ResponseEntity.ok(response);
    }
}
