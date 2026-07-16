package com.telcox.springmicroservices.orderservice.client.dto;

/** Values published by customer-service's CustomerResponse contract. */
public enum CustomerStatus {
    PENDING,
    ACTIVE,
    REJECTED
}
