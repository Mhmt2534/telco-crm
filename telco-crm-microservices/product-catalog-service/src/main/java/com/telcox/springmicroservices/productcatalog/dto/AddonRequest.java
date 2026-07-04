package com.telcox.springmicroservices.productcatalog.dto;

import com.telcox.springmicroservices.productcatalog.domain.enums.AddonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

public record AddonRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull AddonType type,
        @NotNull @PositiveOrZero BigDecimal price,
        @PositiveOrZero Integer dataMb,
        @PositiveOrZero Integer minutes,
        @PositiveOrZero Integer smsCount,
        @PositiveOrZero Integer validityDays,
        @NotNull Instant effectiveFrom
) {}
