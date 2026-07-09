package com.telcox.springmicroservices.orderservice.client.fallback;

import com.telcox.springmicroservices.orderservice.client.CustomerServiceClient;
import com.telcox.springmicroservices.orderservice.client.dto.CustomerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class CustomerServiceFallbackFactory implements FallbackFactory<CustomerServiceClient> {

    @Override
    public CustomerServiceClient create(Throwable cause) {
        if (cause instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            throw (io.github.resilience4j.circuitbreaker.CallNotPermittedException) cause;
        }
        if (cause instanceof com.telcox.common.core.exception.ResourceNotFoundException) {
            throw (com.telcox.common.core.exception.ResourceNotFoundException) cause;
        }
        if (cause instanceof feign.FeignException.NotFound) {
            throw (feign.FeignException.NotFound) cause;
        }
        if (cause instanceof ResponseStatusException && ((ResponseStatusException) cause).getStatusCode() == HttpStatus.NOT_FOUND) {
            throw (ResponseStatusException) cause;
        }

        return new CustomerServiceClient() {
            @Override
            public CustomerDto getCustomerById(Long customerId) {
                log.error("Customer service is unavailable for customerId: {}. Cause: {}", customerId, cause.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer service is currently unavailable");
            }
        };
    }
}
