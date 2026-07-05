package com.telcox.springmicroservices.apigateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(jwtAuth -> jwtAuth.getToken().getSubject())
                .switchIfEmpty(Mono.just(
                        exchange.getRequest().getRemoteAddress() != null
                                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                                : "anonymous"
                ));
    }

    /**
     * Programatically disable rate-limiting headers (X-RateLimit-*) globally.
     * This prevents UnsupportedOperationException when the response is already committed 
     * (e.g. 429 Too Many Requests) and becomes read-only.
     * Doing this programmatically overrides any YAML parsing bugs in Spring Cloud Gateway.
     */
    @Autowired
    public void customizeRateLimiter(RedisRateLimiter redisRateLimiter) {
        redisRateLimiter.setIncludeHeaders(false);
    }
}
