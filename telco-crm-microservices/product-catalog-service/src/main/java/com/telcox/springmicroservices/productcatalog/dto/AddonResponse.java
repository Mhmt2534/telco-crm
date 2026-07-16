package com.telcox.springmicroservices.productcatalog.dto;

import com.telcox.springmicroservices.productcatalog.domain.enums.AddonType;
import com.telcox.springmicroservices.productcatalog.domain.enums.CatalogStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AddonResponse(
        UUID id,
        String code,
        Integer version,
        String name,
        AddonType type,
        BigDecimal price,
        String currency,
        Integer dataMb,
        Integer minutes,
        Integer smsCount,
        Integer validityDays,
        CatalogStatus status,
        Instant effectiveFrom,
        Instant effectiveTo,
        Instant createdAt
) {}
