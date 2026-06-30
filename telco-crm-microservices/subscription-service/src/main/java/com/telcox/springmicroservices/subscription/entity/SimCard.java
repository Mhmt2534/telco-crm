package com.telcox.springmicroservices.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "sim_cards")
public class SimCard {

    @Id
    @Column(length = 30)
    private String iccid;

    @Column(nullable = false, unique = true, length = 30)
    private String imsi;

    @Column(unique = true, length = 20)
    private String msisdn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SimCardStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    public SimCard() {}

    public String getIccid() { return iccid; }
    public void setIccid(String iccid) { this.iccid = iccid; }

    public String getImsi() { return imsi; }
    public void setImsi(String imsi) { this.imsi = imsi; }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public SimCardStatus getStatus() { return status; }
    public void setStatus(SimCardStatus status) { this.status = status; }

    public Long getVersion() { return version; }
}
