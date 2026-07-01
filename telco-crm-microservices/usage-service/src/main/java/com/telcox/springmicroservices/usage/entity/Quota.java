package com.telcox.springmicroservices.usage.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.telcox.common.persistence.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Bir aboneliğin aktif fatura dönemi kota sayacı.
 *
 * total_* alanları: tarife başlangıcında atanan toplam hak miktarlarıdır.
 * Kalan (*_remaining) değerleri ile karşılaştırılarak %80 / %100 eşik
 * bildirimleri hesaplanır.
 */
@Entity
@Table(name = "quotas")
public class Quota extends BaseEntity {

    @Column(name = "subscription_id", nullable = false, unique = true)
    private UUID subscriptionId;

    // ── Dönem bilgisi ─────────────────────────────────────────
    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private OffsetDateTime periodEnd;

    // ── Başlangıç (toplam) haklar  →  eşik hesabı için ───────
    @Column(name = "total_minutes", nullable = false)
    private Integer totalMinutes;

    @Column(name = "total_sms", nullable = false)
    private Integer totalSms;

    @Column(name = "total_mb", nullable = false)
    private Long totalMb;

    // ── Anlık kalan haklar ────────────────────────────────────
    @Column(name = "minutes_remaining", nullable = false)
    private Integer minutesRemaining;

    @Column(name = "sms_remaining", nullable = false)
    private Integer smsRemaining;

    @Column(name = "mb_remaining", nullable = false)
    private Long mbRemaining;

    public Quota() {}

    public Quota(UUID subscriptionId,
                 OffsetDateTime periodStart,
                 OffsetDateTime periodEnd,
                 Integer totalMinutes,
                 Integer totalSms,
                 Long totalMb) {
        this.subscriptionId   = subscriptionId;
        this.periodStart      = periodStart;
        this.periodEnd        = periodEnd;
        this.totalMinutes     = totalMinutes;
        this.totalSms         = totalSms;
        this.totalMb          = totalMb;
        // Başlangıçta kalan = toplam
        this.minutesRemaining = totalMinutes;
        this.smsRemaining     = totalSms;
        this.mbRemaining      = totalMb;
    }

    // ── Getters / Setters ─────────────────────────────────────

    public UUID getSubscriptionId()                        { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId)     { this.subscriptionId = subscriptionId; }

    public OffsetDateTime getPeriodStart()                 { return periodStart; }
    public void setPeriodStart(OffsetDateTime periodStart) { this.periodStart = periodStart; }

    public OffsetDateTime getPeriodEnd()                   { return periodEnd; }
    public void setPeriodEnd(OffsetDateTime periodEnd)     { this.periodEnd = periodEnd; }

    public Integer getTotalMinutes()                       { return totalMinutes; }
    public void setTotalMinutes(Integer totalMinutes)      { this.totalMinutes = totalMinutes; }

    public Integer getTotalSms()                           { return totalSms; }
    public void setTotalSms(Integer totalSms)              { this.totalSms = totalSms; }

    public Long getTotalMb()                               { return totalMb; }
    public void setTotalMb(Long totalMb)                   { this.totalMb = totalMb; }

    public Integer getMinutesRemaining()                   { return minutesRemaining; }
    public void setMinutesRemaining(Integer v)             { this.minutesRemaining = v; }

    public Integer getSmsRemaining()                       { return smsRemaining; }
    public void setSmsRemaining(Integer v)                 { this.smsRemaining = v; }

    public Long getMbRemaining()                           { return mbRemaining; }
    public void setMbRemaining(Long v)                     { this.mbRemaining = v; }

    // ── Yardımcı: %80 / %100 eşik kontrolü ───────────────────

    /** MB kullanımının yüzde kaçının harcandığını döner (0-100). */
    public int mbUsagePercent() {
        if (totalMb == 0) return 100;
        return (int) (((totalMb - mbRemaining) * 100L) / totalMb);
    }

    public boolean isMbAt80Percent()  { return mbUsagePercent() >= 80; }
    public boolean isMbAt100Percent() { return mbRemaining <= 0; }
}
