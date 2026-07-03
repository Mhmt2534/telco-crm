package com.telcox.springmicroservices.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.customer.domain.Customer;
import com.telcox.springmicroservices.customer.domain.OutboxEvent;
import com.telcox.springmicroservices.customer.domain.events.CustomerRegisteredEvent;
import com.telcox.springmicroservices.customer.domain.events.CustomerUpdatedEvent;
import com.telcox.springmicroservices.customer.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void publishCustomerRegistered(Customer customer) {
        try {
            CustomerRegisteredEvent eventPayload = CustomerRegisteredEvent.builder()
                    .customerId(customer.getId())
                    .firstName(customer.getFirstName())
                    .lastName(customer.getLastName())
                    .phone(customer.getPhone())
                    .email(customer.getEmail())
                    .type(customer.getType() != null ? customer.getType().name() : null)
                    .status(customer.getStatus() != null ? customer.getStatus().name() : null)
                    .occurredAt(Instant.now().toString())
                    .build();

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType("Customer");
            outboxEvent.setAggregateId(String.valueOf(customer.getId()));
            outboxEvent.setEventType("CustomerRegistered");
            outboxEvent.setPayload(objectMapper.writeValueAsString(eventPayload));

            outboxEventRepository.save(outboxEvent);
            log.info("CustomerRegistered eventi outbox'a yazıldı. Müşteri ID: {}", customer.getId());
        } catch (Exception e) {
            log.error("CustomerRegistered eventi oluşturulamadı", e);
            throw new RuntimeException("Event serialize edilemedi", e);
        }
    }

    public void publishCustomerUpdated(Customer customer, String changeReason) {
        try {
            CustomerUpdatedEvent eventPayload = CustomerUpdatedEvent.builder()
                    .customerId(customer.getId())
                    .keycloakUserId(customer.getKeycloakUserId())
                    .firstName(customer.getFirstName())
                    .lastName(customer.getLastName())
                    .phone(customer.getPhone())
                    .email(customer.getEmail())
                    .status(customer.getStatus() != null ? customer.getStatus().name() : null)
                    .changeReason(changeReason)
                    .occurredAt(Instant.now().toString())
                    .build();

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType("Customer");
            outboxEvent.setAggregateId(String.valueOf(customer.getId()));
            outboxEvent.setEventType("CustomerUpdated");
            outboxEvent.setPayload(objectMapper.writeValueAsString(eventPayload));

            outboxEventRepository.save(outboxEvent);
            log.info("CustomerUpdated eventi outbox'a yazıldı. Müşteri ID: {}, Sebep: {}", customer.getId(), changeReason);
        } catch (Exception e) {
            log.error("CustomerUpdated eventi oluşturulamadı", e);
            throw new RuntimeException("Event serialize edilemedi", e);
        }
    }
}
