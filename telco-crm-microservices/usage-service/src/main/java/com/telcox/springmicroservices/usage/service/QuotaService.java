package com.telcox.springmicroservices.usage.service;

import com.telcox.springmicroservices.usage.dto.CdrRecordedEvent;
import com.telcox.springmicroservices.usage.dto.QuotaResponse;
import com.telcox.springmicroservices.usage.entity.Quota;
import com.telcox.springmicroservices.usage.entity.UsageRecord;
import com.telcox.springmicroservices.usage.entity.UsageType;
import com.telcox.springmicroservices.usage.repository.QuotaRepository;
import com.telcox.springmicroservices.usage.repository.UsageRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private final QuotaRepository quotaRepository;
    private final UsageRecordRepository usageRecordRepository;

    public QuotaService(QuotaRepository quotaRepository, UsageRecordRepository usageRecordRepository) {
        this.quotaRepository = quotaRepository;
        this.usageRecordRepository = usageRecordRepository;
    }

    /**
     * Belirli bir aboneliğin anlık kota bilgisini döner.
     * KART 8: GET /api/v1/usage/subscriptions/{id}/quota
     */
    @Transactional(readOnly = true)
    public QuotaResponse getQuotaBySubscriptionId(UUID subscriptionId) {
        Quota quota = quotaRepository.findBySubscriptionId(subscriptionId)
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
        Quota quota = quotaRepository.findBySubscriptionId(event.getSubscriptionId())
                .orElseThrow(() -> new IllegalArgumentException("Kota bulunamadı. SubscriptionId: " + event.getSubscriptionId()));

        // Tüketimi düş
        UsageType type = UsageType.valueOf(event.getType());
        double amount = event.getAmount();

        switch (type) {
            case VOICE:
                quota.setMinutesRemaining(Math.max(0, quota.getMinutesRemaining() - (int) amount));
                break;
            case SMS:
                quota.setSmsRemaining(Math.max(0, quota.getSmsRemaining() - (int) amount));
                break;
            case DATA:
                quota.setMbRemaining(Math.max(0L, quota.getMbRemaining() - (long) amount));
                break;
        }
        quotaRepository.save(quota);

        // Kullanım kaydı oluştur
        UsageRecord record = new UsageRecord();
        record.setSubscriptionId(event.getSubscriptionId());
        record.setMsisdn(event.getMsisdn());
        record.setType(type);
        record.setQuantity(amount); // Entity field is quantity
        record.setCdrRef(event.getCdrRef());
        record.setRecordedAt(event.getRecordedAt() != null ? event.getRecordedAt() : OffsetDateTime.now());
        usageRecordRepository.save(record);

        log.info("Processed CDR: {} amount of {} for MSISDN: {}", amount, type, event.getMsisdn());
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
}
