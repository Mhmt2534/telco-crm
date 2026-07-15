package com.telcox.springmicroservices.identity.service;

import com.telcox.springmicroservices.identity.config.KeycloakProperties;
import com.telcox.springmicroservices.identity.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Keycloak üzerinden refresh token ile yeni access token alma işlemini gerçekleştirir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final RestClient keycloakRestClient;
    private final KeycloakProperties keycloakProperties;

    /**
     * Verilen refresh token ile Keycloak'tan yeni token alır.
     *
     * @param refreshToken Keycloak refresh token
     * @return Yeni token bilgileri (access_token, refresh_token, vs.)
     */
    public TokenResponse refresh(String refreshToken) {
        log.debug("Token refresh request received");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("refresh_token", refreshToken);

        try {
            TokenResponse response = keycloakRestClient.post()
                    .uri(keycloakProperties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

            log.info("Token refreshed successfully");
            return response;

        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.warn("Token refresh failed - invalid or expired refresh token");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Oturum süresi dolmuş veya geçersiz token. Lütfen tekrar giriş yapın."
            );
        } catch (HttpClientErrorException e) {
            log.error("Keycloak returned error {} during refresh: {}", e.getStatusCode(), e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Kimlik doğrulama servisi geçici olarak kullanılamıyor"
            );
        }
    }
}
