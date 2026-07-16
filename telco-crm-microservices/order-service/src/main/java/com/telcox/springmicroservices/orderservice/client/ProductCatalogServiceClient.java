package com.telcox.springmicroservices.orderservice.client;

import com.telcox.springmicroservices.orderservice.client.dto.ProductDto;
import com.telcox.springmicroservices.orderservice.client.fallback.ProductCatalogServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(
    name = "product-catalog-service",
    fallbackFactory = ProductCatalogServiceFallbackFactory.class
)
@Retry(name = "product-catalog-service")
public interface ProductCatalogServiceClient {

    @GetMapping("/api/v1/products/batch")
    List<ProductDto> getProductsByIds(@RequestParam("ids") List<UUID> productIds);

    @GetMapping("/api/v1/addons")
    String getActiveAddons(
            @RequestParam(value = "tariffId", required = false) UUID tariffId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    );
}
