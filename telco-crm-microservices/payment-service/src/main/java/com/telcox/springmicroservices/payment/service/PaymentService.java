package com.telcox.springmicroservices.payment.service;

import com.telcox.springmicroservices.payment.dto.OrderCreatedEvent;

import com.telcox.springmicroservices.payment.dto.PaymentRefundRequestedEvent;

public interface PaymentService {
    void processPayment(OrderCreatedEvent event);
    void refundPayment(PaymentRefundRequestedEvent event);
}
