package com.telcox.springmicroservices.identity.service;

import com.telcox.springmicroservices.identity.config.KeycloakProperties;
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
 * Keycloak oturumunu sonlandırma işlemini gerçekleştirir.
 * <p>
 * <b>Strateji:</b> Keycloak'ın kendi token revocation mekanizması kullanılır.
 * Redis blacklist yazılmaz (KART 01 kısıt maddesi).
 * <p>
 * Keycloak'ın logout endpoint'i refresh token'ı blacklist'e alır ve
 * ilgili session'daki tüm access token'ları geçersiz kılar.
 * Bu yaklaşım single-sign-out'u da destekler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RestClient keycloakRestClient;
    private final KeycloakProperties keycloakProperties;

    /**
     * Verilen refresh token ile Keycloak session'ını sonlandırır.
     * <p>
     * Başarılı çağrı sonrası Keycloak 204 döner. Hatalı token durumunda
     * Keycloak 400 dönebilir — her iki durumda da istemciye 200 döneriz
     * (logout idempotent olmalıdır; zaten geçersiz token'ı silmek hata sayılmaz).
     *
     * @param refreshToken Geçersiz kılınacak refresh token
     */
    public void logout(String refreshToken) {
        log.debug("Logout request received");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("refresh_token", refreshToken);

        try {
            keycloakRestClient.post()
                    .uri(keycloakProperties.getLogoutUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Keycloak session terminated successfully");

        } catch (HttpClientErrorException.BadRequest e) {
            // Token zaten geçersiz veya süresi dolmuş — logout idempotent kabul edilir
            log.warn("Logout called with invalid/expired refresh token — treating as successful logout");
        } catch (HttpClientErrorException e) {
            log.error("Keycloak logout failed with status {}: {}", e.getStatusCode(), e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Çıkış işlemi sırasında hata oluştu"
            );
        }
    }
}
