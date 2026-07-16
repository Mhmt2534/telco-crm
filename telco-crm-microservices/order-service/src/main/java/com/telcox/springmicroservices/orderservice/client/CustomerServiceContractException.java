package com.telcox.springmicroservices.orderservice.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Raised when customer-service returns JSON that violates the declared Feign contract. */
public class CustomerServiceContractException extends ResponseStatusException {

    public CustomerServiceContractException(Throwable cause) {
        super(HttpStatus.BAD_GATEWAY,
                "Customer service returned a response incompatible with the API contract", cause);
    }
}
