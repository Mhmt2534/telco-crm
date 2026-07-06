package com.telcox.springmicroservices.payment.domain.entity;

import com.telcox.common.persistence.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "response")
    private String response;

    public PaymentAttempt() {
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
