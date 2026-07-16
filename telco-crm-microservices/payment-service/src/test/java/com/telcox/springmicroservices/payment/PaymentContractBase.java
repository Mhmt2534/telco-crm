package com.telcox.springmicroservices.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.payment.dto.OrderCreatedEvent;
import com.telcox.springmicroservices.payment.domain.entity.OutboxEvent;
import com.telcox.springmicroservices.payment.repository.OutboxEventRepository;
import com.telcox.springmicroservices.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierSender;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMessageVerifier
@Import(PaymentContractBase.MessagingConfig.class)
public abstract class PaymentContractBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @TestConfiguration
    static class MessagingConfig {
        @Bean("telcox.Payment.events")
        public MessageChannel paymentEventsChannel() {
            return new QueueChannel();
        }
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private MessageVerifierSender<Message<?>> messageVerifierSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContractVerifierMessaging contractVerifierMessagingBase;

    @Autowired
    private ContractVerifierObjectMapper contractVerifierObjectMapperBase;

    @BeforeEach
    public void setup() throws Exception {
        outboxRepository.deleteAll();
        
        // Manually inject beans into the generated subclass since Spring Boot 3 ignores javax.inject.Inject
        try {
            java.lang.reflect.Field messagingField = this.getClass().getDeclaredField("contractVerifierMessaging");
            messagingField.setAccessible(true);
            messagingField.set(this, contractVerifierMessagingBase);

            java.lang.reflect.Field mapperField = this.getClass().getDeclaredField("contractVerifierObjectMapper");
            mapperField.setAccessible(true);
            mapperField.set(this, contractVerifierObjectMapperBase);
        } catch (NoSuchFieldException ignored) {
            // Ignored in case the fields are not present
        }
    }

    public void triggerPaymentCompleted() throws Exception {
        // 1. Simulate the input event that triggers the payment process
        OrderCreatedEvent orderEvent = new OrderCreatedEvent();
        orderEvent.setOrderId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        orderEvent.setCustomerId(java.util.UUID.fromString("00000000-0000-0000-0000-000000012345"));
        orderEvent.setTotalAmount(new BigDecimal("100.00"));

        // 2. Call the real business logic
        paymentService.processPayment(orderEvent);

        // 3. Find the resulting outbox event
        List<OutboxEvent> outboxEvents = outboxRepository.findAll();
        OutboxEvent targetEvent = outboxEvents.stream()
                .filter(e -> "PaymentCompleted".equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("PaymentCompleted outbox event not found!"));

        // 4. Parse the payload
        Map<String, Object> payload = objectMapper.readValue(targetEvent.getPayload(), Map.class);

        // 5. Simulate sending the message to the expected Kafka topic so SCC can verify it
        Message<Map<String, Object>> message = MessageBuilder.withPayload(payload)
                .setHeader("eventType", targetEvent.getEventType())
                .build();

        // Send to the destination specified in the contract ('telcox.Payment.events')
        messageVerifierSender.send(message, "telcox.Payment.events");
    }
}
