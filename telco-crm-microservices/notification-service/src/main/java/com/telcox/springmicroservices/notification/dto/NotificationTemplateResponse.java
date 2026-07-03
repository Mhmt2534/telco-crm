package com.telcox.springmicroservices.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateResponse {
    private UUID id;
    private String code;
    private String channel;
    private String locale;
    private String subject;
    private String bodyTemplate;
    private OffsetDateTime createdAt;
}
