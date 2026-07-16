package com.telcox.springmicroservices.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TariffChangeRequest {
    
    @NotNull(message = "Customer ID cannot be null")
    private UUID customerId;
    
    @NotNull(message = "Subscription ID cannot be null")
    private UUID subscriptionId;
    
    @NotNull(message = "New Tariff ID cannot be null")
    private UUID newTariffId;
}
