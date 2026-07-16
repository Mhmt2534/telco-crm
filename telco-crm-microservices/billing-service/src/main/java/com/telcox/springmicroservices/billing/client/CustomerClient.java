package com.telcox.springmicroservices.billing.client;

import com.telcox.springmicroservices.billing.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "customer-service", url = "${app.feign.customer-service-url:}")
public interface CustomerClient {

    @GetMapping("/api/v1/customers/{id}")
    CustomerResponse getCustomerById(@PathVariable("id") UUID id);
}
