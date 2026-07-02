package com.telcox.springmicroservices.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Müşteri OTP talebi için request body.
 * <p>
 * Endpoint: {@code POST /api/v1/auth/customer/request-otp}
 */
@Data
@Schema(description = "Müşteri OTP talep isteği")
public class OtpRequest {

    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(
        regexp = "^9[0-9]{11}$",
        message = "Telefon numarası E.164 formatında olmalıdır (örn: 905001234567)"
    )
    @Schema(
        description = "E.164 formatında telefon numarası (ülke kodu dahil, + işareti olmadan)",
        example = "905001234567"
    )
    private String phone;
}
