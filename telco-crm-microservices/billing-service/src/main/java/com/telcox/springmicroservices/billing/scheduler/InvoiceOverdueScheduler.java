package com.telcox.springmicroservices.billing.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.billing.entity.*;
import com.telcox.springmicroservices.billing.repository.InvoiceRepository;
import com.telcox.springmicroservices.billing.repository.OutboxEventRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class InvoiceOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvoiceOverdueScheduler.class);

    private final InvoiceRepository invoiceRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public InvoiceOverdueScheduler(InvoiceRepository invoiceRepository,
                                   OutboxEventRepository outboxEventRepository,
                                   RedissonClient redissonClient,
                                   ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "${billing.overdue-check.cron:0 0 1 * * ?}")
    public void runOverdueCheck() {
        LocalDate today = LocalDate.now();
        String lockKey = "lock:invoice-overdue-check:" + today.toString();

        log.info("Starting daily invoice overdue check...");

        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(0, 1, TimeUnit.HOURS)) {
                try {
                    log.info("Lock acquired successfully: {}. Executing overdue check...", lockKey);
                    processOverdueInvoices();
                    log.info("Daily invoice overdue check completed successfully.");
                } finally {
                    lock.unlock();
                    log.info("Lock released: {}", lockKey);
                }
            } else {
                log.info("Could not acquire lock: {}. Another billing node is probably running it.", lockKey);
            }
        } catch (InterruptedException e) {
            log.error("Error acquiring redisson lock for overdue check", e);
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public void processOverdueInvoices() {
        LocalDateTime now = LocalDateTime.now();
        List<Invoice> overdueInvoices = invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.UNPAID, now);
        log.info("Found {} unpaid overdue invoices to process", overdueInvoices.size());

        for (Invoice invoice : overdueInvoices) {
            try {
                processSingleOverdueInvoice(invoice);
            } catch (Exception e) {
                log.error("Failed to process overdue invoice id: {}", invoice.getId(), e);
            }
        }
    }

    private void processSingleOverdueInvoice(Invoice invoice) throws JsonProcessingException {
        // 1. Update status
        invoice.setStatus(InvoiceStatus.OVERDUE);
        invoiceRepository.save(invoice);

        // 2. Prepare Event Payload
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("invoiceId", invoice.getId());
        payloadMap.put("customerId", invoice.getCustomerId());
        payloadMap.put("subscriptionId", invoice.getSubscriptionId());
        payloadMap.put("amount", invoice.getAmount());
        payloadMap.put("dueDate", invoice.getDueDate().toString());
        payloadMap.put("status", invoice.getStatus().name());

        String payloadJson = objectMapper.writeValueAsString(payloadMap);

        // 3. Create Outbox record
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateType("invoice");
        outboxEvent.setAggregateId(String.valueOf(invoice.getId()));
        outboxEvent.setType("InvoiceOverdueEvent");
        outboxEvent.setPayload(payloadJson);
        outboxEvent.setStatus(OutboxStatus.PENDING);

        outboxEventRepository.save(outboxEvent);

        log.info("Invoice {} marked as OVERDUE and InvoiceOverdueEvent generated", invoice.getId());
    }
}
