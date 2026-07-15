package com.telcox.springmicroservices.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // CSRF disabled because this is a stateless API Gateway that uses JWTs
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints: OTP request/verify, Staff login, and actuator for monitoring
                        .pathMatchers("/api/v1/auth/staff/login", 
                                      "/api/v1/auth/customer/request-otp", 
                                      "/api/v1/auth/customer/verify-otp",
                                      "/api/v1/auth/refresh",
                                      "/actuator/**",
                                      "/swagger-ui.html",
                                      "/swagger-ui/**",
                                      "/v3/api-docs/**",
                                      "/*/v3/api-docs/**",
                                      "/webjars/**").permitAll()
                        // All other endpoints require authentication
                        .anyExchange().authenticated()
                )
                // Configure OAuth2 Resource Server to validate JWTs (automatically uses Keycloak JWKS via issuer-uri)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(org.springframework.security.config.Customizer.withDefaults()))
                .build();
    }
}
