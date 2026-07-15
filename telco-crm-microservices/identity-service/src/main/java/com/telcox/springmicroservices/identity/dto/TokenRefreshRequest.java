package com.telcox.springmicroservices.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh Token isteği için request body.
 * <p>
 * Endpoint: {@code POST /api/v1/auth/refresh}
 */
@Data
@Schema(description = "Refresh Token isteği")
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh token boş olamaz")
    @JsonProperty("refreshToken")
    @Schema(description = "Geçerli Keycloak refresh token", example = "eyJhbGci...")
    private String refreshToken;
}
