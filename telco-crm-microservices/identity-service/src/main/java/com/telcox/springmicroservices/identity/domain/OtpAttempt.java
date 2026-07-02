package com.telcox.springmicroservices.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Telefon başına OTP deneme sayısını ve kilit durumunu tutan entity.
 * <p>
 * Tablo: {@code otp_attempt} (Flyway V1 migration ile oluşturulur)
 * <p>
 * Kural: 3 başarısız denemede {@code lockedUntil} = şimdiki zaman + 5 dakika.
 * Kilidin dolmasıyla birlikte tekrar deneme yapılabilir; başarılı doğrulamada
 * {@code attemptCount} sıfırlanır ve {@code lockedUntil} temizlenir.
 * <p>
 * NOT: BaseEntity extend edilmez — audit alanları (created_by, version vb.) bu
 * tabloda gereksizdir ve gereksiz JOIN/lock karmaşıklığı yaratır.
 */
@Entity
@Table(name = "otp_attempt")
@Getter
@Setter
@NoArgsConstructor
public class OtpAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * E.164 formatında telefon numarası. Unique index ile korunur.
     * Bir telefon numarası için tek kayıt tutulur, upsert mantığıyla güncellenir.
     */
    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    /**
     * Son başarılı doğrulamadan itibaren art arda gelen başarısız deneme sayısı.
     * 3'e ulaştığında {@code lockedUntil} set edilir.
     */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    /**
     * Bu zaman damgasına kadar telefon numarası kilitlidir.
     * {@code null} ise kilit yok demektir.
     */
    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Yeni kayıt oluştururken audit timestamp'lerini set eder. */
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    /** Her güncelleme öncesi updated_at alanını yeniler. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Verilen telefon için yeni bir deneme kaydı oluşturur.
     *
     * @param phone E.164 formatında telefon numarası
     * @return Başlatılmış {@link OtpAttempt} nesnesi
     */
    public static OtpAttempt forPhone(String phone) {
        OtpAttempt attempt = new OtpAttempt();
        attempt.setPhone(phone);
        attempt.setAttemptCount(0);
        return attempt;
    }

    /**
     * Telefon numarasının şu anda kilitli olup olmadığını kontrol eder.
     *
     * @return {@code true} ise kilit süresi dolmamış — giriş engellenir
     */
    public boolean isLocked() {
        return lockedUntil != null && OffsetDateTime.now().isBefore(lockedUntil);
    }

    /**
     * Başarısız bir deneme kaydeder. 3. başarısız denemede kilidi aktif eder.
     *
     * @param maxAttempts Kilitleme için gereken maksimum deneme sayısı
     * @param lockDurationMinutes Kilit süresi (dakika)
     */
    public void registerFailedAttempt(int maxAttempts, int lockDurationMinutes) {
        this.attemptCount++;
        if (this.attemptCount >= maxAttempts) {
            this.lockedUntil = OffsetDateTime.now().plusMinutes(lockDurationMinutes);
        }
    }

    /**
     * Başarılı doğrulama sonrası deneme sayısını ve kilidi sıfırlar.
     */
    public void resetAfterSuccess() {
        this.attemptCount = 0;
        this.lockedUntil = null;
    }
}
