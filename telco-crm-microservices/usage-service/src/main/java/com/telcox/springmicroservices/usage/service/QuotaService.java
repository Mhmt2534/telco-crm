package com.telcox.springmicroservices.usage.service;

import com.telcox.springmicroservices.usage.dto.CdrRecordedEvent;
import com.telcox.springmicroservices.usage.dto.QuotaResponse;
import com.telcox.springmicroservices.usage.entity.Quota;
import com.telcox.springmicroservices.usage.entity.UsageRecord;
import com.telcox.springmicroservices.usage.entity.UsageType;
import com.telcox.springmicroservices.usage.entity.OutboxEvent;
import com.telcox.springmicroservices.usage.repository.QuotaRepository;
import com.telcox.springmicroservices.usage.repository.UsageRecordRepository;
import com.telcox.springmicroservices.usage.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import com.telcox.springmicroservices.usage.dto.TariffChangeRequestedEvent;
import com.telcox.springmicroservices.usage.dto.TariffDto;
import com.telcox.springmicroservices.usage.client.ProductCatalogServiceClient;
import com.telcox.springmicroservices.usage.entity.QuotaStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.telcox.springmicroservices.usage.dto.OverageSummaryProjection;
import java.util.UUID;

@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private final QuotaRepository quotaRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ProductCatalogServiceClient productCatalogServiceClient;
    private final RedissonClient redissonClient;

    public QuotaService(QuotaRepository quotaRepository,
                        UsageRecordRepository usageRecordRepository,
                        OutboxEventRepository outboxEventRepository,
                        ObjectMapper objectMapper,
                        ProductCatalogServiceClient productCatalogServiceClient,
                        RedissonClient redissonClient) {
        this.quotaRepository = quotaRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.productCatalogServiceClient = productCatalogServiceClient;
        this.redissonClient = redissonClient;
    }

    /**
     * Belirli bir aboneliğin anlık kota bilgisini döner.
     * KART 8: GET /api/v1/usage/subscriptions/{id}/quota
     */
    @Transactional(readOnly = true)
    public QuotaResponse getQuotaBySubscriptionId(UUID subscriptionId) {
        Quota quota = quotaRepository.findBySubscriptionIdAndStatus(subscriptionId, QuotaStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Kota bulunamadı. SubscriptionId: " + subscriptionId));

        return toResponse(quota);
    }

    /**
     * KART 09: Kafka'dan gelen CdrRecorded event'ini işler.
     */
    @Transactional
    public void processCdrEvent(CdrRecordedEvent event) {
        // Idempotency kontrolü
        if (event.getCdrRef() != null && usageRecordRepository.existsByCdrRef(event.getCdrRef())) {
            log.warn("CDR Ref {} already processed. Skipping.", event.getCdrRef());
            return;
        }

        // Abonenin kotasını bul
        Quota quota = quotaRepository.findBySubscriptionIdAndStatus(event.getSubscriptionId(), QuotaStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Abonelik için aktif kota bulunamadı: " + event.getSubscriptionId()));

        // Tüketimi düş ve aşım (overage) miktarını hesapla
        UsageType type = UsageType.valueOf(event.getType());
        double amount = event.getAmount();
        double overageAmount = 0.0;

        switch (type) {
            case VOICE: {
                int oldRemaining = quota.getMinutesRemaining();
                int used = (int) amount;
                int newRemaining = Math.max(0, oldRemaining - used);
                overageAmount = Math.max(0.0, (double) (used - oldRemaining));
                quota.setMinutesRemaining(newRemaining);
                break;
            }
            case SMS: {
                int oldRemaining = quota.getSmsRemaining();
                int used = (int) amount;
                int newRemaining = Math.max(0, oldRemaining - used);
                overageAmount = Math.max(0.0, (double) (used - oldRemaining));
                quota.setSmsRemaining(newRemaining);
                break;
            }
            case DATA: {
                long oldRemaining = quota.getMbRemaining();
                long used = (long) amount;
                long newRemaining = Math.max(0L, oldRemaining - used);
                overageAmount = Math.max(0.0, (double) (used - oldRemaining));
                quota.setMbRemaining(newRemaining);
                break;
            }
        }
        quotaRepository.save(quota);

        // Kullanım kaydı oluştur
        UsageRecord record = new UsageRecord();
        record.setSubscriptionId(event.getSubscriptionId());
        record.setMsisdn(event.getMsisdn());
        record.setType(type);
        record.setQuantity(amount); // Entity field is quantity
        record.setCdrRef(event.getCdrRef());
        record.setRecordedAt(event.getRecordedAt() != null ? event.getRecordedAt() : Instant.now());
        
        // Aşım bilgileri set ediliyor
        if (overageAmount > 0.0) {
            record.setOverage(true);
            record.setOverageAmount(overageAmount);
        } else {
            record.setOverage(false);
            record.setOverageAmount(0.0);
        }
        usageRecordRepository.save(record);

        log.info("Processed CDR: {} amount of {} for MSISDN: {}", amount, type, event.getMsisdn());

        // KART 20.5: Kota eşik ve aşım kontrolleri
        checkAndPublishThresholds(quota, type, event.getMsisdn());
    }

    private void checkAndPublishThresholds(Quota quota, UsageType type, String msisdn) {
        boolean thresholdReached = false;
        boolean exceeded = false;

        switch (type) {
            case VOICE:
                if (quota.isVoiceAt80Percent() && !quota.isVoiceThresholdReached()) {
                    quota.setVoiceThresholdReached(true);
                    thresholdReached = true;
                }
                if (quota.isVoiceAt100Percent() && !quota.isVoiceExceeded()) {
                    quota.setVoiceExceeded(true);
                    exceeded = true;
                }
                break;
            case SMS:
                if (quota.isSmsAt80Percent() && !quota.isSmsThresholdReached()) {
                    quota.setSmsThresholdReached(true);
                    thresholdReached = true;
                }
                if (quota.isSmsAt100Percent() && !quota.isSmsExceeded()) {
                    quota.setSmsExceeded(true);
                    exceeded = true;
                }
                break;
            case DATA:
                if (quota.isDataAt80Percent() && !quota.isDataThresholdReached()) {
                    quota.setDataThresholdReached(true);
                    thresholdReached = true;
                }
                if (quota.isDataAt100Percent() && !quota.isDataExceeded()) {
                    quota.setDataExceeded(true);
                    exceeded = true;
                }
                break;
        }

        if (thresholdReached || exceeded) {
            quotaRepository.save(quota); // Durum bayraklarını güncelle
        }

        if (thresholdReached) {
            Map<String, Object> payload = Map.of(
                "subscriptionId", quota.getSubscriptionId(),
                "msisdn", msisdn,
                "usageType", type.name(),
                "limitType", "80_PERCENT",
                "thresholdReachedAt", Instant.now().toString()
            );
            saveOutboxEvent(quota.getSubscriptionId(), "QuotaThresholdReached", payload);
            log.info("QuotaThresholdReached event written for subscription: {} type: {}", quota.getSubscriptionId(), type);
        }

        if (exceeded) {
            Map<String, Object> payload = Map.of(
                "subscriptionId", quota.getSubscriptionId(),
                "msisdn", msisdn,
                "usageType", type.name(),
                "limitType", "100_PERCENT",
                "exceededAt", Instant.now().toString()
            );
            saveOutboxEvent(quota.getSubscriptionId(), "QuotaExceeded", payload);
            log.info("QuotaExceeded event written for subscription: {} type: {}", quota.getSubscriptionId(), type);
        }
    }

    private void saveOutboxEvent(UUID subscriptionId, String eventType, Object payloadObj) {
        try {
            OutboxEvent outbox = new OutboxEvent();
            outbox.setId(UUID.randomUUID());
            outbox.setAggregateType("Quota");
            outbox.setAggregateId(subscriptionId.toString());
            outbox.setEventType(eventType);
            outbox.setPayload(objectMapper.writeValueAsString(payloadObj));
            outbox.setProcessed(false);
            outbox.setCreatedAt(java.time.ZonedDateTime.now());
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
        }
    }

    @Transactional(readOnly = true)
    public List<OverageSummaryProjection> getOverageSummary(UUID subscriptionId, Instant start, Instant end) {
        log.info("Fetching overage summary for subscription: {} from {} to {}", subscriptionId, start, end);
        return usageRecordRepository.getOverageSummary(subscriptionId, start, end);
    }

    // ── Mapper ────────────────────────────────────────────────

    private QuotaResponse toResponse(Quota q) {
        QuotaResponse r = new QuotaResponse();
        r.setSubscriptionId(q.getSubscriptionId());
        r.setPeriodStart(q.getPeriodStart());
        r.setPeriodEnd(q.getPeriodEnd());
        r.setTotalMinutes(q.getTotalMinutes());
        r.setTotalSms(q.getTotalSms());
        r.setTotalMb(q.getTotalMb());
        r.setMinutesRemaining(q.getMinutesRemaining());
        r.setSmsRemaining(q.getSmsRemaining());
        r.setMbRemaining(q.getMbRemaining());
        r.setMbUsagePercent(q.mbUsagePercent());
        return r;
    }
    @Transactional
    public void processTariffChange(TariffChangeRequestedEvent event) {
        log.info("[USAGE] Tarife değişikliği işleniyor. OrderId: {}", event.getOrderId());

        if (event.getOrderId() == null || event.getSubscriptionId() == null || event.getNewTariffCode() == null) {
            throw new IllegalArgumentException("Tarife değişikliği için gerekli alanlar eksik.");
        }

        String idempotencyKey = "idempotency:quota_change:" + event.getOrderId();
        RBucket<Boolean> bucket = redissonClient.getBucket(idempotencyKey);

        boolean isFirstProcess = bucket.setIfAbsent(true, Duration.ofDays(7));
        if (!isFirstProcess) {
            log.info("[USAGE] Tarife değişikliği daha önce işlenmiş (Idempotent). İşlem atlanıyor. OrderId: {}", event.getOrderId());
            return;
        }

        try {
            Quota activeQuota = quotaRepository.findBySubscriptionIdAndStatus(event.getSubscriptionId(), QuotaStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Abonelik için aktif kota bulunamadı: " + event.getSubscriptionId()));

            TariffDto newTariff = productCatalogServiceClient.getTariffById(event.getNewTariffId());
            if (newTariff == null) {
                throw new RuntimeException("Yeni tarife bilgisi alınamadı: " + event.getNewTariffCode());
            }

            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime periodStart;
            OffsetDateTime periodEnd;
            QuotaStatus newStatus;

            if (!"NEXT_CYCLE".equals(event.getEffectiveBillCycle())) {
                // IMMEDIATE change
                activeQuota.setPeriodEnd(now);
                quotaRepository.save(activeQuota);

                periodStart = now;
                periodEnd = now.plusMonths(1);
                newStatus = QuotaStatus.ACTIVE;
                log.info("[USAGE] Tarife hemen aktif ediliyor. OrderId: {}", event.getOrderId());
            } else {
                // NEXT_CYCLE change
                periodStart = activeQuota.getPeriodEnd();
                periodEnd = periodStart.plusMonths(1);
                newStatus = QuotaStatus.PENDING;
                log.info("[USAGE] Tarife bir sonraki dönemde aktif edilecek. OrderId: {}", event.getOrderId());
            }

            Quota newQuota = new Quota(
                    event.getSubscriptionId(),
                    periodStart,
                    periodEnd,
                    newTariff.getMinutesIncluded(),
                    newTariff.getSmsIncluded(),
                    newTariff.getDataMbIncluded(),
                    newStatus
            );

            quotaRepository.save(newQuota);
            log.info("[USAGE] Yeni kota kaydı başarıyla oluşturuldu. OrderId: {}, Status: {}", event.getOrderId(), newStatus);
        } catch (Exception ex) {
            log.error("[USAGE] Tarife değişikliği işlenirken hata oluştu. OrderId: {}. Idempotency kilidi siliniyor.", event.getOrderId(), ex);
            bucket.delete();
            throw ex;
        }
    }
}
