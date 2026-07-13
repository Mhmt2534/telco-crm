package com.telcox.springmicroservices.orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    private UUID id;
    private Long customerId;
    private String msisdn;
    private String tariffCode;
    private String status;
    private Instant activatedAt;
    private Instant terminatedAt;
    private Instant createdAt;
}
