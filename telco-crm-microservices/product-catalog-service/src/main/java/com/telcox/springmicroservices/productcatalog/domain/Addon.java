package com.telcox.springmicroservices.productcatalog.domain;

import com.telcox.springmicroservices.productcatalog.domain.enums.AddonType;
import com.telcox.springmicroservices.productcatalog.domain.enums.CatalogStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "addon", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "version"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Addon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    @Builder.Default
    private UUID publicId = UUID.randomUUID();

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddonType type;

    @Column(name = "validity_days")
    @Builder.Default
    private Integer validityDays = 30;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "data_mb")
    @Builder.Default
    private Integer dataMb = 0;

    @Builder.Default
    private Integer minutes = 0;

    @Column(name = "sms_count")
    @Builder.Default
    private Integer smsCount = 0;

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
