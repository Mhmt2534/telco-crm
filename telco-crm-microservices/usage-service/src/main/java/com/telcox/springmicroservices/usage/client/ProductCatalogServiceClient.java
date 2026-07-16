package com.telcox.springmicroservices.usage.client;

import com.telcox.springmicroservices.usage.dto.TariffDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "product-catalog-service", path = "/api/v1/tariffs")
public interface ProductCatalogServiceClient {

    @GetMapping("/{id}")
    @CircuitBreaker(name = "productCatalogService", fallbackMethod = "getTariffFallback")
    TariffDto getTariffById(@PathVariable("id") UUID id);

    default TariffDto getTariffFallback(UUID code, Throwable t) {
        throw new RuntimeException("Product Catalog servisi geçici olarak kullanılamıyor, tarife bilgisi alınamadı (Code: " + code + ")", t);
    }
}
