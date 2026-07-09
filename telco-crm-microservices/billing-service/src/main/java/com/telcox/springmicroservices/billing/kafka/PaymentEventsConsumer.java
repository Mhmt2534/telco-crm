package com.telcox.springmicroservices.billing.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.billing.dto.PaymentCompletedEvent;
import com.telcox.springmicroservices.billing.entity.Invoice;
import com.telcox.springmicroservices.billing.entity.InvoiceStatus;
import com.telcox.springmicroservices.billing.entity.OutboxEvent;
import com.telcox.springmicroservices.billing.entity.OutboxStatus;
import com.telcox.springmicroservices.billing.repository.InvoiceRepository;
import com.telcox.springmicroservices.billing.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsConsumer.class);

    private final InvoiceRepository invoiceRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventsConsumer(InvoiceRepository invoiceRepository,
                                 OutboxEventRepository outboxEventRepository,
                                 ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-events:telcox.Payment.events}", groupId = "billing-service-group")
    @Transactional
    public void consume(String message, @Header(name = "eventType", required = false) String headerEventType) {
        log.info("Received raw Payment event: {}, eventType header: {}", message, headerEventType);
        String jsonPayload = message;

        // Clean up double serialization if any
        try {
            JsonNode parsedNode = objectMapper.readTree(message);
            if (parsedNode.isTextual()) {
                jsonPayload = parsedNode.asText();
            }
        } catch (Exception e) {
            log.debug("Message is not a JSON TextNode, treating as raw JSON.");
        }

        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            JsonNode payloadNode = root.has("payload") ? root.get("payload") : root;
            JsonNode afterNode = payloadNode.has("after") ? payloadNode.get("after") : payloadNode;

            String eventType = headerEventType;
            if (eventType == null || eventType.isEmpty()) {
                if (afterNode.has("event_type")) {
                    eventType = afterNode.get("event_type").asText();
                } else if (afterNode.has("eventType")) {
                    eventType = afterNode.get("eventType").asText();
                } else if (root.has("eventType")) {
                    eventType = root.get("eventType").asText();
                }
            }


            if (!"PaymentCompleted".equals(eventType)) {
                log.debug("Skipping payment event type: {}", eventType);
                return;
            }

            String payloadStr = "";
            if (afterNode.has("payload")) {
                JsonNode innerPayload = afterNode.get("payload");
                payloadStr = innerPayload.isTextual() ? innerPayload.asText() : innerPayload.toString();
            } else {
                payloadStr = afterNode.toString();
            }

            PaymentCompletedEvent event = objectMapper.readValue(payloadStr, PaymentCompletedEvent.class);

            if (event.getInvoiceId() == null || event.getInvoiceId().trim().isEmpty()) {
                log.info("Ignoring PaymentCompleted event with no invoiceId (likely an order payment)");
                return;
            }

            log.info("Processing PaymentCompleted for invoiceId: {}", event.getInvoiceId());

            Long invoiceId;
            try {
                invoiceId = Long.parseLong(event.getInvoiceId());
            } catch (NumberFormatException e) {
                log.error("Failed to parse invoiceId: {} to Long", event.getInvoiceId());
                return;
            }

            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            if (invoiceOpt.isEmpty()) {
                log.warn("Invoice not found with ID {}", invoiceId);
                return;
            }

            Invoice invoice = invoiceOpt.get();
            if (invoice.getStatus() == InvoiceStatus.PAID) {
                log.info("Invoice {} is already PAID. Skipping.", invoiceId);
                return;
            }

            // 1. Update invoice status to PAID
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
            log.info("Invoice {} status successfully updated to PAID in DB", invoiceId);

            // 2. Prepare Outbox Event Payload
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("invoiceId", invoice.getId());
            payloadMap.put("customerId", invoice.getCustomerId());
            payloadMap.put("subscriptionId", invoice.getSubscriptionId());
            payloadMap.put("amount", invoice.getAmount());
            payloadMap.put("status", invoice.getStatus().name());
            payloadMap.put("paidAt", LocalDateTime.now().toString());

            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            // 3. Create Outbox record
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setId(UUID.randomUUID());
            outboxEvent.setAggregateType("invoice");
            outboxEvent.setAggregateId(String.valueOf(invoice.getId()));
            outboxEvent.setType("InvoicePaidEvent");
            outboxEvent.setPayload(payloadJson);
            outboxEvent.setStatus(OutboxStatus.PENDING);

            outboxEventRepository.save(outboxEvent);
            log.info("InvoicePaidEvent outbox event generated for invoiceId: {}", invoice.getId());

        } catch (Exception ex) {
            log.error("Failed to process PaymentCompleted Kafka message: {}", message, ex);
        }
    }
}
