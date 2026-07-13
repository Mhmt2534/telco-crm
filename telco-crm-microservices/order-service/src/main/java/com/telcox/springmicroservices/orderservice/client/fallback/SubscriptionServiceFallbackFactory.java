package com.telcox.springmicroservices.orderservice.client.fallback;

import com.telcox.springmicroservices.orderservice.client.SubscriptionServiceClient;
import com.telcox.springmicroservices.orderservice.client.dto.SubscriptionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class SubscriptionServiceFallbackFactory implements FallbackFactory<SubscriptionServiceClient> {

    @Override
    public SubscriptionServiceClient create(Throwable cause) {
        return new SubscriptionServiceClient() {
            @Override
            public SubscriptionDto getSubscription(UUID id) {
                log.error("Fallback triggered for getSubscription, id: {}, reason: {}", id, cause.getMessage());
                throw new RuntimeException("Subscription service is unavailable. Reason: " + cause.getMessage(), cause);
            }
        };
    }
}
