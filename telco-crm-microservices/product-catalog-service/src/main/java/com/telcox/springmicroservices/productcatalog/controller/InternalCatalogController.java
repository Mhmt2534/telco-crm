package com.telcox.springmicroservices.productcatalog.controller;

import com.telcox.springmicroservices.productcatalog.domain.Tariff;
import com.telcox.springmicroservices.productcatalog.dto.TariffResponse;
import com.telcox.springmicroservices.productcatalog.mapper.CatalogMapper;
import com.telcox.springmicroservices.productcatalog.service.TariffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/tariffs")
@RequiredArgsConstructor
public class InternalCatalogController {

    private final TariffService tariffService;
    private final CatalogMapper catalogMapper;

    @GetMapping("/{id}/active")
    public ResponseEntity<TariffResponse> getActiveTariff(@PathVariable UUID id) {
        Tariff active = tariffService.getActiveTariff(id);
        return ResponseEntity.ok(catalogMapper.toResponse(active));
    }
}
