package com.telcox.springmicroservices.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserContextRelayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(jwtAuthToken -> jwtAuthToken.getToken().getSubject())
                .flatMap(sub -> {
                    // Strip existing X-User-Id to prevent spoofing
                    // Add X-User-Id header from JWT subject
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .headers(headers -> headers.remove("X-User-Id"))
                            .header("X-User-Id", sub)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(chain.filter(exchange)); // Proceed without modification if not authenticated
    }

    @Override
    public int getOrder() {
        // Run after Spring Security filters (which populate the ReactiveSecurityContext)
        return 0;
    }
}
