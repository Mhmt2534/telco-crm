package com.telcox.springmicroservices.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Admin ve Dealer (saha bayisi) girişi için request body.
 * <p>
 * Endpoint: {@code POST /api/v1/auth/staff/login}
 * <p>
 * Keycloak'ta önceden tanımlı ADMIN veya DEALER rolüne sahip kullanıcılar bu endpoint'i kullanır.
 */
@Data
@Schema(description = "Admin veya Dealer personeli giriş isteği")
public class StaffLoginRequest {

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Schema(description = "Keycloak kullanıcı adı", example = "dealer01")
    private String username;

    @NotBlank(message = "Şifre boş olamaz")
    @Schema(description = "Keycloak şifresi", example = "P@ssword123")
    private String password;
}
