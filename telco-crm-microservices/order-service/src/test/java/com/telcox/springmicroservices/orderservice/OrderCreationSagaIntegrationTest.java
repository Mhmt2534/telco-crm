package com.telcox.springmicroservices.orderservice;

import com.telcox.springmicroservices.orderservice.client.CustomerServiceClient;
import com.telcox.springmicroservices.orderservice.client.ProductCatalogServiceClient;
import com.telcox.springmicroservices.orderservice.client.dto.CustomerDto;
import com.telcox.springmicroservices.orderservice.client.dto.CustomerStatus;
import com.telcox.springmicroservices.orderservice.client.dto.CustomerType;
import com.telcox.springmicroservices.orderservice.client.dto.ProductDto;
import com.telcox.springmicroservices.orderservice.domain.entity.OutboxEvent;
import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import com.telcox.springmicroservices.orderservice.domain.entity.OrderItem;
import com.telcox.springmicroservices.orderservice.domain.enums.ProductType;
import com.telcox.springmicroservices.orderservice.dto.OrderItemRequest;
import com.telcox.springmicroservices.orderservice.dto.OrderRequest;
import com.telcox.springmicroservices.orderservice.dto.OrderResponse;
import com.telcox.springmicroservices.orderservice.repository.OrderRepository;
import com.telcox.springmicroservices.orderservice.repository.OutboxEventRepository;
import com.telcox.springmicroservices.orderservice.repository.SagaStateRepository;
import com.telcox.springmicroservices.orderservice.mapper.OrderMapper;
import com.telcox.springmicroservices.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:order-saga;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.listener.auto-startup=false",
        "eureka.client.enabled=false"
})
@ActiveProfiles("test")
class OrderCreationSagaIntegrationTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");

    @MockitoBean
    private CustomerServiceClient customerServiceClient;

    @MockitoBean
    private ProductCatalogServiceClient productCatalogServiceClient;

    @MockitoBean
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SagaStateRepository sagaStateRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabaseAndMocks() {
        outboxEventRepository.deleteAll();
        sagaStateRepository.deleteAll();
        orderRepository.deleteAll();
        reset(customerServiceClient, productCatalogServiceClient, orderMapper);

        when(orderMapper.toEntity(any(OrderRequest.class))).thenAnswer(invocation -> {
            OrderRequest request = invocation.getArgument(0);
            return Order.builder().customerId(request.getCustomerId()).build();
        });
        when(orderMapper.toEntity(any(OrderItemRequest.class))).thenAnswer(invocation -> {
            OrderItemRequest item = invocation.getArgument(0);
            return OrderItem.builder()
                    .productId(item.getProductId())
                    .productType(item.getProductType())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build();
        });
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return OrderResponse.builder()
                    .id(order.getPublicId())
                    .customerId(order.getCustomerId())
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .currency(order.getCurrency())
                    .build();
        });

        when(productCatalogServiceClient.getProductsByIds(anyList())).thenReturn(List.of(ProductDto.builder()
                .productId(PRODUCT_ID)
                .productCode("TARIFF-TEST")
                .name("Test Tariff")
                .price(new BigDecimal("100.00"))
                .status("ACTIVE")
                .build()));
    }

    @Test
    void createOrderPassesCustomerCheckAndWritesOrderCreatedToOutbox() {
        when(customerServiceClient.getCustomerById(CUSTOMER_ID)).thenReturn(CustomerDto.builder()
                .id(CUSTOMER_ID)
                .type(CustomerType.INDIVIDUAL)
                .status(CustomerStatus.ACTIVE)
                .build());

        OrderResponse response = orderService.createOrder(orderRequest());

        assertThat(response.getId()).isNotNull();
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(sagaStateRepository.count()).isEqualTo(1);
        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo("OrderCreated");
            assertThat(event.getPayload().get("customerId").asText()).isEqualTo(CUSTOMER_ID.toString());
            assertThat(event.getPayload().get("orderId").asText()).isEqualTo(response.getId().toString());
        });
        verify(customerServiceClient).getCustomerById(CUSTOMER_ID);
    }

    @Test
    void downstreamFailureRollsBackOrderSagaAndOutboxTogether() {
        when(customerServiceClient.getCustomerById(CUSTOMER_ID)).thenThrow(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer service connection failed"));

        assertThatThrownBy(() -> orderService.createOrder(orderRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503 SERVICE_UNAVAILABLE");

        assertThat(orderRepository.count()).isZero();
        assertThat(sagaStateRepository.count()).isZero();
        assertThat(outboxEventRepository.count()).isZero();
    }

    private static OrderRequest orderRequest() {
        return OrderRequest.builder()
                .customerId(CUSTOMER_ID)
                .items(List.of(OrderItemRequest.builder()
                        .productId(PRODUCT_ID)
                        .productType(ProductType.TARIFF)
                        .quantity(1)
                        .unitPrice(new BigDecimal("100.00"))
                        .build()))
                .build();
    }
}
