package com.telcox.springmicroservices.identity.service;

import com.telcox.springmicroservices.identity.client.CustomerServiceClient;
import com.telcox.springmicroservices.identity.client.dto.CustomerOtpInfo;
import com.telcox.springmicroservices.identity.config.KeycloakProperties;
import com.telcox.springmicroservices.identity.domain.OtpAttempt;
import com.telcox.springmicroservices.identity.domain.OtpAttemptRepository;
import com.telcox.springmicroservices.identity.dto.TokenResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Müşteri OTP tabanlı iki adımlı kimlik doğrulama akışını yönetir.
 * <p>
 * <b>Adım 1 — request-otp:</b>
 * <ol>
 *   <li>customer-service'ten telefon numarasına ait müşteri sorgulanır</li>
 *   <li>KYC onaylı değilse 404 döner</li>
 *   <li>6 haneli OTP üretilir ve Redis'e yazılır (key: {@code otp:{phone}}, TTL: 5 dk)</li>
 *   <li>OTP console'a loglanır (// TODO: SMS provider entegrasyonu)</li>
 * </ol>
 * <p>
 * <b>Adım 2 — verify-otp:</b>
 * <ol>
 *   <li>Redis'teki OTP ile karşılaştırılır</li>
 *   <li>Hatalı girişte otp_attempt.attempt_count artar; 3'te 5 dk kilit</li>
 *   <li>Eşleşirse Redis OTP silinir, deneme sayısı sıfırlanır</li>
 *   <li>customer-service'ten internalKeycloakPassword alınır</li>
 *   <li>Keycloak ROPC Grant ile müşteri adına token üretilir</li>
 * </ol>
 * <p>
 * <b>Token Stratejisi (KART 01 kararı):</b><br>
 * Keycloak Token Exchange veya impersonation yerine ROPC kullanılır.
 * KYC anında customer-service, müşteri için Keycloak'ta username=phone, password=internalPassword
 * ile kullanıcı oluşturur. Bu şifre customer-service'te şifreli saklanır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOtpService {

    /** OTP Redis key prefix. Format: otp:{phone} */
    private static final String OTP_KEY_PREFIX = "otp:";

    /** OTP Redis TTL: 5 dakika */
    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    /** Kilit için maksimum hatalı deneme sayısı */
    private static final int MAX_ATTEMPTS = 3;

    /** Kilit süresi (dakika) */
    private static final int LOCK_DURATION_MINUTES = 5;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final OtpAttemptRepository otpAttemptRepository;
    private final CustomerServiceClient customerServiceClient;
    private final RestClient keycloakRestClient;
    private final KeycloakProperties keycloakProperties;

    // ──────────────────────────────────────────────────────────────────────────
    // Adım 1: OTP İsteği
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Verilen telefon numarası için OTP üretir ve Redis'e yazar.
     *
     * @param phone E.164 formatında telefon numarası
     * @throws ResponseStatusException 404 — KYC onaylı müşteri bulunamazsa
     */
    public void requestOtp(String phone) {
        log.debug("OTP request for phone: {}", maskPhone(phone));

        // 1. Müşteriyi customer-service'ten sorgula
        CustomerOtpInfo customerInfo = fetchCustomerOrThrow(phone);

        if (!customerInfo.isKycApproved()) {
            log.warn("OTP request rejected — customer not KYC approved for phone: {}", maskPhone(phone));
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Bu numaraya kayıtlı onaylı müşteri bulunamadı"
            );
        }

        // 2. OTP üret
        String otp = generateOtp();

        // 3. Redis'e yaz (key: otp:{phone}, value: OTP, TTL: 5 dk)
        String redisKey = OTP_KEY_PREFIX + phone;
        redisTemplate.opsForValue().set(redisKey, otp, OTP_TTL);

        // 4. OTP'yi console'a logla — TODO: SMS provider entegrasyonu
        // Üretim ortamında bu log satırı KALDIRILMALI ve gerçek SMS servisi çağrılmalıdır.
        log.info(">>> [OTP SIMULATION] Phone: {} | OTP: {} | TTL: 5 dakika <<<",
                maskPhone(phone), otp);
        // TODO: smsProviderService.send(phone, "Doğrulama kodunuz: " + otp);

        log.debug("OTP generated and stored in Redis for phone: {}", maskPhone(phone));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Adım 2: OTP Doğrulama
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * OTP'yi doğrular ve başarılı olursa Keycloak token döner.
     *
     * @param phone E.164 formatında telefon numarası
     * @param otp   Kullanıcının girdiği 6 haneli OTP
     * @return Keycloak token bilgileri
     * @throws ResponseStatusException 401 — hatalı OTP veya kilitli numara
     * @throws ResponseStatusException 429 — çok fazla başarısız deneme
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TokenResponse verifyOtp(String phone, String otp) {
        log.debug("OTP verify attempt for phone: {}", maskPhone(phone));

        // 1. otp_attempt tablosundan kaydı al (yoksa oluştur)
        OtpAttempt attempt = otpAttemptRepository.findByPhone(phone)
                .orElseGet(() -> OtpAttempt.forPhone(phone));

        // 2. Kilit kontrolü
        if (attempt.isLocked()) {
            log.warn("OTP verify blocked — phone is locked: {}", maskPhone(phone));
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bu telefon numarası geçici olarak kilitlenmiştir. " +
                    "Lütfen birkaç dakika sonra tekrar deneyin."
            );
        }

        // 3. Redis'teki OTP'yi kontrol et
        String redisKey = OTP_KEY_PREFIX + phone;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            log.warn("OTP verify failed — OTP expired or not found for phone: {}", maskPhone(phone));
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "OTP süresi dolmuş veya geçersiz. Lütfen yeni OTP talep edin."
            );
        }

        // 4. OTP eşleşmiyorsa — hata sayacını artır
        if (!storedOtp.equals(otp)) {
            attempt.registerFailedAttempt(MAX_ATTEMPTS, LOCK_DURATION_MINUTES);
            otpAttemptRepository.save(attempt);

            int remainingAttempts = MAX_ATTEMPTS - attempt.getAttemptCount();
            log.warn("OTP mismatch for phone: {}. Remaining attempts: {}",
                    maskPhone(phone), Math.max(remainingAttempts, 0));

            if (attempt.isLocked()) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Çok fazla hatalı deneme. Telefon numarası 5 dakika kilitlendi."
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Hatalı OTP. Kalan deneme hakkı: " + Math.max(remainingAttempts, 0)
            );
        }

        // 5. OTP doğru — Redis'ten sil
        redisTemplate.delete(redisKey);
        attempt.resetAfterSuccess();
        otpAttemptRepository.save(attempt);
        log.debug("OTP verified successfully for phone: {}", maskPhone(phone));

        // 6. customer-service'ten tam bilgiyi al (internalKeycloakPassword dahil)
        CustomerOtpInfo customerInfo = fetchCustomerOrThrow(phone);

        // 7. Keycloak ROPC Grant ile müşteri adına token üret
        return generateCustomerToken(customerInfo);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Yardımcı metodlar
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * customer-service'ten müşteri bilgisini getirir.
     * 404 veya Feign hatası durumunda {@link ResponseStatusException} fırlatır.
     */
    private CustomerOtpInfo fetchCustomerOrThrow(String phone) {
        try {
            return customerServiceClient.getCustomerByPhone(phone);
        } catch (FeignException.NotFound e) {
            log.info("Customer not found for phone: {}", maskPhone(phone));
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Bu numaraya kayıtlı onaylı müşteri bulunamadı"
            );
        } catch (FeignException e) {
            log.error("customer-service call failed for phone {}: {}", maskPhone(phone), e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Müşteri servisi geçici olarak kullanılamıyor"
            );
        }
    }

    /**
     * Keycloak ROPC Grant ile müşteri adına token üretir.
     * username = phone, password = internalKeycloakPassword (KYC anında set edilmiş)
     */
    private TokenResponse generateCustomerToken(CustomerOtpInfo customerInfo) {
        log.debug("Generating Keycloak token for customer: {}", customerInfo.getKeycloakUserId());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("username", customerInfo.getPhone()); // username = phone (Keycloak'ta böyle oluşturuldu)
        form.add("password", customerInfo.getInternalKeycloakPassword());

        try {
            TokenResponse token = keycloakRestClient.post()
                    .uri(keycloakProperties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

            log.info("Customer token generated successfully for keycloakUserId: {}",
                    customerInfo.getKeycloakUserId());
            return token;

        } catch (HttpClientErrorException e) {
            log.error("Keycloak ROPC failed for customer {}: {}",
                    customerInfo.getKeycloakUserId(), e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Müşteri token üretiminde hata oluştu. Lütfen tekrar deneyin."
            );
        }
    }

    /**
     * 6 haneli OTP üretir (SecureRandom ile, kriptografik güvenli).
     */
    private String generateOtp() {
        int otp = SECURE_RANDOM.nextInt(900_000) + 100_000; // 100000-999999 arası
        return String.valueOf(otp);
    }

    /**
     * Log'larda telefon numarasını maskeler: 905XXXXXXX → 905XXX****
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 6) + "****";
    }
}
