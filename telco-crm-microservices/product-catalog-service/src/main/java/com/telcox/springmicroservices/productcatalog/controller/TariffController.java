package com.telcox.springmicroservices.productcatalog.controller;

import com.telcox.springmicroservices.productcatalog.domain.Tariff;
import com.telcox.springmicroservices.productcatalog.dto.TariffAddonRequest;
import com.telcox.springmicroservices.productcatalog.dto.TariffRequest;
import com.telcox.springmicroservices.productcatalog.dto.TariffResponse;
import com.telcox.springmicroservices.productcatalog.mapper.CatalogMapper;
import com.telcox.springmicroservices.productcatalog.service.AddonService;
import com.telcox.springmicroservices.productcatalog.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tariffs")
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;
    private final AddonService addonService;
    private final CatalogMapper catalogMapper;

    @PostMapping
    public ResponseEntity<TariffResponse> createTariff(@Valid @RequestBody TariffRequest request) {
        Tariff saved = tariffService.createTariff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogMapper.toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<Page<TariffResponse>> getActiveTariffs(Pageable pageable) {
        List<Tariff> allTariffs = tariffService.getAllActiveTariffsList();
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allTariffs.size());
        
        List<TariffResponse> responses;
        if (start > allTariffs.size()) {
            responses = List.of();
        } else {
            responses = allTariffs.subList(start, end).stream()
                    .map(catalogMapper::toResponse)
                    .toList();
        }
        
        Page<TariffResponse> page = new PageImpl<>(responses, pageable, allTariffs.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TariffResponse> getActiveTariff(@PathVariable UUID id) {
        Tariff active = tariffService.getActiveTariff(id);
        return ResponseEntity.ok(catalogMapper.toResponse(active));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TariffResponse> updateTariff(@PathVariable UUID id, @Valid @RequestBody TariffRequest request) {
        Tariff updated = tariffService.updateTariff(id, request);
        return ResponseEntity.ok(catalogMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariff(@PathVariable UUID id) {
        tariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TariffResponse>> getTariffHistory(@PathVariable UUID id) {
        List<TariffResponse> history = tariffService.getTariffHistory(id).stream()
                .map(catalogMapper::toResponse)
                .toList();
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/addons")
    public ResponseEntity<Void> addAddonsToTariff(@PathVariable UUID id, @Valid @RequestBody TariffAddonRequest request) {
        addonService.addAddonsToTariff(id, request.addonIds());
        return ResponseEntity.ok().build();
    }
}
