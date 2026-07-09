package com.telcox.springmicroservices.ticket.dto;

import com.telcox.springmicroservices.ticket.entity.TicketPriority;
import lombok.Data;

import java.util.UUID;

@Data
public class TicketCreateRequest {
    private UUID customerId;
    private String category;
    private String description;
    private TicketPriority priority;
}
