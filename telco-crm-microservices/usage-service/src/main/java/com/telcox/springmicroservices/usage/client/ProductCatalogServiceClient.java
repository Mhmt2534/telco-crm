package com.telcox.springmicroservices.usage.client;

import com.telcox.springmicroservices.usage.dto.TariffDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-catalog-service", path = "/api/v1/tariffs")
public interface ProductCatalogServiceClient {

    @GetMapping("/{code}")
    @CircuitBreaker(name = "productCatalogService", fallbackMethod = "getTariffFallback")
    TariffDto getTariffByCode(@PathVariable("code") String code);

    default TariffDto getTariffFallback(String code, Throwable t) {
        throw new RuntimeException("Product Catalog servisi geçici olarak kullanılamıyor, tarife bilgisi alınamadı (Code: " + code + ")", t);
    }
}
