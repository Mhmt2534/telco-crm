package com.telcox.springmicroservices.orderservice.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.telcox.springmicroservices.orderservice.domain.enums.SagaStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "saga_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, unique = true, length = 100)
    private String sagaId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "current_step", nullable = false, length = 50)
    private String currentStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SagaStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode payload;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
}
