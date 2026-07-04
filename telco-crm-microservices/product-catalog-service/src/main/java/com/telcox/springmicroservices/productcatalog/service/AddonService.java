package com.telcox.springmicroservices.productcatalog.service;

import com.telcox.springmicroservices.productcatalog.domain.Addon;
import com.telcox.springmicroservices.productcatalog.domain.Tariff;
import com.telcox.springmicroservices.productcatalog.domain.TariffAddon;
import com.telcox.springmicroservices.productcatalog.domain.TariffAddonId;
import com.telcox.springmicroservices.productcatalog.domain.enums.CatalogStatus;
import com.telcox.springmicroservices.productcatalog.dto.AddonRequest;
import com.telcox.springmicroservices.productcatalog.mapper.CatalogMapper;
import com.telcox.springmicroservices.productcatalog.repository.AddonRepository;
import com.telcox.springmicroservices.productcatalog.repository.TariffAddonRepository;
import com.telcox.springmicroservices.productcatalog.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddonService {

    private final AddonRepository addonRepository;
    private final TariffRepository tariffRepository;
    private final TariffAddonRepository tariffAddonRepository;
    private final CatalogMapper catalogMapper;

    @Transactional
    public Addon createAddon(AddonRequest request) {
        if (addonRepository.findByCodeAndStatus(request.code(), CatalogStatus.ACTIVE).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu code ile aktif addon zaten mevcut");
        }

        Addon addon = catalogMapper.toEntity(request);
        addon.setVersion(1);
        addon.setStatus(CatalogStatus.ACTIVE);

        return addonRepository.save(addon);
    }

    @Transactional(readOnly = true)
    public Page<Addon> getActiveAddons(String tariffCode, Pageable pageable) {
        if (tariffCode != null && !tariffCode.isBlank()) {
            Tariff activeTariff = tariffRepository.findByCodeAndStatus(tariffCode, CatalogStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active tariff not found"));
            
            List<Addon> addons = tariffAddonRepository.findByTariffId(activeTariff.getId())
                    .stream()
                    .map(TariffAddon::getAddon)
                    .filter(a -> a.getStatus() == CatalogStatus.ACTIVE)
                    .toList();
            
            // Simple list wrapping for this scenario
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), addons.size());
            if (start > addons.size()) {
                return new PageImpl<>(List.of(), pageable, addons.size());
            }
            return new PageImpl<>(addons.subList(start, end), pageable, addons.size());
        }

        return addonRepository.findByStatus(CatalogStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Addon getActiveAddonByCode(String code) {
        return addonRepository.findByCodeAndStatus(code, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Addon not found"));
    }

    @Transactional
    public Addon updateAddon(String code, AddonRequest request) {
        if (!code.equals(request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code değiştirilemez");
        }

        Addon activeAddon = addonRepository.findByCodeAndStatus(code, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active addon not found"));

        if (activeAddon.getType() != request.type()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type değiştirilemez");
        }

        activeAddon.setStatus(CatalogStatus.DEPRECATED);
        activeAddon.setEffectiveTo(Instant.now());
        addonRepository.save(activeAddon);

        Addon newAddon = catalogMapper.toEntity(request);
        newAddon.setVersion(activeAddon.getVersion() + 1);
        newAddon.setStatus(CatalogStatus.ACTIVE);
        
        return addonRepository.save(newAddon);
    }

    @Transactional
    public void deleteAddon(String code) {
        Addon activeAddon = addonRepository.findByCodeAndStatus(code, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active addon not found"));
        
        activeAddon.setStatus(CatalogStatus.DEPRECATED);
        activeAddon.setEffectiveTo(Instant.now());
        addonRepository.save(activeAddon);
    }

    @Transactional(readOnly = true)
    public List<Addon> getAddonHistory(String code) {
        return addonRepository.findByCodeOrderByVersionDesc(code);
    }

    @Transactional
    public void addAddonsToTariff(String tariffCode, List<String> addonCodes) {
        Tariff activeTariff = tariffRepository.findByCodeAndStatus(tariffCode, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active tariff not found"));

        for (String addonCode : addonCodes) {
            Addon activeAddon = addonRepository.findByCodeAndStatus(addonCode, CatalogStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktif olmayan tarife veya ek paketlere işlem yapılamaz: " + addonCode));
            
            TariffAddonId id = new TariffAddonId(activeTariff.getId(), activeAddon.getId());
            if (!tariffAddonRepository.existsById(id)) {
                TariffAddon tariffAddon = TariffAddon.builder()
                        .id(id)
                        .tariff(activeTariff)
                        .addon(activeAddon)
                        .build();
                tariffAddonRepository.save(tariffAddon);
            }
        }
    }
}
