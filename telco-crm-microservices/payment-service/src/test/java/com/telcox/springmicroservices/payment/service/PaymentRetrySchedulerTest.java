package com.telcox.springmicroservices.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.payment.domain.entity.Payment;
import com.telcox.springmicroservices.payment.domain.entity.PaymentAttempt;
import com.telcox.springmicroservices.payment.domain.enums.PaymentStatus;
import com.telcox.springmicroservices.payment.repository.OutboxEventRepository;
import com.telcox.springmicroservices.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentRetrySchedulerTest {

    private PaymentRetryScheduler scheduler;
    private PaymentRepository paymentRepository;
    private OutboxEventRepository outboxEventRepository;
    private RedissonClient redissonClient;
    private ObjectMapper objectMapper;
    private RLock mockLock;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        outboxEventRepository = mock(OutboxEventRepository.class);
        redissonClient = mock(RedissonClient.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        scheduler = new PaymentRetryScheduler(paymentRepository, outboxEventRepository, redissonClient, objectMapper);
        
        ReflectionTestUtils.setField(scheduler, "delayHours", List.of(24, 72, 168));
        
        mockLock = mock(RLock.class);
        when(redissonClient.getLock(any(String.class))).thenReturn(mockLock);
    }

    @Test
    void testProcessSingleRetry_NotTimeYet() {
        Payment payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAttempts(new ArrayList<>());
        
        PaymentAttempt attempt1 = new PaymentAttempt();
        attempt1.setAttemptNo(1);
        attempt1.setAttemptedAt(OffsetDateTime.now().minusHours(2)); // Waiting 24h, so 2h is not enough
        payment.addAttempt(attempt1);
        
        scheduler.processSingleRetry(payment);
        
        // No new attempt should be added
        assertEquals(1, payment.getAttempts().size());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testProcessSingleRetry_TimePassed() {
        Payment payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setInvoiceId("INV_123");
        payment.setAttempts(new ArrayList<>());
        
        PaymentAttempt attempt1 = new PaymentAttempt();
        attempt1.setAttemptNo(1);
        attempt1.setAttemptedAt(OffsetDateTime.now().minusHours(25)); // Waiting 24h, 25h passed
        payment.addAttempt(attempt1);
        
        scheduler.processSingleRetry(payment);
        
        // Should have retried
        assertEquals(2, payment.getAttempts().size());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void testProcessSingleRetry_FailAndExhaust() {
        Payment payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setInvoiceId("FAIL_INV"); // Will cause failure
        payment.setAttempts(new ArrayList<>());
        
        // Setup 3 previous attempts
        for (int i = 1; i <= 3; i++) {
            PaymentAttempt attempt = new PaymentAttempt();
            attempt.setAttemptNo(i);
            // make the 3rd attempt 169h ago, so 168h wait is satisfied
            attempt.setAttemptedAt(OffsetDateTime.now().minusHours(200));
            payment.addAttempt(attempt);
        }
        
        scheduler.processSingleRetry(payment);
        
        // Should have retried and failed
        assertEquals(4, payment.getAttempts().size());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
        verify(outboxEventRepository, times(1)).save(any()); // Should emit PaymentFailed event
    }
}
