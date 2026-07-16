package com.telcox.springmicroservices.payment.service;

import com.telcox.springmicroservices.payment.dto.OrderCreatedEvent;

import com.telcox.springmicroservices.payment.dto.PaymentRefundRequestedEvent;

import com.telcox.springmicroservices.payment.dto.PaymentRequest;
import com.telcox.springmicroservices.payment.dto.PaymentResponse;
import java.util.UUID;

public interface PaymentService {
    void processPayment(OrderCreatedEvent event);
    void refundPayment(PaymentRefundRequestedEvent event);
    
    PaymentResponse initiatePayment(PaymentRequest request, String idempotencyKey, String actorId);
    
    com.telcox.springmicroservices.payment.dto.WalletBalanceResponse getWalletBalance(UUID customerId);
    com.telcox.springmicroservices.payment.dto.WalletBalanceResponse topUpWallet(UUID customerId, com.telcox.springmicroservices.payment.dto.WalletTopUpRequest request);
}
