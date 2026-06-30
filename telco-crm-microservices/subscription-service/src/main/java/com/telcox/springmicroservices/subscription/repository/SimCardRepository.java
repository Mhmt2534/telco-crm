package com.telcox.springmicroservices.subscription.repository;

import com.telcox.springmicroservices.subscription.entity.SimCard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimCardRepository extends JpaRepository<SimCard, String> {
}
