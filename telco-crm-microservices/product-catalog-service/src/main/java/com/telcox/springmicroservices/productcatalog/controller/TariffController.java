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

    @GetMapping("/{code}")
    public ResponseEntity<TariffResponse> getActiveTariff(@PathVariable String code) {
        Tariff active = tariffService.getActiveTariffByCode(code);
        return ResponseEntity.ok(catalogMapper.toResponse(active));
    }

    @PutMapping("/{code}")
    public ResponseEntity<TariffResponse> updateTariff(@PathVariable String code, @Valid @RequestBody TariffRequest request) {
        Tariff updated = tariffService.updateTariff(code, request);
        return ResponseEntity.ok(catalogMapper.toResponse(updated));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteTariff(@PathVariable String code) {
        tariffService.deleteTariff(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}/history")
    public ResponseEntity<List<TariffResponse>> getTariffHistory(@PathVariable String code) {
        List<TariffResponse> history = tariffService.getTariffHistory(code).stream()
                .map(catalogMapper::toResponse)
                .toList();
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{code}/addons")
    public ResponseEntity<Void> addAddonsToTariff(@PathVariable String code, @Valid @RequestBody TariffAddonRequest request) {
        addonService.addAddonsToTariff(code, request.addonCodes());
        return ResponseEntity.ok().build();
    }
}
