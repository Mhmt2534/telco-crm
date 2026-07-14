package com.telcox.springmicroservices.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
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
    private Long customerId;
    
    @NotNull(message = "Subscription ID cannot be null")
    private UUID subscriptionId;
    
    @NotBlank(message = "New Tariff Code cannot be blank")
    private String newTariffCode;
}
