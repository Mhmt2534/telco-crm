package com.telcox.springmicroservices.usage.scheduler;

import com.telcox.springmicroservices.usage.entity.Quota;
import com.telcox.springmicroservices.usage.entity.QuotaStatus;
import com.telcox.springmicroservices.usage.repository.QuotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@EnableScheduling
public class QuotaScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuotaScheduler.class);

    private final QuotaRepository quotaRepository;

    public QuotaScheduler(QuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    @Scheduled(cron = "${app.quota.activation.cron:0 0 0 * * *}")
    @Transactional
    public void activatePendingQuotas() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Quota> pendingQuotas = quotaRepository.findByStatusAndPeriodStartLessThanEqual(QuotaStatus.PENDING, now);

        if (!pendingQuotas.isEmpty()) {
            log.info("[USAGE SCHEDULER] Bulunan aktivasyon bekleyen PENDING kota sayısı: {}", pendingQuotas.size());
            for (Quota quota : pendingQuotas) {
                quota.setStatus(QuotaStatus.ACTIVE);
                log.info("[USAGE SCHEDULER] Kota aktif edildi. SubscriptionId: {}", quota.getSubscriptionId());
            }
            quotaRepository.saveAll(pendingQuotas);
        }
    }
}
