package com.telcox.springmicroservices.subscription.repository;

import java.util.List;
import java.util.Optional;

import com.telcox.springmicroservices.subscription.entity.MsisdnPool;
import com.telcox.springmicroservices.subscription.entity.MsisdnStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MsisdnPoolRepository extends JpaRepository<MsisdnPool, String> {

    Optional<MsisdnPool> findByMsisdnAndStatus(String msisdn, MsisdnStatus status);

    /**
     * Havuzdan belirtilen statüdeki ilk N numarayı döner.
     * Pageable(0, 1) ile çağrılarak tek bir FREE numara seçilir.
     */
    List<MsisdnPool> findFirstByStatus(MsisdnStatus status, Pageable pageable);
}

