package com.telcox.springmicroservices.orderservice.dto;

import com.telcox.springmicroservices.orderservice.domain.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private UUID id;
    private UUID productId;
    private String productCode;
    private ProductType productType;
    private Integer quantity;
    private BigDecimal unitPrice;
}
