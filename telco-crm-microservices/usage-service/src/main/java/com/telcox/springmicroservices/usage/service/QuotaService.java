package com.telcox.springmicroservices.usage.service;

import com.telcox.springmicroservices.usage.dto.QuotaResponse;
import com.telcox.springmicroservices.usage.entity.Quota;
import com.telcox.springmicroservices.usage.repository.QuotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class QuotaService {

    private final QuotaRepository quotaRepository;

    public QuotaService(QuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
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
