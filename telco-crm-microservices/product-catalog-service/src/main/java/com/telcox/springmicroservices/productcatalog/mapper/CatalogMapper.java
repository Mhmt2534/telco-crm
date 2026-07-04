package com.telcox.springmicroservices.productcatalog.mapper;

import com.telcox.springmicroservices.productcatalog.domain.Addon;
import com.telcox.springmicroservices.productcatalog.domain.Tariff;
import com.telcox.springmicroservices.productcatalog.dto.AddonRequest;
import com.telcox.springmicroservices.productcatalog.dto.AddonResponse;
import com.telcox.springmicroservices.productcatalog.dto.TariffRequest;
import com.telcox.springmicroservices.productcatalog.dto.TariffResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Tariff toEntity(TariffRequest request);

    @Mapping(target = "currency", constant = "TRY")
    TariffResponse toResponse(Tariff tariff);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Addon toEntity(AddonRequest request);

    @Mapping(target = "currency", constant = "TRY")
    AddonResponse toResponse(Addon addon);
}
