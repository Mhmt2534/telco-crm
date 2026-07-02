package com.telcox.springmicroservices.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Keycloak token endpoint cevabının servis tarafından istemciye aktarılan
 * formu.
 * <p>
 * Keycloak'tan gelen tüm alanlar doğrudan iletilir; token içeriğine dokunulmaz.
 * Custom JWT üretimi yapılmaz — bu kararın gerekçesi: KART 01 kısıt maddesi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Keycloak'tan dönen token bilgileri")
public class TokenResponse {

    @JsonProperty("access_token")
    @Schema(description = "Keycloak access token (JWT)", example = "eyJhbGci...")
    private String accessToken;

    @JsonProperty("refresh_token")
    @Schema(description = "Keycloak refresh token")
    private String refreshToken;

    @JsonProperty("expires_in")
    @Schema(description = "Access token geçerlilik süresi (saniye)", example = "300")
    private Integer expiresIn;

    @JsonProperty("refresh_expires_in")
    @Schema(description = "Refresh token geçerlilik süresi (saniye)", example = "1800")
    private Integer refreshExpiresIn;

    @JsonProperty("token_type")
    @Schema(description = "Token tipi", example = "Bearer")
    private String tokenType;

    @JsonProperty("not-before-policy")
    @Schema(description = "Keycloak not-before-policy değeri")
    private Integer notBeforePolicy;

    @JsonProperty("session_state")
    @Schema(description = "Keycloak session state UUID")
    private String sessionState;

    @JsonProperty("scope")
    @Schema(description = "Verilen scope'lar", example = "openid profile email")
    private String scope;
}
