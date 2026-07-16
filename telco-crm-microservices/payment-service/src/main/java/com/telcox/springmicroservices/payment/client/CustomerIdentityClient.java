package com.telcox.springmicroservices.payment.client;

import com.telcox.common.core.model.CustomerIdentityResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", contextId = "paymentCustomerIdentityClient")
public interface CustomerIdentityClient {

    @GetMapping("/api/v1/internal/customers/by-keycloak-id/{keycloakUserId}")
    CustomerIdentityResponse getByKeycloakUserId(@PathVariable("keycloakUserId") String keycloakUserId);
}
