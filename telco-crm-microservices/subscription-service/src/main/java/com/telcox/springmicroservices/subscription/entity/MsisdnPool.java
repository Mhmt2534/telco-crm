package com.telcox.springmicroservices.subscription.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "msisdn_pool")
public class MsisdnPool {

    @Id
    @Column(length = 20)
    private String msisdn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MsisdnStatus status;

    @Column(name = "reserved_until")
    private Instant reservedUntil;

    @Version
    @Column(nullable = false)
    private Long version;

    public MsisdnPool() {}

    public MsisdnPool(String msisdn, MsisdnStatus status, Instant reservedUntil, Long version) {
        this.msisdn = msisdn;
        this.status = status;
        this.reservedUntil = reservedUntil;
        this.version = version;
    }

    public String getMsisdn() { return msisdn; }

    public MsisdnStatus getStatus() { return status; }
    public void setStatus(MsisdnStatus status) { this.status = status; }

    public Instant getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }

    public Long getVersion() { return version; }
}
