package com.telcox.springmicroservices.usage.entity;

import java.time.Instant;
import java.util.UUID;

import com.telcox.common.persistence.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Kafka'dan gelen ham CDR (Call Detail Record) kayıtlarının logu.
 *
 * cdr_ref: Kafka mesajındaki benzersiz referans numarasıdır.
 * Aynı CDR'nin tekrar işlenmesini (idempotency) önler.
 *
 * msisdn: Kafka partition key olarak kullanılır; sorgu kolaylığı
 * açısından burada da saklanır.
 */
@Entity
@Table(name = "usage_records")
public class UsageRecord extends BaseEntity {

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    /** Kafka partition key — sorgu kolaylığı için tutulur. */
    @Column(nullable = false, length = 20)
    private String msisdn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UsageType type;

    /** Tüketilen miktar (dakika / adet / MB). */
    @Column(nullable = false)
    private Double quantity;

    /** Kafka CDR referansı — idempotency için UNIQUE. */
    @Column(name = "cdr_ref", length = 100, unique = true)
    private String cdrRef;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public UsageRecord() {}

    public UsageRecord(UUID subscriptionId,
                       String msisdn,
                       UsageType type,
                       Double quantity,
                       String cdrRef,
                       Instant recordedAt) {
        this.subscriptionId = subscriptionId;
        this.msisdn         = msisdn;
        this.type           = type;
        this.quantity       = quantity;
        this.cdrRef         = cdrRef;
        this.recordedAt     = recordedAt;
    }

    // ── Getters / Setters ─────────────────────────────────────

    public UUID getSubscriptionId()                        { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId)     { this.subscriptionId = subscriptionId; }

    public String getMsisdn()                              { return msisdn; }
    public void setMsisdn(String msisdn)                   { this.msisdn = msisdn; }

    public UsageType getType()                             { return type; }
    public void setType(UsageType type)                    { this.type = type; }

    public Double getQuantity()                            { return quantity; }
    public void setQuantity(Double quantity)               { this.quantity = quantity; }

    public String getCdrRef()                              { return cdrRef; }
    public void setCdrRef(String cdrRef)                   { this.cdrRef = cdrRef; }

    public Instant getRecordedAt()                  { return recordedAt; }
    public void setRecordedAt(Instant recordedAt)   { this.recordedAt = recordedAt; }
}
