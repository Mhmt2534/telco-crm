package com.telcox.springmicroservices.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.payment.domain.entity.OutboxEvent;
import com.telcox.springmicroservices.payment.domain.entity.Payment;
import com.telcox.springmicroservices.payment.domain.entity.PaymentAttempt;
import com.telcox.springmicroservices.payment.domain.enums.PaymentMethod;
import com.telcox.springmicroservices.payment.domain.enums.PaymentStatus;
import com.telcox.springmicroservices.payment.dto.OrderCreatedEvent;
import com.telcox.springmicroservices.payment.dto.PaymentCompletedEvent;
import com.telcox.springmicroservices.payment.dto.PaymentFailedEvent;
import com.telcox.springmicroservices.payment.repository.OutboxRepository;
import com.telcox.springmicroservices.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OutboxRepository outboxRepository,
                              ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        log.info("Processing payment for Order ID: {}, Customer ID: {}, Amount: {}",
                event.getOrderId(), event.getCustomerId(), event.getTotalAmount());

        // 1. Idempotency Check: Prevent duplicate payments for the same Order
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(event.getOrderId());
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                log.info("Payment already successfully processed for Order ID: {}. Skipping.", event.getOrderId());
                return;
            }
            log.warn("Found existing failed/pending payment for Order ID: {}. Retrying processing.", event.getOrderId());
        }

        // 2. Initialize Payment record
        Payment payment = new Payment();
        payment.setOrderId(event.getOrderId());
        payment.setAmount(event.getTotalAmount());
        payment.setMethod(PaymentMethod.CARD);
        // paymentRequestId represents the unique business constraint preventing race-condition double billing
        payment.setPaymentRequestId("order-payment-req-" + event.getOrderId());

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setAttemptNo(1);

        // 3. Mock Payment Decision Logic
        boolean isFailedCustomer = event.getCustomerId() != null && event.getCustomerId() == 999L;
        String eventPayloadJson;
        String eventType;

        if (isFailedCustomer) {
            log.warn("Simulating payment rejection for test/blacklisted customer (ID: 999)");
            payment.setStatus(PaymentStatus.FAILED);
            
            attempt.setResponse("{\"status\":\"REJECTED\",\"code\":\"51\",\"reason\":\"INSUFFICIENT_FUNDS\"}");
            payment.addAttempt(attempt);

            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getTotalAmount(),
                    "INSUFFICIENT_FUNDS",
                    "Customer card limit is insufficient for simulation.",
                    Instant.now()
            );

            try {
                eventPayloadJson = objectMapper.writeValueAsString(failedEvent);
            } catch (Exception e) {
                log.error("Failed to serialize PaymentFailedEvent", e);
                throw new RuntimeException("Serialization failure", e);
            }
            eventType = "PaymentFailed";
        } else {
            log.info("Payment approved successfully for customer ID: {}", event.getCustomerId());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(OffsetDateTime.now());
            
            attempt.setResponse("{\"status\":\"APPROVED\",\"code\":\"00\"}");
            payment.addAttempt(attempt);

            // Save the payment first to get the generated UUID
            payment = paymentRepository.save(payment);

            PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                    payment.getId(),
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getTotalAmount(),
                    Instant.now()
            );

            try {
                eventPayloadJson = objectMapper.writeValueAsString(completedEvent);
            } catch (Exception e) {
                log.error("Failed to serialize PaymentCompletedEvent", e);
                throw new RuntimeException("Serialization failure", e);
            }
            eventType = "PaymentCompleted";
        }

        // Save payment & attempts cascade
        if (payment.getId() == null) {
            payment = paymentRepository.save(payment);
        }

        // 4. Write transactional outbox event in pure Lombok-free constructor style
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),           // id
                UUID.randomUUID(),           // event_id
                "Payment",                   // aggregate_type
                payment.getId().toString(),  // aggregate_id
                eventType,                   // event_type
                eventPayloadJson             // payload
        );

        outboxRepository.save(outboxEvent);
        log.info("Saved outbox event {} for Order ID: {}", eventType, event.getOrderId());
    }
}
