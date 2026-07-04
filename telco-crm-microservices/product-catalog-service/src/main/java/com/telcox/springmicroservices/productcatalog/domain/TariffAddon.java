package com.telcox.springmicroservices.productcatalog.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tariff_addon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffAddon {

    @EmbeddedId
    private TariffAddonId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tariffId")
    @JoinColumn(name = "tariff_id")
    private Tariff tariff;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("addonId")
    @JoinColumn(name = "addon_id")
    private Addon addon;
}
