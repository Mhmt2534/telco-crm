package com.telcox.springmicroservices.ticket.repository;

import com.telcox.springmicroservices.ticket.entity.Ticket;
import com.telcox.springmicroservices.ticket.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByStatusAndSlaDueAtBefore(TicketStatus status, LocalDateTime now);
    List<Ticket> findByStatusInAndSlaDueAtBefore(List<TicketStatus> statuses, LocalDateTime now);
}
