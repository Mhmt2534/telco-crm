package com.telcox.springmicroservices.productcatalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.productcatalog.domain.OutboxEvent;
import com.telcox.springmicroservices.productcatalog.domain.Tariff;
import com.telcox.springmicroservices.productcatalog.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public void publishTariffCreated(Tariff tariff) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("tariffId", tariff.getPublicId());
            payload.put("code", tariff.getCode());
            payload.put("name", tariff.getName());
            payload.put("type", tariff.getType().name());
            payload.put("monthlyFee", tariff.getMonthlyFee());
            payload.put("currency", "TRY");
            payload.put("minutesIncluded", tariff.getMinutesIncluded());
            payload.put("smsIncluded", tariff.getSmsIncluded());
            payload.put("dataMbIncluded", tariff.getDataMbIncluded());
            payload.put("version", tariff.getVersion());
            payload.put("effectiveFrom", ISO_FORMATTER.format(tariff.getEffectiveFrom()));
            payload.put("occurredAt", ISO_FORMATTER.format(Instant.now()));

            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Tariff")
                    .aggregateId(tariff.getPublicId().toString())
                    .eventType("TariffCreated")
                    .payload(payloadJson)
                    .build();

            outboxEventRepository.save(event);
            log.info("TariffCreated event published to outbox for tariff code: {}", tariff.getCode());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TariffCreated event payload", e);
            throw new RuntimeException("Error processing JSON for outbox event", e);
        }
    }

    public void publishTariffPriceChanged(Tariff oldTariff, Tariff newTariff) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("tariffId", newTariff.getPublicId());
            payload.put("code", newTariff.getCode());
            payload.put("oldVersion", oldTariff.getVersion());
            payload.put("newVersion", newTariff.getVersion());
            payload.put("oldMonthlyFee", oldTariff.getMonthlyFee());
            payload.put("newMonthlyFee", newTariff.getMonthlyFee());
            payload.put("currency", "TRY");
            payload.put("effectiveFrom", ISO_FORMATTER.format(newTariff.getEffectiveFrom()));
            payload.put("occurredAt", ISO_FORMATTER.format(Instant.now()));

            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Tariff")
                    .aggregateId(newTariff.getPublicId().toString())
                    .eventType("TariffPriceChanged")
                    .payload(payloadJson)
                    .build();

            outboxEventRepository.save(event);
            log.info("TariffPriceChanged event published to outbox for tariff code: {}", newTariff.getCode());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TariffPriceChanged event payload", e);
            throw new RuntimeException("Error processing JSON for outbox event", e);
        }
    }
}
