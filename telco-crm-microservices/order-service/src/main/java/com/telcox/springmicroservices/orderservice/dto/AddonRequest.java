package com.telcox.springmicroservices.orderservice.dto;

import jakarta.validation.constraints.Min;
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
public class AddonRequest {
    
    private UUID customerId;
    
    @NotNull(message = "Subscription ID cannot be null")
    private UUID subscriptionId;
    
    @NotNull(message = "Addon ID cannot be null")
    private UUID addonId;
    
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
