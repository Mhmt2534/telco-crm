package com.telcox.springmicroservices.productcatalog.repository;

import com.telcox.springmicroservices.productcatalog.domain.Addon;
import com.telcox.springmicroservices.productcatalog.domain.enums.CatalogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddonRepository extends JpaRepository<Addon, Long> {
    Optional<Addon> findByCodeAndStatus(String code, CatalogStatus status);
    Page<Addon> findByStatus(CatalogStatus status, Pageable pageable);
    List<Addon> findByCodeOrderByVersionDesc(String code);
}
