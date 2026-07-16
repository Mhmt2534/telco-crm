package com.telcox.springmicroservices.orderservice.controller;

import com.telcox.springmicroservices.orderservice.dto.OrderRequest;
import com.telcox.springmicroservices.orderservice.dto.OrderResponse;
import com.telcox.springmicroservices.orderservice.service.AuthenticatedCustomerResolver;
import com.telcox.springmicroservices.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderControllerIdentityTest {

    @Test
    void replacesUntrustedBodyCustomerIdWithAuthenticatedCustomersPublicId() {
        OrderService orderService = mock(OrderService.class);
        AuthenticatedCustomerResolver resolver = mock(AuthenticatedCustomerResolver.class);
        UUID authenticatedCustomerId = UUID.randomUUID();
        OrderRequest request = new OrderRequest();
        request.setCustomerId(UUID.randomUUID());
        OrderResponse response = OrderResponse.builder().customerId(authenticatedCustomerId).build();
        when(resolver.resolve("keycloak-subject")).thenReturn(authenticatedCustomerId);
        when(orderService.createOrder(request)).thenReturn(response);

        OrderController controller = new OrderController(orderService, resolver);
        var result = controller.createOrder("idempotency-key", "keycloak-subject", request);

        assertThat(request.getCustomerId()).isEqualTo(authenticatedCustomerId);
        assertThat(result.getBody()).isSameAs(response);
        verify(orderService).createOrder(request);
    }
}
