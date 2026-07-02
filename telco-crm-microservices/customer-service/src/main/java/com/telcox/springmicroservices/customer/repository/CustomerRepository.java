package com.telcox.springmicroservices.customer.repository;

import com.telcox.springmicroservices.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByIdentityNumber(String identityNumber);
    boolean existsByPhone(String phone);
    boolean existsByIdentityNumber(String identityNumber);
}
