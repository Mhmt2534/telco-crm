package com.telcox.springmicroservices.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * identity-service güvenlik konfigürasyonu.
 * <p>
 * Bu servis bir OAuth2 Resource Server DEĞİLDİR. JWT doğrulaması API Gateway
 * (KART 13) tarafından yapılır. Bu servis sadece login/OTP orchestration'ı yapar.
 * <p>
 * Tüm /api/v1/auth/** endpoint'leri public'tir (zaten unauthenticated kullanıcılar çağırır).
 * Actuator endpoint'leri Docker/K8s health probe'ları için açık bırakılır.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: stateless REST API — kapatılır
            .csrf(AbstractHttpConfigurer::disable)
            // Oturum tutulmaz — her istek kendi başına değerlendirilir
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Actuator: Docker/K8s readiness + liveness probe'ları için açık
                .requestMatchers("/actuator/**").permitAll()
                // Auth endpoint'leri: login, OTP request/verify, logout — herkese açık
                .requestMatchers("/api/v1/auth/**").permitAll()
                // OpenAPI / Swagger UI
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Diğer her şey — gelecekte korumalı endpoint'ler için
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
