package com.telcox.springmicroservices.orderservice.client.fallback;

import com.telcox.springmicroservices.orderservice.client.ProductCatalogServiceClient;
import com.telcox.springmicroservices.orderservice.client.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Component
public class ProductCatalogServiceFallbackFactory implements FallbackFactory<ProductCatalogServiceClient> {

    @Override
    public ProductCatalogServiceClient create(Throwable cause) {
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

        return new ProductCatalogServiceClient() {
            @Override
            public List<ProductDto> getProductsByCodes(List<String> productCodes) {
                log.error("Product catalog service is unavailable for productCodes: {}. Cause: {}", productCodes, cause.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product catalog service is currently unavailable");
            }

            @Override
            public String getActiveAddons(String tariffCode, int page, int size) {
                log.error("Product catalog service is unavailable for getting addons by tariffCode: {}. Cause: {}", tariffCode, cause.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product catalog service is currently unavailable");
            }
        };
    }
}
