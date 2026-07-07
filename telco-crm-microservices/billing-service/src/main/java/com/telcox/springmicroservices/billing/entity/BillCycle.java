package com.telcox.springmicroservices.billing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill_cycle")
public class BillCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(nullable = false, length = 20)
    private String msisdn;

    @Column(name = "cut_off_day", nullable = false)
    private Integer cutOffDay;

    @Column(name = "fixed_amount", nullable = false)
    private BigDecimal fixedAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public BillCycle() {
    }

    public BillCycle(Long customerId, Long subscriptionId, String msisdn, Integer cutOffDay, BigDecimal fixedAmount) {
        this.customerId = customerId;
        this.subscriptionId = subscriptionId;
        this.msisdn = msisdn;
        this.cutOffDay = cutOffDay;
        this.fixedAmount = fixedAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public Integer getCutOffDay() {
        return cutOffDay;
    }

    public void setCutOffDay(Integer cutOffDay) {
        this.cutOffDay = cutOffDay;
    }

    public BigDecimal getFixedAmount() {
        return fixedAmount;
    }

    public void setFixedAmount(BigDecimal fixedAmount) {
        this.fixedAmount = fixedAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
