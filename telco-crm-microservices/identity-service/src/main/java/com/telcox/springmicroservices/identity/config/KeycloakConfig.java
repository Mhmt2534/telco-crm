package com.telcox.springmicroservices.identity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Keycloak ile HTTP iletişimi için gereken bean'leri tanımlar.
 * <p>
 * identity-service bir resource server DEĞİLDİR; sadece Keycloak'a
 * HTTP istek atan bir orchestration servisidir. Bu nedenle JWT doğrulaması
 * burada yapılmaz — bu iş API Gateway'e (KART 13) aittir.
 * <p>
 * Kullanılan {@link RestClient}, WebFlux bağımlılığı gerektirmeyen
 * Spring Framework 6.1+ senkron fluent HTTP istemcisidir.
 */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakConfig {

    /**
     * Keycloak OIDC endpoint'lerine (token, logout) istek atmak için kullanılır.
     * Base URL ayarlanmadı — her serviste tam URL verilir (token vs logout farklı path).
     */
    @Bean
    public RestClient keycloakRestClient() {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
