package com.telcox.springmicroservices.orderservice.client;

import com.telcox.springmicroservices.orderservice.client.dto.CustomerDto;
import com.telcox.springmicroservices.orderservice.client.fallback.CustomerServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "customer-service", fallbackFactory = CustomerServiceFallbackFactory.class)
@Retry(name = "customer-service")
public interface CustomerServiceClient {

    @GetMapping("/api/v1/customers/{customerId}")
    CustomerDto getCustomerById(@PathVariable("customerId") Long customerId);
}
