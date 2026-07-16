package com.telcox.springmicroservices.productcatalog.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record TariffAddonRequest(
        @NotEmpty List<UUID> addonIds
) {}
