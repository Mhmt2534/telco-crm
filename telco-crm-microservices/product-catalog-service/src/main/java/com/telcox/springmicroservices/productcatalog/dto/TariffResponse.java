package com.telcox.springmicroservices.productcatalog.dto;

import com.telcox.springmicroservices.productcatalog.domain.enums.CatalogStatus;
import com.telcox.springmicroservices.productcatalog.domain.enums.TariffType;

import java.math.BigDecimal;
import java.time.Instant;

public record TariffResponse(
        Long id,
        String code,
        Integer version,
        String name,
        TariffType type,
        BigDecimal monthlyFee,
        String currency,
        Integer minutesIncluded,
        Integer smsIncluded,
        Integer dataMbIncluded,
        CatalogStatus status,
        Instant effectiveFrom,
        Instant effectiveTo,
        Instant createdAt
) {}
