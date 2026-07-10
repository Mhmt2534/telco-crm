package com.telcox.springmicroservices.notification.service;

import com.telcox.springmicroservices.notification.entity.NotificationHistory;
import com.telcox.springmicroservices.notification.entity.NotificationTemplate;
import com.telcox.springmicroservices.notification.repository.NotificationHistoryRepository;
import com.telcox.springmicroservices.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationHistoryRepository historyRepository;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    /**
     * Sends an EMAIL notification using the given template code.
     * Falls back to Mock log if template is not found.
     */
    public void sendEmail(String toEmail, String templateCode, Map<String, String> params) {
        NotificationTemplate template = templateRepository.findByCode(templateCode)
                .orElse(null);

        if (template == null) {
            log.warn("[EMAIL - MOCK] Template '{}' not found. Skipping email to: {}", templateCode, toEmail);
            return;
        }

        String body = templateService.parseTemplate(template.getBodyTemplate(), params);
        String subject = template.getSubject() != null
                ? templateService.parseTemplate(template.getSubject(), params)
                : "TelcoX Bildirimi";

        String status = "SENT";
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // HTML
            helper.setFrom("noreply@telcox.com");
            mailSender.send(message);
            log.info("[EMAIL] Sent '{}' to: {}", templateCode, toEmail);
        } catch (MessagingException e) {
            status = "FAILED";
            log.error("[EMAIL] Failed to send '{}' to: {}. Error: {}", templateCode, toEmail, e.getMessage());
        }

        String payloadJson = "{}";
        try {
            payloadJson = objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("Failed to serialize parameters to JSON for WELCOME_EMAIL: {}", e.getMessage());
        }

        saveHistory(null, templateCode, "EMAIL", payloadJson, status);
    }

    /**
     * Sends a MOCK SMS notification (logs to console, saves to history).
     */
    public void sendSms(String msisdn, String templateCode, Map<String, String> params) {
        NotificationTemplate template = templateRepository.findByCode(templateCode)
                .orElse(null);

        String body;
        if (template != null) {
            body = templateService.parseTemplate(template.getBodyTemplate(), params);
        } else {
            log.warn("[SMS - MOCK] Template '{}' not found. Using raw params.", templateCode);
            body = params.toString();
        }

        // Mock SMS: log to console (in real systems this calls an SMS gateway API)
        log.info("[SMS] >>> TO: {} | TEMPLATE: {} | MESSAGE: {}", msisdn, templateCode, body);

        String payloadJson = "{}";
        try {
            payloadJson = objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("Failed to serialize parameters to JSON for SMS: {}", e.getMessage());
        }

        saveHistory(null, templateCode, "SMS", payloadJson, "SENT");
    }

    private void saveHistory(UUID userId, String templateCode, String channel, String payloadJson, String status) {
        try {
            NotificationHistory history = NotificationHistory.builder()
                    .userId(userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000000"))
                    .templateCode(templateCode)
                    .channel(channel)
                    .payloadJson(payloadJson)
                    .status(status)
                    .sentAt(OffsetDateTime.now())
                    .build();
            historyRepository.save(history);
        } catch (Exception e) {
            log.warn("[NOTIFICATION] Failed to save notification history: {}", e.getMessage());
        }
    }
}
