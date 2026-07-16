package com.telcox.springmicroservices.billing.repository;

import com.telcox.springmicroservices.billing.entity.Invoice;
import com.telcox.springmicroservices.billing.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDateTime dateTime);
    Optional<Invoice> findByPublicId(UUID publicId);
}
