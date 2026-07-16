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
import java.util.UUID;

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
            @RequestParam(required = false) UUID tariffId, Pageable pageable) {
        Page<AddonResponse> page = addonService.getActiveAddons(tariffId, pageable).map(catalogMapper::toResponse);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddonResponse> getActiveAddon(@PathVariable UUID id) {
        Addon active = addonService.getActiveAddon(id);
        return ResponseEntity.ok(catalogMapper.toResponse(active));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddonResponse> updateAddon(@PathVariable UUID id, @Valid @RequestBody AddonRequest request) {
        Addon updated = addonService.updateAddon(id, request);
        return ResponseEntity.ok(catalogMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddon(@PathVariable UUID id) {
        addonService.deleteAddon(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<AddonResponse>> getAddonHistory(@PathVariable UUID id) {
        List<AddonResponse> history = addonService.getAddonHistory(id).stream()
                .map(catalogMapper::toResponse)
                .toList();
        return ResponseEntity.ok(history);
    }
}
