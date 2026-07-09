package com.telcox.springmicroservices.orderservice.client;

import com.telcox.springmicroservices.orderservice.client.dto.ProductDto;
import com.telcox.springmicroservices.orderservice.client.fallback.ProductCatalogServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(
    name = "product-catalog-service",
    fallbackFactory = ProductCatalogServiceFallbackFactory.class
)
@Retry(name = "product-catalog-service")
public interface ProductCatalogServiceClient {

    @GetMapping("/api/products/batch")
    List<ProductDto> getProductsByCodes(@RequestParam("codes") List<String> productCodes);
}
