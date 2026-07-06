package com.telcox.springmicroservices.payment.service;

import com.telcox.springmicroservices.payment.dto.OrderCreatedEvent;

public interface PaymentService {
    void processPayment(OrderCreatedEvent event);
}
