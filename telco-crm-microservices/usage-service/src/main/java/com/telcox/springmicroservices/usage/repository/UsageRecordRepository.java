package com.telcox.springmicroservices.usage.repository;

import com.telcox.springmicroservices.usage.dto.OverageSummaryProjection;
import com.telcox.springmicroservices.usage.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {
    boolean existsByCdrRef(String cdrRef);

    @Query("SELECT u.type as type, SUM(u.overageAmount) as totalOverageAmount " +
           "FROM UsageRecord u " +
           "WHERE u.subscriptionId = :subscriptionId " +
           "AND u.recordedAt >= :start " +
           "AND u.recordedAt <= :end " +
           "AND u.overage = true " +
           "GROUP BY u.type")
    List<OverageSummaryProjection> getOverageSummary(
            @Param("subscriptionId") UUID subscriptionId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
