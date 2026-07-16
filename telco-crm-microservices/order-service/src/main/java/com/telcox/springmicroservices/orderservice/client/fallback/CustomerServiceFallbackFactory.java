package com.telcox.springmicroservices.orderservice.client.fallback;

import com.telcox.springmicroservices.orderservice.client.CustomerServiceClient;
import com.telcox.springmicroservices.orderservice.client.CustomerServiceContractException;
import com.telcox.springmicroservices.orderservice.client.dto.CustomerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import feign.FeignException;
import feign.RetryableException;
import feign.codec.DecodeException;

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
            public CustomerDto getCustomerById(UUID customerId) {
                Throwable classified = findRelevantCause(cause);

                if (classified instanceof DecodeException) {
                    log.error("Customer service contract violation for customerId {}: response could not be decoded ({})",
                            customerId, classified.getClass().getSimpleName());
                    throw new CustomerServiceContractException(classified);
                }

                if (classified instanceof RetryableException) {
                    log.error("Customer service connection failed for customerId {}: {}",
                            customerId, classified.getClass().getSimpleName());
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Customer service connection failed", classified);
                }

                if (classified instanceof FeignException feignException) {
                    log.error("Customer service returned HTTP {} for customerId {}",
                            feignException.status(), customerId);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Customer service returned HTTP " + feignException.status(), feignException);
                }

                log.error("Customer service call failed for customerId {}: {}",
                        customerId, classified.getClass().getSimpleName());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Customer service is currently unavailable", classified);
            }
        };
    }

    private static Throwable findRelevantCause(Throwable cause) {
        Throwable current = cause;
        while (current.getCause() != null && current != current.getCause()) {
            if (current instanceof DecodeException
                    || current instanceof RetryableException
                    || current instanceof FeignException) {
                return current;
            }
            current = current.getCause();
        }
        return current;
    }
}
