package com.telcox.springmicroservices.usage.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** GET /api/v1/usage/subscriptions/{id}/quota yanıtı */
public class QuotaResponse {

    private UUID   subscriptionId;

    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;

    // Toplam (başlangıç) haklar
    private Integer totalMinutes;
    private Integer totalSms;
    private Long    totalMb;

    // Anlık kalan haklar
    private Integer minutesRemaining;
    private Integer smsRemaining;
    private Long    mbRemaining;

    // Kullanım yüzdesi (hesaplanmış)
    private Integer mbUsagePercent;

    public QuotaResponse() {}

    // ── Getters / Setters ─────────────────────────────────────

    public UUID getSubscriptionId()                         { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId)      { this.subscriptionId = subscriptionId; }

    public OffsetDateTime getPeriodStart()                  { return periodStart; }
    public void setPeriodStart(OffsetDateTime periodStart)  { this.periodStart = periodStart; }

    public OffsetDateTime getPeriodEnd()                    { return periodEnd; }
    public void setPeriodEnd(OffsetDateTime periodEnd)      { this.periodEnd = periodEnd; }

    public Integer getTotalMinutes()                        { return totalMinutes; }
    public void setTotalMinutes(Integer totalMinutes)       { this.totalMinutes = totalMinutes; }

    public Integer getTotalSms()                            { return totalSms; }
    public void setTotalSms(Integer totalSms)               { this.totalSms = totalSms; }

    public Long getTotalMb()                                { return totalMb; }
    public void setTotalMb(Long totalMb)                    { this.totalMb = totalMb; }

    public Integer getMinutesRemaining()                    { return minutesRemaining; }
    public void setMinutesRemaining(Integer minutesRemaining){ this.minutesRemaining = minutesRemaining; }

    public Integer getSmsRemaining()                        { return smsRemaining; }
    public void setSmsRemaining(Integer smsRemaining)       { this.smsRemaining = smsRemaining; }

    public Long getMbRemaining()                            { return mbRemaining; }
    public void setMbRemaining(Long mbRemaining)            { this.mbRemaining = mbRemaining; }

    public Integer getMbUsagePercent()                      { return mbUsagePercent; }
    public void setMbUsagePercent(Integer mbUsagePercent)   { this.mbUsagePercent = mbUsagePercent; }
}
