package com.telcox.springmicroservices.orderservice.mapper;

import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import com.telcox.springmicroservices.orderservice.domain.entity.OrderItem;
import com.telcox.springmicroservices.orderservice.domain.entity.SagaState;
import com.telcox.springmicroservices.orderservice.dto.OrderItemRequest;
import com.telcox.springmicroservices.orderservice.dto.OrderItemResponse;
import com.telcox.springmicroservices.orderservice.dto.OrderRequest;
import com.telcox.springmicroservices.orderservice.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, 
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(OrderRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    OrderItem toEntity(OrderItemRequest request);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "sagaState", ignore = true)
    OrderResponse toResponse(Order order);

    @Mapping(target = "id", source = "publicId")
    OrderItemResponse toResponse(OrderItem item);
    
    // Helper default method to enrich response if sagaState is available later
    default OrderResponse toResponse(Order order, SagaState sagaState) {
        OrderResponse response = toResponse(order);
        if (sagaState != null) {
            response.setSagaState(sagaState.getPayload());
        }
        return response;
    }
}
