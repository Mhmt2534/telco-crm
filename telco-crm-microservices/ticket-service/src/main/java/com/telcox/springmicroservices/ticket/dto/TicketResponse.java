package com.telcox.springmicroservices.ticket.dto;

import com.telcox.springmicroservices.ticket.entity.TicketPriority;
import com.telcox.springmicroservices.ticket.entity.TicketStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TicketResponse {
    private UUID id;
    private UUID customerId;
    private String category;
    private String description;
    private TicketPriority priority;
    private TicketStatus status;
    private LocalDateTime slaDueAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
