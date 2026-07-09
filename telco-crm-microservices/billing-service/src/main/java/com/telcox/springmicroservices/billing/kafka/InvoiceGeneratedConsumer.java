package com.telcox.springmicroservices.billing.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.billing.service.InvoicePdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceGeneratedConsumer {

    private final InvoicePdfService invoicePdfService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "telcox.invoice.events", groupId = "billing-pdf-group")
    public void consumeInvoiceEvent(String messagePayload, 
                                     @Header(value = "eventType", required = false) String eventType) {
        log.info("Received invoice event: type={}, payload={}", eventType, messagePayload);

        if ("InvoiceGeneratedEvent".equals(eventType)) {
            try {
                JsonNode json = objectMapper.readTree(messagePayload);
                if (json.isTextual()) {
                    json = objectMapper.readTree(json.asText());
                }
                Long invoiceId = json.get("invoiceId").asLong();
                log.info("Triggering PDF generation for invoice: {}", invoiceId);
                invoicePdfService.generateAndUploadInvoicePdf(invoiceId);
            } catch (Exception e) {
                log.error("Error processing InvoiceGeneratedEvent for payload: {}", messagePayload, e);
            }
        } else {
            log.debug("Skipping event type: {}", eventType);
        }
    }
}
