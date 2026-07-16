package com.telcox.springmicroservices.subscription.controller;

import java.util.UUID;

import com.telcox.springmicroservices.subscription.dto.CreateSubscriptionRequest;
import com.telcox.springmicroservices.subscription.dto.SubscriptionResponse;
import com.telcox.springmicroservices.subscription.service.AuthenticatedCustomerResolver;
import com.telcox.springmicroservices.subscription.service.SubscriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Abonelik yönetimi REST API'si.
 * Yeni hat açma, askıya alma, yeniden aktive etme ve sonlandırma işlemlerini sağlar.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Abonelik yönetimi operasyonları")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AuthenticatedCustomerResolver authenticatedCustomerResolver;

    public SubscriptionController(SubscriptionService subscriptionService,
            AuthenticatedCustomerResolver authenticatedCustomerResolver) {
        this.subscriptionService = subscriptionService;
        this.authenticatedCustomerResolver = authenticatedCustomerResolver;
    }

    @PostMapping
    @Operation(summary = "Yeni abonelik oluştur",
               description = "Havuzdan boş numara tahsis ederek yeni abonelik başlatır.")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @RequestHeader("X-User-Id") String keycloakUserId,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        UUID customerId = authenticatedCustomerResolver.resolve(keycloakUserId);
        CreateSubscriptionRequest authenticatedRequest = new CreateSubscriptionRequest(
                customerId, request.tariffId(), request.tariffCode(), request.orderId());
        SubscriptionResponse response = subscriptionService.createSubscription(authenticatedRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Abonelik detayı getir")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.getSubscription(id));
    }

    @GetMapping
    @Operation(summary = "Tüm abonelikleri getir", description = "Veritabanındaki tüm abonelik kayıtlarını listeler.")
    public ResponseEntity<java.util.List<SubscriptionResponse>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Aboneliği askıya al",
               description = "Ödeme yapılmadığında abonelik SUSPENDED statüsüne alınır.")
    public ResponseEntity<SubscriptionResponse> suspendSubscription(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.suspendSubscription(id));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Aboneliği yeniden aktive et")
    public ResponseEntity<SubscriptionResponse> reactivateSubscription(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.reactivateSubscription(id));
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "Aboneliği sonlandır",
               description = "Abonelik TERMINATED statüsüne alınır ve MSISDN havuza geri döner.")
    public ResponseEntity<SubscriptionResponse> terminateSubscription(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.terminateSubscription(id));
    }
}
