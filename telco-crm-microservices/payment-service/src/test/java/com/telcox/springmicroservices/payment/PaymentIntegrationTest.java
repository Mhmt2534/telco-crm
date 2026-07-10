package com.telcox.springmicroservices.payment;

import com.telcox.springmicroservices.payment.domain.entity.OutboxEvent;
import com.telcox.springmicroservices.payment.domain.entity.Payment;
import com.telcox.springmicroservices.payment.domain.enums.PaymentStatus;
import com.telcox.springmicroservices.payment.repository.OutboxEventRepository;
import com.telcox.springmicroservices.payment.repository.PaymentRepository;
import com.telcox.springmicroservices.payment.service.PaymentRetryScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PaymentIntegrationTest {

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
        
        // Very short delays for testing
        registry.add("payment.retry.delays-seconds", () -> "1,1,1");
        registry.add("payment.retry.delays-hours", () -> "24,72,168");
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PaymentRetryScheduler paymentRetryScheduler;

    @Test
    void testPaymentRetryScheduler_FailureThenEvent() throws InterruptedException {
        // Setup a failed payment with 3 previous attempts (simulating exhausted retries)
        Payment payment = new Payment();
        payment.setInvoiceId("FAIL_INV_TEST");
        payment.setCustomerId("123");
        payment.setAmount(new java.math.BigDecimal("100.00"));
        payment.setCurrency("TRY");
        payment.setMethod(com.telcox.springmicroservices.payment.domain.enums.PaymentMethod.CARD);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(UUID.randomUUID().toString());
        
        for (int i = 1; i <= 3; i++) {
            com.telcox.springmicroservices.payment.domain.entity.PaymentAttempt attempt = new com.telcox.springmicroservices.payment.domain.entity.PaymentAttempt();
            attempt.setAttemptNo(i);
            // set it in the past to ensure delay is satisfied
            attempt.setAttemptedAt(java.time.OffsetDateTime.now().minusHours(200));
            payment.addAttempt(attempt);
        }
        
        paymentRepository.save(payment);
        
        // Ensure no outbox events exist
        outboxEventRepository.deleteAll();

        // Run the scheduler method manually
        paymentRetryScheduler.processRetries();

        // Verify payment is still FAILED and has 4 attempts
        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, updatedPayment.getStatus());
        assertEquals(4, updatedPayment.getAttempts().size());

        // Verify PaymentFailed outbox event was created
        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertEquals(1, events.size());
        OutboxEvent event = events.get(0);
        assertEquals("PaymentFailed", event.getEventType());
        assertTrue(event.getPayload().contains("INSUFFICIENT_FUNDS"));
    }
}
