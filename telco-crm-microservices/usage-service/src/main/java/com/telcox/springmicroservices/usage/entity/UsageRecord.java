package com.telcox.springmicroservices.usage.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.telcox.common.persistence.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "usage_records")
public class UsageRecord extends BaseEntity {

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false, length = 20)
    private String msisdn;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UsageType type;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    public UsageRecord() {}

    public UsageRecord(UUID subscriptionId, String msisdn, Double amount, UsageType type, OffsetDateTime recordedAt) {
        this.subscriptionId = subscriptionId;
        this.msisdn = msisdn;
        this.amount = amount;
        this.type = type;
        this.recordedAt = recordedAt;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public UsageType getType() {
        return type;
    }

    public void setType(UsageType type) {
        this.type = type;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
