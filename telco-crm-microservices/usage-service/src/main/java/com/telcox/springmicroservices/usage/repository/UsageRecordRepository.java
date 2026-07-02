package com.telcox.springmicroservices.usage.repository;

import com.telcox.springmicroservices.usage.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {
    boolean existsByCdrRef(String cdrRef);
}
