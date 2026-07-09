package com.telcox.springmicroservices.ticket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.ticket.dto.TicketCreateRequest;
import com.telcox.springmicroservices.ticket.dto.TicketResponse;
import com.telcox.springmicroservices.ticket.entity.*;
import com.telcox.springmicroservices.ticket.repository.OutboxEventRepository;
import com.telcox.springmicroservices.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TicketResponse createTicket(TicketCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slaDueAt = calculateSlaDueAt(request.getPriority(), now);

        Ticket ticket = Ticket.builder()
                .customerId(request.getCustomerId())
                .category(request.getCategory())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(TicketStatus.OPEN)
                .slaDueAt(slaDueAt)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        
        writeTicketOpenedEvent(savedTicket);

        return mapToResponse(savedTicket);
    }

    private LocalDateTime calculateSlaDueAt(TicketPriority priority, LocalDateTime now) {
        return switch (priority) {
            case CRITICAL -> now.plusHours(4);
            case HIGH -> now.plusHours(24);
            case MEDIUM -> now.plusHours(72);
        };
    }

    private void writeTicketOpenedEvent(Ticket ticket) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("ticketId", ticket.getId().toString());
            payloadMap.put("customerId", ticket.getCustomerId().toString());
            payloadMap.put("category", ticket.getCategory());
            payloadMap.put("priority", ticket.getPriority().name());
            payloadMap.put("slaDueAt", ticket.getSlaDueAt().toString());

            String payload = objectMapper.writeValueAsString(payloadMap);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Ticket")
                    .aggregateId(ticket.getId().toString())
                    .eventType("TicketOpened")
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxEventRepository.save(event);
            log.info("TicketOpened event written to outbox for Ticket ID: {}", ticket.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TicketOpened event payload for Ticket ID: {}", ticket.getId(), e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .customerId(ticket.getCustomerId())
                .category(ticket.getCategory())
                .description(ticket.getDescription())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .slaDueAt(ticket.getSlaDueAt())
                .resolvedAt(ticket.getResolvedAt())
                .createdAt(ticket.getCreatedAt())
                .build();
    }
}
