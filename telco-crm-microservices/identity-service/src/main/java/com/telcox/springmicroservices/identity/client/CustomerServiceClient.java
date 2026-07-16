package com.telcox.springmicroservices.identity.client;

import com.telcox.springmicroservices.identity.client.dto.CustomerOtpInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * customer-service ile iletişim için Feign deklaratif HTTP client stub'ı.
 * <p>
 * <b>Bu interface identity-service'e ait bir STUB'dır.</b><br>
 * Gerçek implementasyon customer-service tarafında KART 02/03 kapsamında yapılacaktır.
 * Bu aşamada customer-service henüz bu endpoint'i sunmuyorsa, Feign 404 döner ve
 * {@link CustomerOtpService} bunu {@code CustomerNotFoundException}'a çevirir.
 * <p>
 * <b>customer-service'in implement etmesi gereken endpoint:</b><br>
 * {@code GET /api/v1/internal/customers/by-phone/{phone}}<br>
 * Cevap: {@link CustomerOtpInfo}
 * <p>
 * Güvenlik: Bu endpoint yalnızca servis ağı içinden (telco-net) erişilebilir olmalıdır.
 * Gateway üzerinden dışarı açılmamalıdır.
 */
@FeignClient(
    name = "customer-service",
    path = "/api/v1/internal"
)
public interface CustomerServiceClient {

    /**
     * Telefon numarasına göre müşterinin Keycloak bilgilerini sorgular.
     * <p>
     * KYC onaylı ve Keycloak'a kaydedilmiş müşteriler için {@link CustomerOtpInfo} döner.
     * Müşteri bulunamazsa veya KYC onaylı değilse customer-service 404 döner
     * ve Feign {@code FeignException.NotFound} fırlatır.
     *
     * @param phone customer-service'in yerel saklama formatındaki telefon numarası
     *              (örn: 5001234567)
     * @return Müşterinin Keycloak bilgileri
     * @throws feign.FeignException.NotFound Müşteri bulunamazsa veya KYC onaylı değilse
     */
    @GetMapping("/customers/by-phone/{phone}")
    CustomerOtpInfo getCustomerByPhone(@PathVariable("phone") String phone);
}
