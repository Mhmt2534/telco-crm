package com.telcox.springmicroservices.identity.service;

import com.telcox.springmicroservices.identity.config.KeycloakProperties;
import com.telcox.springmicroservices.identity.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Admin ve Dealer (saha bayisi) girişini Keycloak ROPC Grant üzerinden gerçekleştirir.
 * <p>
 * <b>Akış:</b>
 * <ol>
 *   <li>Kullanıcı adı + şifreyi alır</li>
 *   <li>Keycloak'ın {@code /protocol/openid-connect/token} endpoint'ine
 *       {@code grant_type=password} ile istek atar</li>
 *   <li>Keycloak'tan gelen {@code access_token + refresh_token + expires_in}
 *       bilgilerini olduğu gibi istemciye iletir</li>
 * </ol>
 * <p>
 * <b>Güvenlik notu:</b> Custom JWT üretilmez. Token oluşturma tamamen Keycloak'a aittir.
 * identity-service sadece ROPC proxy görevi görür.
 * <p>
 * Gereksinimler: Keycloak'ta "Direct Access Grants" (Resource Owner Password Credentials)
 * client ayarından aktif edilmiş olmalıdır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffAuthService {

    private final RestClient keycloakRestClient;
    private final KeycloakProperties keycloakProperties;

    /**
     * Verilen kullanıcı adı ve şifreyle Keycloak ROPC Grant akışını tetikler.
     *
     * @param username Keycloak kullanıcı adı (ADMIN veya DEALER rolüne sahip)
     * @param password Kullanıcı şifresi
     * @return Keycloak'tan dönen token bilgileri
     * @throws org.springframework.web.server.ResponseStatusException 401 — hatalı kimlik bilgileri
     */
    public TokenResponse login(String username, String password) {
        log.debug("Staff login attempt for user: {}", username);

        MultiValueMap<String, String> form = buildRopcForm(username, password);

        try {
            TokenResponse response = keycloakRestClient.post()
                    .uri(keycloakProperties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

            log.info("Staff login successful for user: {}", username);
            return response;

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Staff login failed — invalid credentials for user: {}", username);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Geçersiz kullanıcı adı veya şifre"
            );
        } catch (HttpClientErrorException e) {
            log.error("Keycloak returned error {} for user {}: {}", e.getStatusCode(), username, e.getMessage());
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Kimlik doğrulama servisi geçici olarak kullanılamıyor"
            );
        }
    }

    /**
     * Keycloak ROPC Grant için gerekli form parametrelerini oluşturur.
     */
    private MultiValueMap<String, String> buildRopcForm(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("username", username);
        form.add("password", password);
        return form;
    }
}
