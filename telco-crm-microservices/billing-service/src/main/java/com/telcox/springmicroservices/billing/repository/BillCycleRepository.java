package com.telcox.springmicroservices.billing.repository;

import com.telcox.springmicroservices.billing.entity.BillCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillCycleRepository extends JpaRepository<BillCycle, Long> {
    List<BillCycle> findByCutOffDay(Integer cutOffDay);
}
