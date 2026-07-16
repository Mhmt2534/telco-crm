package com.telcox.springmicroservices.payment.controller;

import com.telcox.springmicroservices.payment.dto.PaymentRequest;
import com.telcox.springmicroservices.payment.dto.PaymentResponse;
import com.telcox.springmicroservices.payment.service.AuthenticatedCustomerResolver;
import com.telcox.springmicroservices.payment.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerIdentityTest {

    @Test
    void keepsActorSubjectSeparateAndUsesResolvedPublicCustomerId() {
        PaymentService paymentService = mock(PaymentService.class);
        AuthenticatedCustomerResolver resolver = mock(AuthenticatedCustomerResolver.class);
        UUID authenticatedCustomerId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest();
        request.setCustomerId(UUID.randomUUID());
        PaymentResponse response = new PaymentResponse();
        when(resolver.resolve("keycloak-subject")).thenReturn(authenticatedCustomerId);
        when(paymentService.initiatePayment(request, "idempotency-key", "keycloak-subject"))
                .thenReturn(response);

        PaymentController controller = new PaymentController(paymentService, resolver);
        var result = controller.initiatePayment(
                "idempotency-key", "keycloak-subject", "correlation-id", request);

        assertThat(request.getCustomerId()).isEqualTo(authenticatedCustomerId);
        assertThat(result.getBody()).isSameAs(response);
        verify(paymentService).initiatePayment(request, "idempotency-key", "keycloak-subject");
    }
}
