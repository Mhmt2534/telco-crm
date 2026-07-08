package com.telcox.springmicroservices.billing.repository;

import com.telcox.springmicroservices.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}
