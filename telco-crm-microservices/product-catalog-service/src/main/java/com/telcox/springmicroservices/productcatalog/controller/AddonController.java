package com.telcox.springmicroservices.productcatalog.controller;

import com.telcox.springmicroservices.productcatalog.domain.Addon;
import com.telcox.springmicroservices.productcatalog.dto.AddonRequest;
import com.telcox.springmicroservices.productcatalog.dto.AddonResponse;
import com.telcox.springmicroservices.productcatalog.mapper.CatalogMapper;
import com.telcox.springmicroservices.productcatalog.service.AddonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addons")
@RequiredArgsConstructor
public class AddonController {

    private final AddonService addonService;
    private final CatalogMapper catalogMapper;

    @PostMapping
    public ResponseEntity<AddonResponse> createAddon(@Valid @RequestBody AddonRequest request) {
        Addon saved = addonService.createAddon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogMapper.toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<Page<AddonResponse>> getActiveAddons(
            @RequestParam(required = false) String tariffCode, Pageable pageable) {
        Page<AddonResponse> page = addonService.getActiveAddons(tariffCode, pageable).map(catalogMapper::toResponse);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{code}")
    public ResponseEntity<AddonResponse> getActiveAddon(@PathVariable String code) {
        Addon active = addonService.getActiveAddonByCode(code);
        return ResponseEntity.ok(catalogMapper.toResponse(active));
    }

    @PutMapping("/{code}")
    public ResponseEntity<AddonResponse> updateAddon(@PathVariable String code, @Valid @RequestBody AddonRequest request) {
        Addon updated = addonService.updateAddon(code, request);
        return ResponseEntity.ok(catalogMapper.toResponse(updated));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteAddon(@PathVariable String code) {
        addonService.deleteAddon(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}/history")
    public ResponseEntity<List<AddonResponse>> getAddonHistory(@PathVariable String code) {
        List<AddonResponse> history = addonService.getAddonHistory(code).stream()
                .map(catalogMapper::toResponse)
                .toList();
        return ResponseEntity.ok(history);
    }
}
