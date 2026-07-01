package com.telcox.springmicroservices.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * OTP deneme kayıtlarına erişim için repository.
 * <p>
 * Telefon başına tek kayıt tutulur; kayıt yoksa service katmanı yeni kayıt oluşturur.
 */
@Repository
public interface OtpAttemptRepository extends JpaRepository<OtpAttempt, Long> {

    /**
     * Verilen telefon numarasına ait OTP deneme kaydını getirir.
     *
     * @param phone E.164 formatında telefon numarası
     * @return Optional OTP attempt kaydı
     */
    Optional<OtpAttempt> findByPhone(String phone);
}
