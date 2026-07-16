package com.telcox.springmicroservices.orderservice.repository;

import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByPublicId(UUID publicId);
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);
}
