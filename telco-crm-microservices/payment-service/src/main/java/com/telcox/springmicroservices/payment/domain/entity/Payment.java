package com.telcox.springmicroservices.payment.domain.entity;

import com.telcox.common.persistence.entity.BaseEntity;
import com.telcox.springmicroservices.payment.domain.enums.PaymentMethod;
import com.telcox.springmicroservices.payment.domain.enums.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(name = "invoice_id", length = 100)
    private String invoiceId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 50)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;

    @Column(name = "external_ref", length = 100)
    private String externalRef;

    @Column(name = "payment_request_id", unique = true, length = 100)
    private String paymentRequestId;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentAttempt> attempts = new ArrayList<>();

    public Payment() {
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getPaymentRequestId() {
        return paymentRequestId;
    }

    public void setPaymentRequestId(String paymentRequestId) {
        this.paymentRequestId = paymentRequestId;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public List<PaymentAttempt> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<PaymentAttempt> attempts) {
        this.attempts = attempts;
    }

    public void addAttempt(PaymentAttempt attempt) {
        attempts.add(attempt);
        attempt.setPayment(this);
    }
}
