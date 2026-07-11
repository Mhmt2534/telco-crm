package com.telcox.springmicroservices.usage.controller;

import com.telcox.springmicroservices.usage.dto.OverageSummaryProjection;
import com.telcox.springmicroservices.usage.dto.QuotaResponse;
import com.telcox.springmicroservices.usage.service.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usage")
@Tag(name = "Usage & Quota", description = "Abonelik kota ve kullanım sorgulama endpoint'leri")
public class UsageController {

    private final QuotaService quotaService;

    public UsageController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    /**
     * Bir abonenin aktif dönem için anlık kalan kota bilgisini döner.
     * KART 8: GET /api/v1/usage/subscriptions/{id}/quota
     */
    @GetMapping("/subscriptions/{id}/quota")
    @Operation(
        summary = "Abonelik kotasını getir",
        description = "Verilen abonelik ID'sine ait anlık kalan dakika, SMS ve MB haklarını döner. " +
                      "MB kullanım yüzdesi (mbUsagePercent) %80 ve %100 eşik bildirimleri için kullanılır."
    )
    public ResponseEntity<QuotaResponse> getQuota(@PathVariable UUID id) {
        return ResponseEntity.ok(quotaService.getQuotaBySubscriptionId(id));
    }

    /**
     * Bir abonenin belirli tarih aralığındaki kota aşım özetini döner.
     * KART 27: GET /api/v1/usage/subscriptions/{id}/overage-summary
     */
    @GetMapping("/subscriptions/{id}/overage-summary")
    @Operation(
        summary = "Abonelik kota aşım özetini getir",
        description = "Verilen abonelik ID'si ve tarih aralığı için aşım tiplerine göre toplam aşım miktarlarını döner."
    )
    public ResponseEntity<List<OverageSummaryProjection>> getOverageSummary(
            @PathVariable UUID id,
            @RequestParam Instant start,
            @RequestParam Instant end
    ) {
        return ResponseEntity.ok(quotaService.getOverageSummary(id, start, end));
    }
}
