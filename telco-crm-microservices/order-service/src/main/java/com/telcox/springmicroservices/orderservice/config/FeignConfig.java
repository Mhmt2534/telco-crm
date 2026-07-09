package com.telcox.springmicroservices.orderservice.config;

import com.telcox.common.core.constant.HeaderConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.resilience4j.core.ContextPropagator;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import feign.Response;
import feign.codec.ErrorDecoder;
import com.telcox.common.core.exception.ResourceNotFoundException;

@Configuration
public class FeignConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 1. Try to retrieve from MDC (if propagated)
        String correlationId = MDC.get("correlationId");
        
        // 2. Try to retrieve from RequestContextHolder (if interceptor runs in request thread or if propagated)
        if (correlationId == null || correlationId.isBlank()) {
            org.springframework.web.context.request.RequestAttributes attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) attributes).getRequest();
                correlationId = request.getHeader(HeaderConstants.CORRELATION_ID);
            }
        }
        
        // 3. Fallback to generating a new one
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = java.util.UUID.randomUUID().toString();
        }

        template.header(HeaderConstants.CORRELATION_ID, correlationId);
    }

    @Bean
    public ContextPropagator<Map<String, String>> mdcContextPropagator() {
        return new ContextPropagator<Map<String, String>>() {
            @Override
            public Supplier<Optional<Map<String, String>>> retrieve() {
                return () -> Optional.ofNullable(MDC.getCopyOfContextMap());
            }

            @Override
            public Consumer<Optional<Map<String, String>>> copy() {
                return map -> {
                    if (map.isPresent()) {
                        MDC.setContextMap(map.get());
                    } else {
                        MDC.clear();
                    }
                };
            }

            @Override
            public Consumer<Optional<Map<String, String>>> clear() {
                return map -> MDC.clear();
            }
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new ErrorDecoder() {
            private final ErrorDecoder defaultErrorDecoder = new Default();

            @Override
            public Exception decode(String methodKey, Response response) {
                if (response.status() == 404) {
                    return new ResourceNotFoundException("Bağımlı serviste kayıt bulunamadı (404 Not Found)");
                }
                return defaultErrorDecoder.decode(methodKey, response);
            }
        };
    }
}
