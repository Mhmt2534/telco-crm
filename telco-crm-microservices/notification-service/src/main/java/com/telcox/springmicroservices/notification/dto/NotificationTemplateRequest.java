package com.telcox.springmicroservices.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateRequest {
    private String code;
    private String channel;
    private String locale;
    private String subject;
    private String bodyTemplate;
}
