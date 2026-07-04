package com.telcox.springmicroservices.orderservice.domain.enums;

public enum OrderStatus {
    DRAFT,
    PENDING_PAYMENT,
    PAID,
    FULFILLED,
    CANCELLED
}
