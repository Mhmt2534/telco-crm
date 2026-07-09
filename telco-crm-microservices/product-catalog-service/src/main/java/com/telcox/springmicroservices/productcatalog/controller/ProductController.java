package com.telcox.springmicroservices.productcatalog.controller;

import com.telcox.springmicroservices.productcatalog.domain.Addon;
import com.telcox.springmicroservices.productcatalog.domain.Tariff;
import com.telcox.springmicroservices.productcatalog.dto.ProductResponse;
import com.telcox.springmicroservices.productcatalog.service.AddonService;
import com.telcox.springmicroservices.productcatalog.service.TariffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final TariffService tariffService;
    private final AddonService addonService;

    @GetMapping("/batch")
    public ResponseEntity<List<ProductResponse>> getProductsByCodes(@RequestParam("codes") List<String> codes) {
        List<ProductResponse> responses = new ArrayList<>();
        
        for (String code : codes) {
            try {
                Tariff tariff = tariffService.getActiveTariffByCode(code);
                responses.add(ProductResponse.builder()
                        .productCode(tariff.getCode())
                        .name(tariff.getName())
                        .price(tariff.getMonthlyFee())
                        .status(tariff.getStatus().name())
                        .build());
                continue;
            } catch (Exception e) {
                // Not a tariff, try addon
            }
            
            try {
                Addon addon = addonService.getActiveAddonByCode(code);
                responses.add(ProductResponse.builder()
                        .productCode(addon.getCode())
                        .name(addon.getName())
                        .price(addon.getPrice())
                        .status("ACTIVE") // Assuming Addons are active if found
                        .build());
            } catch (Exception e) {
                // Not found, ignore or could throw exception based on requirement
            }
        }
        
        if (responses.isEmpty() && !codes.isEmpty()) {
            throw new com.telcox.common.core.exception.ResourceNotFoundException("Products not found for codes: " + codes);
        }
        
        return ResponseEntity.ok(responses);
    }
}
