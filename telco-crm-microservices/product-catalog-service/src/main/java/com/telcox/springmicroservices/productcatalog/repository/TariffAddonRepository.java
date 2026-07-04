package com.telcox.springmicroservices.productcatalog.repository;

import com.telcox.springmicroservices.productcatalog.domain.TariffAddon;
import com.telcox.springmicroservices.productcatalog.domain.TariffAddonId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TariffAddonRepository extends JpaRepository<TariffAddon, TariffAddonId> {
    List<TariffAddon> findByTariffId(Long tariffId);
}
