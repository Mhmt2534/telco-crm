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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

        Addon savedAddon = addonRepository.save(addon);

        for (UUID tariffId : request.tariffIds()) {
            Tariff activeTariff = tariffRepository.findByPublicIdAndStatus(tariffId, CatalogStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktif tarife bulunamadı: " + tariffId));
            
            TariffAddonId id = new TariffAddonId(activeTariff.getId(), savedAddon.getId());
            TariffAddon tariffAddon = TariffAddon.builder()
                    .id(id)
                    .tariff(activeTariff)
                    .addon(savedAddon)
                    .build();
            tariffAddonRepository.save(tariffAddon);
        }

        return savedAddon;
    }

    @Transactional(readOnly = true)
    public Page<Addon> getActiveAddons(UUID tariffId, Pageable pageable) {
        if (tariffId != null) {
            Tariff activeTariff = tariffRepository.findByPublicIdAndStatus(tariffId, CatalogStatus.ACTIVE)
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

    @Transactional(readOnly = true)
    public Addon getActiveAddon(UUID publicId) {
        return addonRepository.findByPublicIdAndStatus(publicId, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Addon not found"));
    }

    @Transactional
    public Addon updateAddon(UUID publicId, AddonRequest request) {
        Addon activeAddon = addonRepository.findByPublicIdAndStatus(publicId, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active addon not found"));

        if (!activeAddon.getCode().equals(request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code değiştirilemez");
        }

        if (activeAddon.getType() != request.type()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type değiştirilemez");
        }

        activeAddon.setStatus(CatalogStatus.DEPRECATED);
        activeAddon.setEffectiveTo(Instant.now());
        addonRepository.save(activeAddon);

        Addon newAddon = catalogMapper.toEntity(request);
        newAddon.setVersion(activeAddon.getVersion() + 1);
        newAddon.setStatus(CatalogStatus.ACTIVE);
        
        Addon savedNewAddon = addonRepository.save(newAddon);

        List<TariffAddon> oldRelations = tariffAddonRepository.findByAddonId(activeAddon.getId());
        Set<UUID> oldTariffIds = oldRelations.stream()
                .map(r -> r.getTariff().getPublicId())
                .collect(Collectors.toSet());

        for (TariffAddon oldRelation : oldRelations) {
            TariffAddonId newId = new TariffAddonId(oldRelation.getTariff().getId(), savedNewAddon.getId());
            TariffAddon newTariffAddon = TariffAddon.builder()
                    .id(newId)
                    .tariff(oldRelation.getTariff())
                    .addon(savedNewAddon)
                    .build();
            tariffAddonRepository.save(newTariffAddon);
        }

        for (UUID tariffId : request.tariffIds()) {
            if (!oldTariffIds.contains(tariffId)) {
                Tariff activeTariff = tariffRepository.findByPublicIdAndStatus(tariffId, CatalogStatus.ACTIVE)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktif tarife bulunamadı: " + tariffId));
                TariffAddonId id = new TariffAddonId(activeTariff.getId(), savedNewAddon.getId());
                TariffAddon newTariffAddon = TariffAddon.builder()
                        .id(id)
                        .tariff(activeTariff)
                        .addon(savedNewAddon)
                        .build();
                tariffAddonRepository.save(newTariffAddon);
            }
        }

        return savedNewAddon;
    }

    @Transactional
    public void deleteAddon(UUID publicId) {
        Addon activeAddon = addonRepository.findByPublicIdAndStatus(publicId, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active addon not found"));
        
        activeAddon.setStatus(CatalogStatus.DEPRECATED);
        activeAddon.setEffectiveTo(Instant.now());
        addonRepository.save(activeAddon);
    }

    @Transactional(readOnly = true)
    public List<Addon> getAddonHistory(UUID publicId) {
        Addon addon = addonRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Addon not found"));
        return addonRepository.findByCodeOrderByVersionDesc(addon.getCode());
    }

    @Transactional
    public void addAddonsToTariff(UUID tariffId, List<UUID> addonIds) {
        Tariff activeTariff = tariffRepository.findByPublicIdAndStatus(tariffId, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active tariff not found"));

        for (UUID addonId : addonIds) {
            Addon activeAddon = addonRepository.findByPublicIdAndStatus(addonId, CatalogStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktif olmayan ek pakete işlem yapılamaz: " + addonId));
            
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
