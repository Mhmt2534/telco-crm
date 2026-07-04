package com.telcox.springmicroservices.productcatalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffAddonId implements Serializable {

    @Column(name = "tariff_id")
    private Long tariffId;

    @Column(name = "addon_id")
    private Long addonId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TariffAddonId that = (TariffAddonId) o;
        return Objects.equals(tariffId, that.tariffId) &&
               Objects.equals(addonId, that.addonId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tariffId, addonId);
    }
}
