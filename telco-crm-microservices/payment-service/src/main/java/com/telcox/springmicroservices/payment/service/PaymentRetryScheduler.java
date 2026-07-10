package com.telcox.springmicroservices.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.payment.domain.entity.OutboxEvent;
import com.telcox.springmicroservices.payment.domain.entity.Payment;
import com.telcox.springmicroservices.payment.domain.entity.PaymentAttempt;
import com.telcox.springmicroservices.payment.domain.enums.PaymentStatus;
import com.telcox.springmicroservices.payment.dto.PaymentCompletedEvent;
import com.telcox.springmicroservices.payment.dto.PaymentFailedEvent;
import com.telcox.springmicroservices.payment.repository.OutboxEventRepository;
import com.telcox.springmicroservices.payment.repository.PaymentRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PaymentRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentRetryScheduler.class);

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Value("${payment.retry.delays-hours:24,72,168}")
    private List<Integer> delayHours;

    @Value("${payment.retry.delays-seconds:#{null}}")
    private List<Integer> delaySeconds;

    public PaymentRetryScheduler(PaymentRepository paymentRepository,
                                 OutboxEventRepository outboxEventRepository,
                                 RedissonClient redissonClient,
                                 ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    // Run every minute
    @Scheduled(fixedDelay = 60000)
    public void processRetries() {
        // max attempts = 1 initial + size of delays list
        int maxAttempts = delayHours.size();
        List<Payment> eligiblePayments = paymentRepository.findFailedPaymentsEligibleForRetry(maxAttempts);

        for (Payment payment : eligiblePayments) {
            String lockKey = "lock:payment:retry:" + payment.getId();
            RLock lock = redissonClient.getLock(lockKey);
            
            try {
                if (lock.tryLock(0, 5, TimeUnit.MINUTES)) {
                    try {
                        processSingleRetry(payment);
                    } catch (Exception e) {
                        log.error("Failed to retry payment {}", payment.getId(), e);
                    } finally {
                        lock.unlock();
                    }
                } else {
                    log.info("Could not acquire lock for payment {}, another node might be processing it.", payment.getId());
                }
            } catch (InterruptedException e) {
                log.error("Error acquiring lock for payment retry", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Transactional
    public void processSingleRetry(Payment payment) {
        List<PaymentAttempt> attempts = payment.getAttempts();
        if (attempts.isEmpty()) return;

        attempts.sort(Comparator.comparing(PaymentAttempt::getAttemptNo));
        PaymentAttempt lastAttempt = attempts.get(attempts.size() - 1);
        
        int currentAttemptIndex = attempts.size() - 1; // 0-based index for delay array
        if (currentAttemptIndex >= delayHours.size()) {
            return; // Already exhausted, query should have filtered this, but double check
        }

        long delayAmount = delaySeconds != null && !delaySeconds.isEmpty() 
                ? delaySeconds.get(currentAttemptIndex) 
                : delayHours.get(currentAttemptIndex);
                
        ChronoUnit delayUnit = delaySeconds != null && !delaySeconds.isEmpty() 
                ? ChronoUnit.SECONDS 
                : ChronoUnit.HOURS;

        OffsetDateTime nextRetryTime = lastAttempt.getAttemptedAt().plus(delayAmount, delayUnit);
        
        if (OffsetDateTime.now().isBefore(nextRetryTime)) {
            // Not yet time to retry
            return;
        }

        log.info("Retrying payment {}, attempt number {}", payment.getId(), attempts.size() + 1);

        PaymentAttempt newAttempt = new PaymentAttempt();
        newAttempt.setAttemptNo(attempts.size() + 1);
        newAttempt.setAttemptedAt(OffsetDateTime.now());

        // Mock PSP Call for retry
        boolean isFailed = payment.getInvoiceId() != null && 
            (payment.getInvoiceId().startsWith("FAIL_") || payment.getInvoiceId().equals("112"));

        if (isFailed) {
            newAttempt.setResponseCode("51");
            newAttempt.setResponseMessage("INSUFFICIENT_FUNDS");
            payment.addAttempt(newAttempt);
            
            // If this was the last possible retry
            if (newAttempt.getAttemptNo() > delayHours.size()) {
                log.warn("Payment {} exhausted all retries. Emitting PaymentFailed event.", payment.getId());
                payment.setStatus(PaymentStatus.FAILED);
                emitPaymentFailedEvent(payment, "INSUFFICIENT_FUNDS");
            }
        } else {
            newAttempt.setResponseCode("00");
            newAttempt.setResponseMessage("APPROVED");
            payment.addAttempt(newAttempt);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(OffsetDateTime.now());
            
            log.info("Payment {} retry successful. Emitting PaymentCompleted event.", payment.getId());
            emitPaymentCompletedEvent(payment);
        }

        paymentRepository.save(payment);
    }
    
    private void emitPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getPaidAt() != null ? payment.getPaidAt().toInstant() : Instant.now()
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID())
                    .aggregateType("Payment")
                    .aggregateId(payment.getId().toString())
                    .eventType("PaymentCompleted")
                    .payload(payloadJson)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to serialize PaymentCompletedEvent", e);
            throw new RuntimeException("Serialization failure", e);
        }
    }

    private void emitPaymentFailedEvent(Payment payment, String failureReason) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                failureReason,
                "Exhausted all retries",
                Instant.now()
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID())
                    .aggregateType("Payment")
                    .aggregateId(payment.getId().toString())
                    .eventType("PaymentFailed")
                    .payload(payloadJson)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to serialize PaymentFailedEvent", e);
            throw new RuntimeException("Serialization failure", e);
        }
    }
}
