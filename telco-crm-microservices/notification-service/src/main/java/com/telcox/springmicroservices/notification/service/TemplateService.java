package com.telcox.springmicroservices.notification.service;

import com.telcox.springmicroservices.notification.dto.NotificationTemplateRequest;
import com.telcox.springmicroservices.notification.dto.NotificationTemplateResponse;
import com.telcox.springmicroservices.notification.entity.NotificationTemplate;
import com.telcox.springmicroservices.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final NotificationTemplateRepository repository;

    @Transactional
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
        log.info("Creating new notification template with code: {}", request.getCode());
        
        if (repository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Template code already exists: " + request.getCode());
        }

        NotificationTemplate template = NotificationTemplate.builder()
                .code(request.getCode())
                .channel(request.getChannel())
                .locale(request.getLocale())
                .subject(request.getSubject())
                .bodyTemplate(request.getBodyTemplate())
                .build();

        NotificationTemplate saved = repository.save(template);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getAllTemplates() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplateByCode(String code) {
        NotificationTemplate template = repository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Template not found for code: " + code));
        return mapToResponse(template);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        log.info("Deleting template with ID: {}", id);
        repository.deleteById(id);
    }

    /**
     * Replaces placeholders like {customerName} with actual values from the map.
     */
    public String parseTemplate(String templateBody, Map<String, String> parameters) {
        if (templateBody == null || parameters == null) {
            return templateBody;
        }
        
        String parsedBody = templateBody;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            parsedBody = parsedBody.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }
        return parsedBody;
    }

    private NotificationTemplateResponse mapToResponse(NotificationTemplate entity) {
        return NotificationTemplateResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .channel(entity.getChannel())
                .locale(entity.getLocale())
                .subject(entity.getSubject())
                .bodyTemplate(entity.getBodyTemplate())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
