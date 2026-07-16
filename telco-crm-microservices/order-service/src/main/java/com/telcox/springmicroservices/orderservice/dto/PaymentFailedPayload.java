package com.telcox.springmicroservices.orderservice.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PaymentFailedPayload {
    private UUID orderId;
    private String errorCode;
    private String errorDescription;
}
