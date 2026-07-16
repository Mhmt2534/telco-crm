package com.telcox.springmicroservices.billing.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.billing.entity.*;
import com.telcox.springmicroservices.billing.repository.BillCycleRepository;
import com.telcox.springmicroservices.billing.repository.InvoiceRepository;
import com.telcox.springmicroservices.billing.repository.OutboxEventRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.telcox.springmicroservices.billing.client.UsageServiceClient;
import com.telcox.springmicroservices.billing.dto.UsageOverageSummaryDto;

@Component
public class BillRunScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillRunScheduler.class);

    private final BillCycleRepository billCycleRepository;
    private final InvoiceRepository invoiceRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final UsageServiceClient usageServiceClient;

    public BillRunScheduler(BillCycleRepository billCycleRepository,
                            InvoiceRepository invoiceRepository,
                            OutboxEventRepository outboxEventRepository,
                            RedissonClient redissonClient,
                            ObjectMapper objectMapper,
                            UsageServiceClient usageServiceClient) {
        this.billCycleRepository = billCycleRepository;
        this.invoiceRepository = invoiceRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.usageServiceClient = usageServiceClient;
    }

    // Cron = Saniye Dakika Saat Gun Ay Gun(Hafta)
    // Ornek: "0 0 0 * * ?" -> Her gece saat 00:00'da calisir
    @Scheduled(cron = "0 0 0 * * ?")
    public void runDailyBilling() {
        LocalDate today = LocalDate.now();
        int currentDay = today.getDayOfMonth();
        String lockKey = "lock:bill-run:" + today.toString();

        log.info("Starting daily bill run for day: {}", currentDay);

        RLock lock = redissonClient.getLock(lockKey);
        try {
            // waitTime = 0: baska node calistiriyorsa bekleme direkt iptal et
            // leaseTime = 1: kilit alindiysa 1 saat sonra otomatik dussun
            if (lock.tryLock(0, 1, TimeUnit.HOURS)) {
                try {
                    log.info("Lock acquired successfully: {}. Executing bill run...", lockKey);
                    executeBillRunForDay(currentDay);
                    log.info("Daily bill run completed successfully for day: {}", currentDay);
                } finally {
                    lock.unlock();
                    log.info("Lock released: {}", lockKey);
                }
            } else {
                log.info("Could not acquire lock: {}. Another billing node is probably running it.", lockKey);
            }
        } catch (InterruptedException e) {
            log.error("Error acquiring redisson lock for bill run", e);
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public void executeBillRunForDay(int dayOfMonth) {
        List<BillCycle> cyclesToBill = billCycleRepository.findByCutOffDay(dayOfMonth);
        log.info("Found {} bill cycles to process for day {}", cyclesToBill.size(), dayOfMonth);

        for (BillCycle cycle : cyclesToBill) {
            try {
                processInvoiceForCycle(cycle);
            } catch (Exception e) {
                log.error("Failed to process invoice for cycle id: {}, customer id: {}", cycle.getId(), cycle.getCustomerId(), e);
            }
        }
    }

    private void processInvoiceForCycle(BillCycle cycle) throws JsonProcessingException {
        // 1. Fatura olustur
        Invoice invoice = new Invoice();
        invoice.setCustomerId(cycle.getCustomerId());
        invoice.setSubscriptionId(cycle.getSubscriptionId());
        invoice.setDueDate(LocalDateTime.now().plusDays(15)); // Son odeme tarihi: +15 gun
        invoice.setStatus(InvoiceStatus.UNPAID);

        // Sabit ücret satırı
        InvoiceLine line = new InvoiceLine();
        line.setDescription("Monthly Fixed Tariff Fee");
        line.setQuantity(1);
        line.setUnitPrice(cycle.getFixedAmount());
        line.setLineTotal(cycle.getFixedAmount());
        invoice.addLine(line);

        BigDecimal totalAmount = cycle.getFixedAmount();

        // 2. Kota Aşım (Overage) Bilgilerini usage-service'ten çek ve faturaya ekle
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMonths(1);
        Instant startInstant = start.atZone(java.time.ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atZone(java.time.ZoneId.systemDefault()).toInstant();

        try {
            List<UsageOverageSummaryDto> overages = usageServiceClient.getOverageSummary(
                    cycle.getSubscriptionId(),
                    startInstant.toString(),
                    endInstant.toString()
            );

            if (overages != null) {
                BigDecimal dataPrice = new BigDecimal("0.05");
                BigDecimal voicePrice = new BigDecimal("0.10");
                BigDecimal smsPrice = new BigDecimal("0.03");

                for (UsageOverageSummaryDto item : overages) {
                    if (item.getTotalOverageAmount() == null || item.getTotalOverageAmount() <= 0.0) {
                        continue;
                    }

                    BigDecimal price = BigDecimal.ZERO;
                    String desc = "";

                    if ("DATA".equals(item.getType())) {
                        price = dataPrice;
                        desc = "DATA Overage Charge";
                    } else if ("VOICE".equals(item.getType())) {
                        price = voicePrice;
                        desc = "VOICE Overage Charge";
                    } else if ("SMS".equals(item.getType())) {
                        price = smsPrice;
                        desc = "SMS Overage Charge";
                    } else {
                        continue;
                    }

                    BigDecimal quantityDec = BigDecimal.valueOf(item.getTotalOverageAmount());
                    BigDecimal lineTotal = quantityDec.multiply(price).setScale(2, java.math.RoundingMode.HALF_UP);

                    InvoiceLine overageLine = new InvoiceLine();
                    overageLine.setDescription(desc);
                    overageLine.setQuantity(quantityDec.intValue());
                    overageLine.setUnitPrice(price);
                    overageLine.setLineTotal(lineTotal);

                    invoice.addLine(overageLine);
                    totalAmount = totalAmount.add(lineTotal);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch overage summary from usage-service for subscription {}, fallback to fixed amount only.", cycle.getSubscriptionId(), e);
        }

        invoice.setAmount(totalAmount);
        invoice = invoiceRepository.save(invoice);

        // 2. Event Payload hazirla
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("invoiceId", invoice.getPublicId());
        payloadMap.put("customerId", invoice.getCustomerId());
        payloadMap.put("subscriptionId", invoice.getSubscriptionId());
        payloadMap.put("amount", invoice.getAmount());
        payloadMap.put("dueDate", invoice.getDueDate().toString());
        payloadMap.put("status", invoice.getStatus().name());

        String payloadJson = objectMapper.writeValueAsString(payloadMap);

        // 3. Outbox kaydi olustur
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateType("invoice");
        outboxEvent.setAggregateId(invoice.getPublicId().toString());
        outboxEvent.setType("InvoiceGenerated");
        outboxEvent.setPayload(payloadJson);
        outboxEvent.setStatus(OutboxStatus.PENDING);

        outboxEventRepository.save(outboxEvent);

        log.info("Invoice {} generated successfully for customer {}", invoice.getPublicId(), invoice.getCustomerId());
    }
}
