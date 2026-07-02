package com.telcox.springmicroservices.identity.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * customer-service'ten OTP doğrulama için alınan müşteri bilgisi.
 * <p>
 * Bu DTO, customer-service'in {@code GET /api/v1/internal/customers/by-phone/{phone}}
 * endpoint'i tarafından dönülür. Endpoint implementasyonu KART 02/03 kapsamındadır;
 * bu sınıf sadece identity-service'in Feign client interface'i için tanımlanmıştır.
 * <p>
 * <b>Token Stratejisi (KART 01 kararı):</b><br>
 * KYC onayında (KART 03) customer-service, müşteri için Keycloak'ta
 * username=phone ile bir kullanıcı oluşturur ve rastgele bir internal şifre set eder.
 * Bu şifre {@code internalKeycloakPassword} alanında döner.
 * verify-otp başarılı olduğunda bu bilgiyle Keycloak ROPC Grant yapılır.
 */
@Data
@Schema(description = "Müşteri OTP bilgisi — customer-service tarafından sağlanır")
public class CustomerOtpInfo {

    @Schema(description = "Keycloak'taki kullanıcı UUID'si", example = "550e8400-e29b-41d4-a716-446655440000")
    private String keycloakUserId;

    @Schema(description = "E.164 formatında telefon numarası", example = "905001234567")
    private String phone;

    /**
     * KYC onayında customer-service tarafından Keycloak'a set edilen internal şifre.
     * <p>
     * Bu şifre son kullanıcıya hiçbir zaman açıklanmaz. Sadece identity-service'in
     * müşteri adına Keycloak ROPC Grant yapabilmesi için kullanılır.
     * <p>
     * Güvenlik notu: Bu değer customer-service DB'de AES-GCM ile şifreli saklanmalıdır.
     */
    @Schema(
        description = "Keycloak ROPC için kullanılan internal şifre (KYC anında set edilir)",
        example = "int_k3yc1oak_p@ss_abc123"
    )
    private String internalKeycloakPassword;

    /** KYC onaylı mı? {@code false} ise müşteri henüz giriş yapamaz. */
    @Schema(description = "Müşteri KYC onay durumu", example = "true")
    private boolean kycApproved;
}
