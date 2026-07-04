package com.telcox.springmicroservices.productcatalog.service;

import com.telcox.springmicroservices.productcatalog.domain.Tariff;
import com.telcox.springmicroservices.productcatalog.domain.TariffAddon;
import com.telcox.springmicroservices.productcatalog.domain.TariffAddonId;
import com.telcox.springmicroservices.productcatalog.domain.enums.CatalogStatus;
import com.telcox.springmicroservices.productcatalog.dto.TariffRequest;
import com.telcox.springmicroservices.productcatalog.mapper.CatalogMapper;
import com.telcox.springmicroservices.productcatalog.repository.TariffAddonRepository;
import com.telcox.springmicroservices.productcatalog.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
public class TariffService {

    private final TariffRepository tariffRepository;
    private final TariffAddonRepository tariffAddonRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final CatalogMapper catalogMapper;

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Tariff createTariff(TariffRequest request) {
        if (tariffRepository.existsByCodeAndStatus(request.code(), CatalogStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu code ile aktif tarife zaten mevcut, güncelleme için PUT kullanın");
        }

        Tariff tariff = catalogMapper.toEntity(request);
        tariff.setVersion(1);
        tariff.setStatus(CatalogStatus.ACTIVE);

        Tariff savedTariff = tariffRepository.save(tariff);
        outboxEventPublisher.publishTariffCreated(savedTariff);

        return savedTariff;
    }

    @Transactional(readOnly = true)
    public Page<Tariff> getActiveTariffs(Pageable pageable) {
        return tariffRepository.findByStatus(CatalogStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Tariff getActiveTariffByCode(String code) {
        return tariffRepository.findByCodeAndStatus(code, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tariff not found"));
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Tariff updateTariff(String code, TariffRequest request) {
        if (!code.equals(request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code değiştirilemez");
        }

        Tariff activeTariff = tariffRepository.findByCodeAndStatus(code, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active tariff not found"));

        if (activeTariff.getType() != request.type()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type değiştirilemez");
        }

        // Deprecate old
        activeTariff.setStatus(CatalogStatus.DEPRECATED);
        activeTariff.setEffectiveTo(Instant.now());
        tariffRepository.save(activeTariff);

        // Create new version
        Tariff newTariff = catalogMapper.toEntity(request);
        newTariff.setVersion(activeTariff.getVersion() + 1);
        newTariff.setStatus(CatalogStatus.ACTIVE);
        
        Tariff savedNewTariff = tariffRepository.save(newTariff);

        // Copy TariffAddon relationships
        List<TariffAddon> oldAddons = tariffAddonRepository.findByTariffId(activeTariff.getId());
        for (TariffAddon oldAddon : oldAddons) {
            TariffAddon newTariffAddon = TariffAddon.builder()
                    .id(new TariffAddonId(savedNewTariff.getId(), oldAddon.getAddon().getId()))
                    .tariff(savedNewTariff)
                    .addon(oldAddon.getAddon())
                    .build();
            tariffAddonRepository.save(newTariffAddon);
        }

        outboxEventPublisher.publishTariffPriceChanged(activeTariff, savedNewTariff);
        return savedNewTariff;
    }

    @Transactional
    public void deleteTariff(String code) {
        Tariff activeTariff = tariffRepository.findByCodeAndStatus(code, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active tariff not found"));
        
        activeTariff.setStatus(CatalogStatus.DEPRECATED);
        activeTariff.setEffectiveTo(Instant.now());
        tariffRepository.save(activeTariff);
    }

    @Transactional(readOnly = true)
    public List<Tariff> getTariffHistory(String code) {
        return tariffRepository.findByCodeOrderByVersionDesc(code);
    }
}
