package com.telcox.springmicroservices.ticket.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.ticket.entity.OutboxEvent;
import com.telcox.springmicroservices.ticket.entity.OutboxStatus;
import com.telcox.springmicroservices.ticket.entity.Ticket;
import com.telcox.springmicroservices.ticket.entity.TicketStatus;
import com.telcox.springmicroservices.ticket.repository.OutboxEventRepository;
import com.telcox.springmicroservices.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaCheckScheduler {

    private final TicketRepository ticketRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 */1 * * * *") // Run every minute
    @Transactional
    public void checkSlaBreaches() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Running SLA breach check at {}", now);

        List<Ticket> breachedTickets = ticketRepository.findByStatusInAndSlaDueAtBefore(
                List.of(TicketStatus.OPEN, TicketStatus.ASSIGNED), now);

        for (Ticket ticket : breachedTickets) {
            log.warn("Ticket {} SLA breached. Due At: {}", ticket.getId(), ticket.getSlaDueAt());
            
            // Update ticket status
            ticket.setStatus(TicketStatus.BREACHED);
            ticketRepository.save(ticket);
            
            // Publish event
            writeSlaBreachedEvent(ticket, now);
        }
    }

    private void writeSlaBreachedEvent(Ticket ticket, LocalDateTime breachedAt) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("ticketId", ticket.getId().toString());
            payloadMap.put("customerId", ticket.getCustomerId().toString());
            payloadMap.put("category", ticket.getCategory());
            payloadMap.put("priority", ticket.getPriority().name());
            payloadMap.put("slaDueAt", ticket.getSlaDueAt().toString());
            payloadMap.put("breachedAt", breachedAt.toString());

            String payload = objectMapper.writeValueAsString(payloadMap);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Ticket")
                    .aggregateId(ticket.getId().toString())
                    .eventType("SlaBreachedEvent")
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxEventRepository.save(event);
            log.info("SlaBreachedEvent written to outbox for Ticket ID: {}", ticket.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SlaBreachedEvent payload for Ticket ID: {}", ticket.getId(), e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}
