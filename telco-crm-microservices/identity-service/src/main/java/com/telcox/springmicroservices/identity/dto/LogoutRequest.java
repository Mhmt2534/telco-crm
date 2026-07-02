package com.telcox.springmicroservices.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Logout isteği için request body.
 * <p>
 * Endpoint: {@code POST /api/v1/auth/logout}
 * <p>
 * Keycloak'ın kendi session invalidation mekanizması kullanılır.
 * Redis blacklist yazılmaz (KART 01 kısıt maddesi).
 */
@Data
@Schema(description = "Logout isteği — refresh token ile Keycloak oturumunu sonlandırır")
public class LogoutRequest {

    @NotBlank(message = "Refresh token boş olamaz")
    @JsonProperty("refresh_token")
    @Schema(description = "Geçerli Keycloak refresh token", example = "eyJhbGci...")
    private String refreshToken;
}
