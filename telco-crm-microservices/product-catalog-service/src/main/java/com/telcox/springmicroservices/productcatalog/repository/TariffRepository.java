package com.telcox.springmicroservices.productcatalog.repository;

import com.telcox.springmicroservices.productcatalog.domain.Tariff;
import com.telcox.springmicroservices.productcatalog.domain.enums.CatalogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TariffRepository extends JpaRepository<Tariff, Long> {
    Optional<Tariff> findByPublicId(UUID publicId);
    Optional<Tariff> findByPublicIdAndStatus(UUID publicId, CatalogStatus status);
    Optional<Tariff> findByCodeAndStatus(String code, CatalogStatus status);
    Page<Tariff> findByStatus(CatalogStatus status, Pageable pageable);
    List<Tariff> findByCodeOrderByVersionDesc(String code);
    boolean existsByCodeAndStatus(String code, CatalogStatus status);
}
