package com.telcox.springmicroservices.productcatalog.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TariffAddonRequest(
        @NotEmpty List<String> addonCodes
) {}
