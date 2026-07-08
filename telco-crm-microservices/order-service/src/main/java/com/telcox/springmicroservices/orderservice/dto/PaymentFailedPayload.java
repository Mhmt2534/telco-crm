package com.telcox.springmicroservices.orderservice.dto;

import lombok.Data;

@Data
public class PaymentFailedPayload {
    private Long orderId;
    private String errorCode;
    private String errorDescription;
}
