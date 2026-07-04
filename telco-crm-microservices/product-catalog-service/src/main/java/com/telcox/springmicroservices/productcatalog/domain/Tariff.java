package com.telcox.springmicroservices.productcatalog.domain;

import com.telcox.springmicroservices.productcatalog.domain.enums.CatalogStatus;
import com.telcox.springmicroservices.productcatalog.domain.enums.TariffType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tariff", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "version"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TariffType type;

    @Column(name = "monthly_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyFee;

    @Column(name = "minutes_included")
    @Builder.Default
    private Integer minutesIncluded = 0;

    @Column(name = "sms_included")
    @Builder.Default
    private Integer smsIncluded = 0;

    @Column(name = "data_mb_included")
    @Builder.Default
    private Integer dataMbIncluded = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CatalogStatus status = CatalogStatus.ACTIVE;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
