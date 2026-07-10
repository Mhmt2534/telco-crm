package com.telcox.springmicroservices.payment.service;

import com.telcox.springmicroservices.payment.dto.OrderCreatedEvent;

import com.telcox.springmicroservices.payment.dto.PaymentRefundRequestedEvent;

import com.telcox.springmicroservices.payment.dto.PaymentRequest;
import com.telcox.springmicroservices.payment.dto.PaymentResponse;

public interface PaymentService {
    void processPayment(OrderCreatedEvent event);
    void refundPayment(PaymentRefundRequestedEvent event);
    
    PaymentResponse initiatePayment(PaymentRequest request, String idempotencyKey, String customerId);
}
