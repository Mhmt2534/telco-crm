package com.telcox.springmicroservices.notification.controller;

import com.telcox.springmicroservices.notification.dto.NotificationTemplateRequest;
import com.telcox.springmicroservices.notification.dto.NotificationTemplateResponse;
import com.telcox.springmicroservices.notification.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<NotificationTemplateResponse> createTemplate(@RequestBody NotificationTemplateRequest request) {
        return new ResponseEntity<>(templateService.createTemplate(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<NotificationTemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{code}")
    public ResponseEntity<NotificationTemplateResponse> getTemplateByCode(@PathVariable String code) {
        return ResponseEntity.ok(templateService.getTemplateByCode(code));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
