package com.telcox.springmicroservices.usage.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.telcox.common.persistence.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "quotas")
public class Quota extends BaseEntity {

    @Column(name = "subscription_id", nullable = false, unique = true)
    private UUID subscriptionId;

    @Column(name = "minutes_remaining", nullable = false)
    private Integer minutesRemaining;

    @Column(name = "sms_remaining", nullable = false)
    private Integer smsRemaining;

    @Column(name = "mb_remaining", nullable = false)
    private Long mbRemaining;

    @Column(name = "period_end", nullable = false)
    private OffsetDateTime periodEnd;

    public Quota() {}

    public Quota(UUID subscriptionId, Integer minutesRemaining, Integer smsRemaining, Long mbRemaining, OffsetDateTime periodEnd) {
        this.subscriptionId = subscriptionId;
        this.minutesRemaining = minutesRemaining;
        this.smsRemaining = smsRemaining;
        this.mbRemaining = mbRemaining;
        this.periodEnd = periodEnd;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Integer getMinutesRemaining() {
        return minutesRemaining;
    }

    public void setMinutesRemaining(Integer minutesRemaining) {
        this.minutesRemaining = minutesRemaining;
    }

    public Integer getSmsRemaining() {
        return smsRemaining;
    }

    public void setSmsRemaining(Integer smsRemaining) {
        this.smsRemaining = smsRemaining;
    }

    public Long getMbRemaining() {
        return mbRemaining;
    }

    public void setMbRemaining(Long mbRemaining) {
        this.mbRemaining = mbRemaining;
    }

    public OffsetDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(OffsetDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }
}
