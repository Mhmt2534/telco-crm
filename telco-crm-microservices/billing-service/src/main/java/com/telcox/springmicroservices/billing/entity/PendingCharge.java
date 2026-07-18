package com.telcox.springmicroservices.billing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pending_charge")
public class PendingCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "customer_public_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "effective_bill_cycle", nullable = false)
    private String effectiveBillCycle;

    @Column(nullable = false)
    private String status; // PENDING, PENDING_NEXT, BILLED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PendingCharge() {
    }

    public PendingCharge(UUID orderId, UUID subscriptionId, UUID customerId, String description, BigDecimal amount, String effectiveBillCycle, String status) {
        this.orderId = orderId;
        this.subscriptionId = subscriptionId;
        this.customerId = customerId;
        this.description = description;
        this.amount = amount;
        this.effectiveBillCycle = effectiveBillCycle;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getEffectiveBillCycle() {
        return effectiveBillCycle;
    }

    public void setEffectiveBillCycle(String effectiveBillCycle) {
        this.effectiveBillCycle = effectiveBillCycle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
