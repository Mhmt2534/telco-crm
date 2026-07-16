package com.telcox.springmicroservices.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.payment.domain.entity.OutboxEvent;
import com.telcox.springmicroservices.payment.domain.entity.Payment;
import com.telcox.springmicroservices.payment.domain.entity.PaymentAttempt;
import com.telcox.springmicroservices.payment.domain.enums.PaymentMethod;
import com.telcox.springmicroservices.payment.domain.enums.PaymentStatus;
import com.telcox.springmicroservices.payment.dto.*;
import com.telcox.springmicroservices.payment.repository.OutboxEventRepository;
import com.telcox.springmicroservices.payment.repository.PaymentRepository;
import com.telcox.springmicroservices.payment.repository.WalletRepository;
import com.telcox.springmicroservices.payment.domain.entity.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;
    private final WalletRepository walletRepository;
    private final WalletSecurityService walletSecurityService;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper,
            IdempotencyService idempotencyService,
            WalletRepository walletRepository,
            WalletSecurityService walletSecurityService) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.idempotencyService = idempotencyService;
        this.walletRepository = walletRepository;
        this.walletSecurityService = walletSecurityService;
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, String idempotencyKey, String actorId) {
        UUID customerId = request.getCustomerId();
        log.info("Initiating payment for Invoice ID: {}, Customer ID: {}, Actor ID: {}", request.getInvoiceId(), customerId, actorId);

        String cachedResponse = idempotencyService.processIdempotency(idempotencyKey);
        if (cachedResponse != null) {
            log.info("Returning cached response for Idempotency-Key: {}", idempotencyKey);
            try {
                return objectMapper.readValue(cachedResponse, PaymentResponse.class);
            } catch (Exception e) {
                log.error("Failed to deserialize cached response", e);
                throw new RuntimeException("Deserialization failure", e);
            }
        }

        PaymentResponse response = null;
        try {
            Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existingPayment.isPresent()) {
                Payment payment = existingPayment.get();
                response = buildResponse(payment);
                return response;
            }

            Payment payment = new Payment();
            payment.setInvoiceId(request.getInvoiceId());
            payment.setCustomerId(customerId);
            payment.setActorId(actorId);
            payment.setOrderId(request.getOrderId());
            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency());
            payment.setMethod(PaymentMethod.valueOf(request.getMethod()));
            payment.setIdempotencyKey(idempotencyKey);

            PaymentAttempt attempt = new PaymentAttempt();
            attempt.setAttemptNo(1);
            attempt.setAttemptedAt(OffsetDateTime.now());

            boolean isFailed = payment.getExternalRef() != null && payment.getExternalRef().startsWith("FAIL_");

            BigDecimal amountToPay = request.getAmount();
            BigDecimal walletDeduction = BigDecimal.ZERO;
            
            // Priority Payment via Wallet
            Optional<Wallet> walletOpt = walletRepository.findByCustomerId(customerId);
            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                walletSecurityService.verifyHash(wallet); // verify tampering
                
                BigDecimal currentBalance = wallet.getBalance();
                if (currentBalance.compareTo(BigDecimal.ZERO) > 0) {
                    if (currentBalance.compareTo(amountToPay) >= 0) {
                        // Wallet covers everything
                        walletDeduction = amountToPay;
                        wallet.setBalance(currentBalance.subtract(amountToPay));
                        amountToPay = BigDecimal.ZERO;
                    } else {
                        // Wallet covers partially
                        walletDeduction = currentBalance;
                        wallet.setBalance(BigDecimal.ZERO);
                        amountToPay = amountToPay.subtract(walletDeduction);
                    }
                    
                    wallet.setBalanceHash(walletSecurityService.calculateHash(wallet.getCustomerId(), wallet.getBalance()));
                    walletRepository.save(wallet);
                    
                    log.info("Deducted {} from wallet for invoice {}. Remaining amount for PSP: {}", 
                            walletDeduction, request.getInvoiceId(), amountToPay);
                }
            }
            
            // If amountToPay is 0, we don't need PSP call.
            if (amountToPay.compareTo(BigDecimal.ZERO) == 0) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(OffsetDateTime.now());
                payment.setMethod(PaymentMethod.WALLET); // Record as fully wallet payment
                attempt.setResponseCode("00");
                attempt.setResponseMessage("APPROVED_FROM_WALLET");
                log.info("Payment fully covered by wallet.");
            } else if (isFailed) {
                payment.setStatus(PaymentStatus.PENDING);
                attempt.setResponseCode("51");
                attempt.setResponseMessage("INSUFFICIENT_FUNDS");
            } else {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(OffsetDateTime.now());
                attempt.setResponseCode("00");
                attempt.setResponseMessage("APPROVED");
            }

            payment.addAttempt(attempt);
            payment = paymentRepository.save(payment);

            response = buildResponse(payment);

            if (!isFailed) {
                emitPaymentCompletedEvent(payment);
            }

            return response;
        } catch (Exception ex) {
            log.error("Error processing payment", ex);
            idempotencyService.removeKey(idempotencyKey);
            throw ex;
        } finally {
            if (response != null) {
                try {
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    idempotencyService.cacheResponse(idempotencyKey, jsonResponse);
                } catch (Exception e) {
                    log.error("Failed to cache response", e);
                }
            }
        }
    }

    private PaymentResponse buildResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getPaidAt());
    }

    private void emitPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent payload = new PaymentCompletedEvent(
                payment.getId(), payment.getOrderId(), payment.getInvoiceId(), payment.getCustomerId(),
                payment.getAmount(), payment.getPaidAt() != null ? payment.getPaidAt().toInstant() : Instant.now());

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize PaymentCompletedEvent payload", e);
            throw new RuntimeException("Serialization failure", e);
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(UUID.randomUUID())
                .aggregateType("Payment")
                .aggregateId(payment.getId().toString())
                .eventType("PaymentCompleted")
                .payload(payloadJson)
                .build();

        outboxRepository.save(outboxEvent);
    }

    @Override
    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        log.info("Processing payment for Order ID: {}, Customer ID: {}, Amount: {}",
                event.getOrderId(), event.getCustomerId(), event.getTotalAmount());

        Optional<Payment> existingPayment = paymentRepository.findByOrderId(event.getOrderId());
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                log.info("Payment already successfully processed for Order ID: {}. Skipping.", event.getOrderId());
                return;
            }
        }

        Payment payment = new Payment();
        payment.setOrderId(event.getOrderId());
        payment.setCustomerId(event.getCustomerId());
        payment.setAmount(event.getTotalAmount());
        payment.setCurrency("TRY");
        payment.setMethod(PaymentMethod.CARD);
        payment.setIdempotencyKey("order-payment-req-" + event.getOrderId());

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setAttemptNo(1);
        attempt.setAttemptedAt(OffsetDateTime.now());

        boolean isFailedCustomer = event.getCustomerId() != null && event.getCustomerId().toString().endsWith("0999");
        String eventPayloadJson;
        String eventType;

        if (isFailedCustomer) {
            payment.setStatus(PaymentStatus.PENDING);
            attempt.setResponseCode("51");
            attempt.setResponseMessage("INSUFFICIENT_FUNDS");
            payment.addAttempt(attempt);

            // Do not emit event yet, it will be handled by scheduler if it fails finally
            eventType = null;
            eventPayloadJson = null;
        } else {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(OffsetDateTime.now());
            attempt.setResponseCode("00");
            attempt.setResponseMessage("APPROVED");
            payment.addAttempt(attempt);

            payment = paymentRepository.save(payment);

            PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                    payment.getId(),
                    event.getOrderId(),
                    null,
                    event.getCustomerId(),
                    event.getTotalAmount(),
                    Instant.now());

            try {
                eventPayloadJson = objectMapper.writeValueAsString(completedEvent);
            } catch (Exception e) {
                throw new RuntimeException("Serialization failure", e);
            }
            eventType = "PaymentCompleted";
        }

        if (payment.getId() == null) {
            payment = paymentRepository.save(payment);
        }

        if (eventType != null) {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID())
                    .aggregateType("Payment")
                    .aggregateId(payment.getId().toString())
                    .eventType(eventType)
                    .payload(eventPayloadJson)
                    .build();

            outboxRepository.save(outboxEvent);
        }
    }

    @Override
    @Transactional
    public void refundPayment(com.telcox.springmicroservices.payment.dto.PaymentRefundRequestedEvent event) {
        log.info("Processing refund for Payment ID: {}, Order ID: {}", event.getPaymentId(), event.getOrderId());

        Optional<Payment> existingPaymentOpt = paymentRepository.findById(event.getPaymentId());
        if (existingPaymentOpt.isEmpty()) {
            return;
        }

        Payment payment = existingPaymentOpt.get();

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            return;
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        com.telcox.springmicroservices.payment.dto.PaymentRefundedEvent refundedEvent = new com.telcox.springmicroservices.payment.dto.PaymentRefundedEvent(
                payment.getId(),
                payment.getOrderId(),
                event.getAmount(),
                Instant.now());

        String eventPayloadJson;
        try {
            eventPayloadJson = objectMapper.writeValueAsString(refundedEvent);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failure", e);
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(UUID.randomUUID())
                .aggregateType("Payment")
                .aggregateId(payment.getId().toString())
                .eventType("PaymentRefunded")
                .payload(eventPayloadJson)
                .build();

        outboxRepository.save(outboxEvent);
    }
    
    @Override
    @Transactional
    public WalletBalanceResponse topUpWallet(UUID customerId, WalletTopUpRequest request) {
        Wallet wallet = walletRepository.findByCustomerId(customerId).orElseGet(() -> {
            Wallet newWallet = new Wallet();
            newWallet.setCustomerId(customerId);
            newWallet.setBalance(BigDecimal.ZERO);
            return newWallet;
        });

        if (wallet.getId() != null) {
            walletSecurityService.verifyHash(wallet);
        }

        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(newBalance);
        wallet.setBalanceHash(walletSecurityService.calculateHash(wallet.getCustomerId(), newBalance));
        
        wallet = walletRepository.save(wallet);
        
        log.info("Wallet topped up for customerId: {}. New balance: {}", customerId, newBalance);
        return new WalletBalanceResponse(wallet.getCustomerId(), wallet.getBalance());
    }

    @Override
    @Transactional(readOnly = true)
    public WalletBalanceResponse getWalletBalance(UUID customerId) {
        Optional<Wallet> walletOpt = walletRepository.findByCustomerId(customerId);
        if (walletOpt.isEmpty()) {
            return new WalletBalanceResponse(customerId, BigDecimal.ZERO);
        }
        
        Wallet wallet = walletOpt.get();
        walletSecurityService.verifyHash(wallet);
        
        return new WalletBalanceResponse(wallet.getCustomerId(), wallet.getBalance());
    }
}
