package com.telcox.springmicroservices.usage.dto;

import com.telcox.springmicroservices.usage.entity.UsageType;

public interface OverageSummaryProjection {
    UsageType getType();
    Double getTotalOverageAmount();
}
