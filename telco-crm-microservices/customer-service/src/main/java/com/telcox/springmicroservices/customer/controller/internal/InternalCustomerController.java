package com.telcox.springmicroservices.customer.controller.internal;

import com.telcox.springmicroservices.customer.dto.InternalCustomerResponse;
import com.telcox.common.core.model.CustomerIdentityResponse;
import com.telcox.springmicroservices.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/customers")
@RequiredArgsConstructor
public class InternalCustomerController {

    private final CustomerService customerService;

    @GetMapping("/by-phone/{phone}")
    public ResponseEntity<InternalCustomerResponse> getCustomerByPhone(@PathVariable String phone) {
        InternalCustomerResponse response = customerService.getInternalCustomerByPhone(phone);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-keycloak-id/{keycloakUserId}")
    public ResponseEntity<CustomerIdentityResponse> getCustomerByKeycloakUserId(
            @PathVariable String keycloakUserId) {
        return ResponseEntity.ok(customerService.getCustomerIdentityByKeycloakUserId(keycloakUserId));
    }
}
