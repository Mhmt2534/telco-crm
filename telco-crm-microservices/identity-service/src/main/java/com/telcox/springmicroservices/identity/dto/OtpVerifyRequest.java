package com.telcox.springmicroservices.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Müşteri OTP doğrulama isteği.
 * <p>
 * Endpoint: {@code POST /api/v1/auth/customer/verify-otp}
 * <p>
 * Başarılı doğrulama sonucunda {@link TokenResponse} dönülür.
 * 3 başarısız denemede telefon 5 dakika kilitlenir.
 */
@Data
@Schema(description = "Müşteri OTP doğrulama isteği")
public class OtpVerifyRequest {

    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(
        regexp = "^9[0-9]{11}$",
        message = "Telefon numarası E.164 formatında olmalıdır (örn: 905001234567)"
    )
    @Schema(description = "E.164 formatında telefon numarası", example = "905001234567")
    private String phone;

    @NotBlank(message = "OTP kodu boş olamaz")
    @Size(min = 6, max = 6, message = "OTP kodu 6 haneli olmalıdır")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP kodu sadece rakam içermelidir")
    @Schema(description = "6 haneli OTP kodu", example = "123456")
    private String otp;
}
