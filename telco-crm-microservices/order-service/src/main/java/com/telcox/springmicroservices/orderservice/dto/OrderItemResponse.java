package com.telcox.springmicroservices.orderservice.dto;

import com.telcox.springmicroservices.orderservice.domain.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private Long id;
    private String productCode;
    private ProductType productType;
    private Integer quantity;
    private BigDecimal unitPrice;
}
