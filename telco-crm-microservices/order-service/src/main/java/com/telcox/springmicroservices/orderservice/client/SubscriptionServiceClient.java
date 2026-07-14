package com.telcox.springmicroservices.orderservice.client;

import com.telcox.springmicroservices.orderservice.client.dto.SubscriptionDto;
import com.telcox.springmicroservices.orderservice.client.fallback.SubscriptionServiceFallbackFactory;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
    name = "subscription-service",
    fallbackFactory = SubscriptionServiceFallbackFactory.class
)
@Retry(name = "subscription-service")
public interface SubscriptionServiceClient {

    @GetMapping("/api/v1/subscriptions/{id}")
    SubscriptionDto getSubscription(@PathVariable("id") UUID id);
}
