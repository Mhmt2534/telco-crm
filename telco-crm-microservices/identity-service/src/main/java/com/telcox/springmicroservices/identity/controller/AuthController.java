package com.telcox.springmicroservices.identity.controller;

import com.telcox.springmicroservices.identity.dto.*;
import com.telcox.springmicroservices.identity.service.CustomerOtpService;
import com.telcox.springmicroservices.identity.service.LogoutService;
import com.telcox.springmicroservices.identity.service.StaffAuthService;
import com.telcox.springmicroservices.identity.service.TokenRefreshService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kimlik doğrulama endpoint'leri.
 * <p>
 * Tüm endpoint'ler unauthenticated kullanıcılar tarafından çağrılır (SecurityConfig'de permitAll).
 * Token doğrulaması bu serviste değil, API Gateway (KART 13) katmanında yapılır.
 * <p>
 * <b>Test için curl örnekleri README kısmında yer almaktadır.</b>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Kimlik doğrulama endpoint'leri — Staff login ve Customer OTP akışı")
public class AuthController {

    private final StaffAuthService staffAuthService;
    private final CustomerOtpService customerOtpService;
    private final LogoutService logoutService;
    private final TokenRefreshService tokenRefreshService;

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Staff Login (Admin / Dealer)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Admin ve Saha Bayisi (DEALER) girişi.
     *
     * <pre>
     * # Örnek istek:
     * curl -X POST http://localhost:9001/api/v1/auth/staff/login \
     *   -H "Content-Type: application/json" \
     *   -d '{"username":"admin","password":"admin123"}'
     *
     * # Başarılı yanıt (200):
     * {
     *   "access_token": "eyJhbGci...",
     *   "refresh_token": "eyJhbGci...",
     *   "expires_in": 300,
     *   "token_type": "Bearer"
     * }
     * </pre>
     */
    @Operation(
        summary = "Admin / Dealer girişi",
        description = "Keycloak ROPC Grant üzerinden staff kullanıcısı için token üretir. " +
                      "Keycloak'ta 'Direct Access Grants' aktif olmalıdır."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Başarılı giriş",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(responseCode = "400", description = "Geçersiz istek formatı"),
        @ApiResponse(responseCode = "401", description = "Hatalı kullanıcı adı veya şifre")
    })
    @PostMapping("/staff/login")
    public ResponseEntity<TokenResponse> staffLogin(
            @Valid @RequestBody StaffLoginRequest request) {

        log.info("Staff login request for user: {}", request.getUsername());
        TokenResponse token = staffAuthService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(token);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Müşteri OTP — Adım 1: OTP Talebi
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Müşteri için OTP gönderim isteği (Adım 1).
     *
     * <pre>
     * # Örnek istek:
     * curl -X POST http://localhost:9001/api/v1/auth/customer/request-otp \
     *   -H "Content-Type: application/json" \
     *   -d '{"phone":"905001234567"}'
     *
     * # Başarılı yanıt (200): {}
     * # Müşteri bulunamazsa (404):
     * { "status": 404, "detail": "Bu numaraya kayıtlı onaylı müşteri bulunamadı" }
     * </pre>
     */
    @Operation(
        summary = "Müşteri OTP talebi (Adım 1)",
        description = "KYC onaylı müşteri için 6 haneli OTP üretir ve (şimdilik) console'a loglar. " +
                      "TODO: SMS provider entegrasyonu. OTP TTL: 5 dakika."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP başarıyla gönderildi (simüle)"),
        @ApiResponse(responseCode = "400", description = "Geçersiz telefon formatı"),
        @ApiResponse(responseCode = "404", description = "KYC onaylı müşteri bulunamadı"),
        @ApiResponse(responseCode = "503", description = "Müşteri servisi kullanılamıyor")
    })
    @PostMapping("/customer/request-otp")
    public ResponseEntity<Void> requestOtp(
            @Valid @RequestBody OtpRequest request) {

        log.info("OTP request for phone: {}****", request.getPhone().substring(0, 6));
        customerOtpService.requestOtp(request.getPhone());
        return ResponseEntity.ok().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Müşteri OTP — Adım 2: OTP Doğrulama
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * OTP doğrulama ve Keycloak token üretimi (Adım 2).
     *
     * <pre>
     * # Örnek istek:
     * curl -X POST http://localhost:9001/api/v1/auth/customer/verify-otp \
     *   -H "Content-Type: application/json" \
     *   -d '{"phone":"905001234567","otp":"123456"}'
     *
     * # Başarılı yanıt (200): TokenResponse (access_token, refresh_token, ...)
     * # Hatalı OTP (401): { "detail": "Hatalı OTP. Kalan deneme hakkı: 2" }
     * # Kilitli numara (401): { "detail": "Telefon numarası 5 dakika kilitlendi." }
     * </pre>
     */
    @Operation(
        summary = "Müşteri OTP doğrulama (Adım 2)",
        description = "OTP'yi doğrular ve başarılı olursa Keycloak'tan müşteri token'ı döner. " +
                      "3 hatalı denemede telefon 5 dakika kilitlenir."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP doğrulandı, token döndürüldü",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(responseCode = "400", description = "Geçersiz istek formatı"),
        @ApiResponse(responseCode = "401", description = "Hatalı OTP veya kilitli numara"),
        @ApiResponse(responseCode = "404", description = "Müşteri bulunamadı")
    })
    @PostMapping("/customer/verify-otp")
    public ResponseEntity<TokenResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {

        log.info("OTP verify request for phone: {}****", request.getPhone().substring(0, 6));
        TokenResponse token = customerOtpService.verifyOtp(request.getPhone(), request.getOtp());
        return ResponseEntity.ok(token);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Token Refresh
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Refresh token kullanarak yeni bir access token alır.
     *
     * <pre>
     * # Örnek istek:
     * curl -X POST http://localhost:9001/api/v1/auth/refresh \
     *   -H "Content-Type: application/json" \
     *   -d '{"refreshToken":"eyJhbGci..."}'
     *
     * # Başarılı yanıt (200):
     * {
     *   "access_token": "eyJhbGci...",
     *   "refresh_token": "eyJhbGci...",
     *   "expires_in": 300,
     *   "token_type": "Bearer"
     * }
     * </pre>
     */
    @Operation(
        summary = "Token Yenileme",
        description = "Geçerli bir refresh token ile Keycloak üzerinden yeni access token ve refresh token üretir."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token başarıyla yenilendi",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(responseCode = "400", description = "Geçersiz istek formatı"),
        @ApiResponse(responseCode = "401", description = "Refresh token geçersiz veya süresi dolmuş")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {

        log.info("Token refresh request received");
        TokenResponse token = tokenRefreshService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(token);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Logout
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Keycloak oturumunu sonlandırır.
     *
     * <pre>
     * # Örnek istek:
     * curl -X POST http://localhost:9001/api/v1/auth/logout \
     *   -H "Content-Type: application/json" \
     *   -d '{"refresh_token":"eyJhbGci..."}'
     *
     * # Başarılı yanıt (204): boş
     * </pre>
     */
    @Operation(
        summary = "Oturum kapat",
        description = "Refresh token'ı Keycloak'a göndererek oturumu sonlandırır. " +
                      "Redis blacklist kullanılmaz — Keycloak'ın kendi session invalidation'ına güvenilir."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Oturum başarıyla sonlandırıldı"),
        @ApiResponse(responseCode = "400", description = "Geçersiz istek formatı")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request) {

        log.info("Logout request received");
        logoutService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
