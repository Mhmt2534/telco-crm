package com.telcox.springmicroservices.orderservice.dto;

import jakarta.validation.constraints.Min;
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
public class AddonRequest {
    
    @NotNull(message = "Customer ID cannot be null")
    private Long customerId;
    
    @NotNull(message = "Subscription ID cannot be null")
    private UUID subscriptionId;
    
    @NotBlank(message = "Addon Code cannot be blank")
    private String addonCode;
    
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
