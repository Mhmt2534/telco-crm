package com.telcox.springmicroservices.productcatalog.dto;

import com.telcox.springmicroservices.productcatalog.domain.enums.TariffType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

public record TariffRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull TariffType type,
        @NotNull @PositiveOrZero BigDecimal monthlyFee,
        @PositiveOrZero Integer minutesIncluded,
        @PositiveOrZero Integer smsIncluded,
        @PositiveOrZero Integer dataMbIncluded,
        @NotNull Instant effectiveFrom
) {}
