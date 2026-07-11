package com.telcox.springmicroservices.billing.client;

import com.telcox.springmicroservices.billing.dto.UsageOverageSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "usage-service", url = "${app.feign.usage-service-url:}")
public interface UsageServiceClient {

    @GetMapping("/api/v1/usage/subscriptions/{id}/overage-summary")
    List<UsageOverageSummaryDto> getOverageSummary(
            @PathVariable("id") UUID id,
            @RequestParam("start") String start,
            @RequestParam("end") String end
    );
}
