package com.telcox.springmicroservices.notification.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.notification.kafka.event.*;
import com.telcox.springmicroservices.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    // ─── 1. SubscriptionActivated → WELCOME_SMS ──────────────────────────────
    @KafkaListener(
            topics = "telcox.Subscription.events",
            groupId = "notification-subscription-group"
    )
    public void onSubscriptionEvent(
            @Payload String payload,
            @Header(name = "eventType", required = false) String eventType
    ) {
        try {
            // Debezium outbox envelope'dan gerçek payload'ı al
            String actualPayload = extractPayload(payload);
            String resolvedEventType = eventType != null ? eventType : resolveEventTypeFromPayload(actualPayload, "SubscriptionActivated");
            log.info("[NOTIFICATION] Subscription event received. eventType: {}", resolvedEventType);

            if ("SubscriptionActivated".equals(resolvedEventType)) {
                SubscriptionActivatedEvent event = objectMapper.readValue(actualPayload, SubscriptionActivatedEvent.class);
                Map<String, String> params = new HashMap<>();
                params.put("customerName", event.getCustomerName() != null ? event.getCustomerName() : event.getMsisdn());
                params.put("msisdn", event.getMsisdn());
                notificationService.sendSms(event.getMsisdn(), "WELCOME_SMS", params);

                // Eğer email bilgisi varsa email de gönder
                if (event.getEmail() != null && !event.getEmail().isBlank()) {
                    notificationService.sendEmail(event.getEmail(), "WELCOME_EMAIL", params);
                }
            }
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to process Subscription event: {}", e.getMessage(), e);
        }
    }

    // ─── 2. InvoiceGenerated → INVOICE_EMAIL ─────────────────────────────────
    // ─── 3. InvoiceOverdue  → INVOICE_OVERDUE_SMS ────────────────────────────
    @KafkaListener(
            topics = "telcox.invoice.events",
            groupId = "notification-invoice-group"
    )
    public void onInvoiceEvent(
            @Payload String payload,
            @Header(name = "eventType", required = false) String eventType
    ) {
        try {
            String actualPayload = extractPayload(payload);
            String resolvedEventType = eventType != null ? eventType : resolveEventTypeFromPayload(actualPayload, null);
            log.info("[NOTIFICATION] Invoice event received. eventType: {}", resolvedEventType);

            if ("InvoiceGenerated".equals(resolvedEventType)) {
                InvoiceGeneratedEvent event = objectMapper.readValue(actualPayload, InvoiceGeneratedEvent.class);
                if (event.getEmail() != null && !event.getEmail().isBlank()) {
                    Map<String, String> params = new HashMap<>();
                    params.put("customerName", event.getCustomerName() != null ? event.getCustomerName() : "Abone");
                    params.put("invoiceMonth", event.getInvoiceMonth() != null ? event.getInvoiceMonth() : "");
                    params.put("amount", event.getAmount() != null ? event.getAmount().toPlainString() : "0");
                    params.put("pdfUrl", event.getPdfUrl() != null ? event.getPdfUrl() : "");
                    notificationService.sendEmail(event.getEmail(), "INVOICE_EMAIL", params);
                } else {
                    log.warn("[NOTIFICATION] InvoiceGenerated event has no email, skipping email notification for invoice: {}", event.getInvoiceId());
                }
            } else if ("InvoiceOverdue".equals(resolvedEventType)) {
                InvoiceOverdueEvent event = objectMapper.readValue(actualPayload, InvoiceOverdueEvent.class);
                Map<String, String> params = new HashMap<>();
                params.put("customerName", event.getCustomerName() != null ? event.getCustomerName() : "Abone");
                params.put("amount", event.getAmount() != null ? event.getAmount().toPlainString() : "0");
                params.put("dueDate", event.getDueDate() != null ? event.getDueDate() : "");
                String msisdn = event.getMsisdn() != null ? event.getMsisdn() : "unknown";
                notificationService.sendSms(msisdn, "INVOICE_OVERDUE_SMS", params);
            }
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to process Invoice event: {}", e.getMessage(), e);
        }
    }

    // ─── 4. QuotaThresholdReached → QUOTA_WARNING_SMS ────────────────────────
    // ─── 5. QuotaExceeded        → QUOTA_EXCEEDED_SMS ────────────────────────
    @KafkaListener(
            topics = {"telcox.Quota.events", "telcox.quota.events"},
            groupId = "notification-quota-group"
    )
    public void onQuotaEvent(
            @Payload String payload,
            @Header(name = "eventType", required = false) String eventType
    ) {
        try {
            String actualPayload = extractPayload(payload);
            String resolvedEventType = eventType != null ? eventType : resolveEventTypeFromPayload(actualPayload, null);
            log.info("[NOTIFICATION] Quota event received. eventType: {}", resolvedEventType);

            if ("QuotaThresholdReached".equals(resolvedEventType)) {
                QuotaThresholdReachedEvent event = objectMapper.readValue(actualPayload, QuotaThresholdReachedEvent.class);
                Map<String, String> params = new HashMap<>();
                params.put("customerName", event.getMsisdn());
                params.put("usageType", formatUsageType(event.getUsageType()));
                notificationService.sendSms(event.getMsisdn(), "QUOTA_WARNING_SMS", params);

            } else if ("QuotaExceeded".equals(resolvedEventType)) {
                QuotaExceededEvent event = objectMapper.readValue(actualPayload, QuotaExceededEvent.class);
                Map<String, String> params = new HashMap<>();
                params.put("customerName", event.getMsisdn());
                params.put("usageType", formatUsageType(event.getUsageType()));
                notificationService.sendSms(event.getMsisdn(), "QUOTA_EXCEEDED_SMS", params);
            }
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to process Quota event: {}", e.getMessage(), e);
        }
    }

    /**
     * Debezium outbox CDC ile gelen payload bir JSON envelope olabilir.
     * Gerçek payload'ı (after.payload) ya da raw string'i döndürür.
     */
    private String extractPayload(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            // Debezium envelope: {"after": {"payload": "..."}}
            if (root.has("after")) {
                JsonNode after = root.get("after");
                if (after.has("payload")) {
                    String innerPayload = after.get("payload").asText();
                    return innerPayload;
                }
                return after.toString();
            }
            // Zaten düz payload
            return raw;
        } catch (JsonProcessingException e) {
            // String değilse raw olarak dön
            return raw;
        }
    }

    private String resolveEventTypeFromPayload(String actualPayload, String defaultType) {
        try {
            JsonNode json = objectMapper.readTree(actualPayload);
            if (json.has("eventType")) {
                return json.get("eventType").asText();
            }
        } catch (Exception e) {
            // Ignore
        }
        return defaultType;
    }

    private String formatUsageType(String usageType) {
        if (usageType == null) return "kullanım";
        return switch (usageType.toUpperCase()) {
            case "VOICE" -> "Dakika";
            case "SMS"   -> "SMS";
            case "DATA"  -> "İnternet";
            default      -> usageType;
        };
    }
}
