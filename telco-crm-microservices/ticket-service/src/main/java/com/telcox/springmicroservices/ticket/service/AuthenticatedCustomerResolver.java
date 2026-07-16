package com.telcox.springmicroservices.ticket.service;

import com.telcox.common.core.model.CustomerIdentityResponse;
import com.telcox.springmicroservices.ticket.client.CustomerIdentityClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthenticatedCustomerResolver {

    private final CustomerIdentityClient customerIdentityClient;

    public UUID resolve(String keycloakUserId) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user identity is missing");
        }
        try {
            CustomerIdentityResponse identity = customerIdentityClient.getByKeycloakUserId(keycloakUserId);
            if (identity == null || identity.customerId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Customer identity response is invalid");
            }
            return identity.customerId();
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Authenticated user is not mapped to an active customer", ex);
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Customer identity lookup is temporarily unavailable", ex);
        }
    }
}
