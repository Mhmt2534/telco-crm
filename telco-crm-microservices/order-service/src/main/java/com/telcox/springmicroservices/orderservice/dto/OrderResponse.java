package com.telcox.springmicroservices.orderservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.telcox.springmicroservices.orderservice.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID id;
    private UUID customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemResponse> items;
    
    // Saga state will be null for this card as requested
    private JsonNode sagaState;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
