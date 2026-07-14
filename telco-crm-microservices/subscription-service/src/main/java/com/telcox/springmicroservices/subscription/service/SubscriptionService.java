package com.telcox.springmicroservices.subscription.service;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telcox.springmicroservices.subscription.dto.CreateSubscriptionRequest;
import com.telcox.springmicroservices.subscription.dto.SubscriptionResponse;
import com.telcox.springmicroservices.subscription.entity.Subscription;
import com.telcox.springmicroservices.subscription.entity.SubscriptionStatus;
import com.telcox.springmicroservices.subscription.repository.SubscriptionRepository;
import com.telcox.springmicroservices.subscription.entity.OutboxEvent;
import com.telcox.springmicroservices.subscription.entity.OutboxStatus;
import com.telcox.springmicroservices.subscription.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.subscription.dto.SubscriptionActivatedEvent;
import com.telcox.springmicroservices.subscription.dto.SubscriptionActivationFailedEvent;
import com.telcox.springmicroservices.subscription.repository.SubscriptionAddonRepository;
import com.telcox.springmicroservices.subscription.entity.SubscriptionAddon;
import com.telcox.springmicroservices.subscription.dto.OrderConfirmedEvent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import java.time.Duration;
import com.telcox.springmicroservices.subscription.dto.TariffChangeRequestedEvent;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final MsisdnAllocationService msisdnAllocationService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final SubscriptionAddonRepository subscriptionAddonRepository;
    private final RedissonClient redissonClient;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
            MsisdnAllocationService msisdnAllocationService,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            SubscriptionAddonRepository subscriptionAddonRepository,
            RedissonClient redissonClient) {
        this.subscriptionRepository = subscriptionRepository;
        this.msisdnAllocationService = msisdnAllocationService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.subscriptionAddonRepository = subscriptionAddonRepository;
        this.redissonClient = redissonClient;
    }

    /**
     * Yeni abonelik oluşturur.
     * Redisson ile bir FREE MSISDN tahsis edildikten sonra
     * abonelik kaydı ACTIVE olarak veritabanına yazılır.
     */
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        log.info("[SUBSCRIPTION] Yeni abonelik oluşturuluyor. Müşteri: " + request.customerId()
                + ", Tarife: " + request.tariffCode());

        // =========================================================
        // 🚨 TEST TUZAĞI BAŞLANGICI 🚨
        // =========================================================
        if (request.customerId() != null && request.customerId() == 999L) {
            log.error("🚨 TEST TUZAĞI TETİKLENDİ! Müşteri 999 için aktivasyon bilerek patlatılıyor.");
            throw new RuntimeException("Simüle edilmiş altyapı hatası (Müşteri 999)");
        }
        // =========================================================
        // 🚨 TEST TUZAĞI BİTİŞİ 🚨
        // =========================================================

        // 1) Dağıtık kilit altında boş numara tahsis et
        String msisdn = msisdnAllocationService.allocateFreeMsisdn();

        // 2) Abonelik kaydını oluştur
        Subscription subscription = new Subscription();
        subscription.setCustomerId(request.customerId());
        subscription.setMsisdn(msisdn);
        subscription.setTariffCode(request.tariffCode());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setActivatedAt(Instant.now());

        subscriptionRepository.save(subscription);

        // 3) Numarayı ALLOCATED olarak onayla
        msisdnAllocationService.confirmAllocation(msisdn);

        log.info("[SUBSCRIPTION] Abonelik başarıyla oluşturuldu. ID: " + subscription.getId()
                + ", MSISDN: " + msisdn);

        // 4) Outbox tablosuna SubscriptionActivated ve MSISDNAllocated event'lerini yaz
        try {
            OutboxEvent subscriptionActivatedEvent = new OutboxEvent();
            subscriptionActivatedEvent.setEventId(UUID.randomUUID());
            subscriptionActivatedEvent.setAggregateType("Subscription");
            subscriptionActivatedEvent.setAggregateId(subscription.getId().toString());
            subscriptionActivatedEvent.setEventType("SubscriptionActivated");
            
            SubscriptionActivatedEvent activatedDto = new SubscriptionActivatedEvent(
                    request.orderId(),
                    subscription.getId(),
                    subscription.getMsisdn(),
                    subscription.getStatus().name()
            );
            subscriptionActivatedEvent.setPayload(objectMapper.writeValueAsString(activatedDto));
            subscriptionActivatedEvent.setStatus(OutboxStatus.PENDING);
            outboxEventRepository.save(subscriptionActivatedEvent);

            OutboxEvent msisdnAllocatedEvent = new OutboxEvent();
            msisdnAllocatedEvent.setEventId(UUID.randomUUID());
            msisdnAllocatedEvent.setAggregateType("Subscription");
            msisdnAllocatedEvent.setAggregateId(subscription.getId().toString());
            msisdnAllocatedEvent.setEventType("MSISDNAllocated");
            msisdnAllocatedEvent.setPayload(objectMapper.writeValueAsString(subscription));
            msisdnAllocatedEvent.setStatus(OutboxStatus.PENDING);
            outboxEventRepository.save(msisdnAllocatedEvent);

            log.info("[SUBSCRIPTION] Outbox event'leri (SubscriptionActivated, MSISDNAllocated) başarıyla yazıldı.");
        } catch (Exception e) {
            throw new RuntimeException("Outbox event'leri yazılamadı", e);
        }

        return toResponse(subscription);
    }

    /**
     * Abonelik detayını UUID ile getirir.
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Abonelik bulunamadı: " + subscriptionId));
        return toResponse(subscription);
    }

    /**
     * Tüm abonelikleri getirir.
     */
    @Transactional(readOnly = true)
    public java.util.List<SubscriptionResponse> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Aboneliği askıya alır (ödeme yapılmadığında).
     */
    @Transactional
    public SubscriptionResponse suspendSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Abonelik bulunamadı: " + subscriptionId));

        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        subscriptionRepository.save(subscription);
        log.info("[SUBSCRIPTION] Abonelik askıya alındı. ID: " + subscriptionId);
        return toResponse(subscription);
    }

    /**
     * Askıya alınan aboneliği yeniden aktive eder.
     */
    @Transactional
    public SubscriptionResponse reactivateSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Abonelik bulunamadı: " + subscriptionId));

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);
        log.info("[SUBSCRIPTION] Abonelik yeniden aktive edildi. ID: " + subscriptionId);
        return toResponse(subscription);
    }

    /**
     * Aboneliği kalıcı olarak sonlandırır ve MSISDN'i havuza geri döndürür.
     */
    @Transactional
    public SubscriptionResponse terminateSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Abonelik bulunamadı: " + subscriptionId));

        subscription.setStatus(SubscriptionStatus.TERMINATED);
        subscription.setTerminatedAt(Instant.now());
        subscriptionRepository.save(subscription);

        // MSISDN'i tekrar FREE durumuna al (havuza geri dön)
        msisdnAllocationService.releaseMsisdn(subscription.getMsisdn());

        log.info("[SUBSCRIPTION] Abonelik sonlandırıldı. ID: " + subscriptionId
                + ", MSISDN: " + subscription.getMsisdn() + " havuza döndürüldü.");
        return toResponse(subscription);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCustomerId(),
                subscription.getMsisdn(),
                subscription.getTariffCode(),
                subscription.getStatus(),
                subscription.getActivatedAt(),
                subscription.getTerminatedAt(),
                subscription.getCreatedAt());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishActivationFailedEvent(Long orderId, String reason) {
        try {
            OutboxEvent failedEvent = new OutboxEvent();
            failedEvent.setEventId(UUID.randomUUID());
            failedEvent.setAggregateType("Subscription");
            failedEvent.setAggregateId(orderId != null ? orderId.toString() : "UNKNOWN");
            failedEvent.setEventType("SubscriptionActivationFailed");

            SubscriptionActivationFailedEvent dto = new SubscriptionActivationFailedEvent(orderId, reason);
            failedEvent.setPayload(objectMapper.writeValueAsString(dto));
            failedEvent.setStatus(OutboxStatus.PENDING);

            outboxEventRepository.save(failedEvent);
            log.info("[SUBSCRIPTION] SubscriptionActivationFailed eventi Outbox'a başarıyla yazıldı. OrderId: {}", orderId);
        } catch (Exception e) {
            log.error("[SUBSCRIPTION] Failed to publish SubscriptionActivationFailed event for OrderId: {}", orderId, e);
        }
    }

    @Transactional
    public void addAddonToSubscription(OrderConfirmedEvent event) {
        log.info("[SUBSCRIPTION] Ek paket satın alımı işleniyor. OrderId: {}, SubscriptionId: {}, Addon: {}",
                event.getOrderId(), event.getSubscriptionId(), event.getTariffCode());

        if (event.getSubscriptionId() == null) {
            throw new IllegalArgumentException("Addon siparişi için subscriptionId boş olamaz.");
        }

        Subscription subscription = subscriptionRepository.findById(event.getSubscriptionId())
                .orElseThrow(() -> new IllegalArgumentException("Abonelik bulunamadı: " + event.getSubscriptionId()));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Abonelik aktif değil. Durum: " + subscription.getStatus());
        }

        SubscriptionAddon addon = SubscriptionAddon.builder()
                .subscription(subscription)
                .addonCode(event.getTariffCode()) // event'ten gelen kod
                .activatedAt(Instant.now())
                .build();
        subscriptionAddonRepository.save(addon);

        try {
            OutboxEvent addonActivatedEvent = new OutboxEvent();
            addonActivatedEvent.setEventId(UUID.randomUUID());
            addonActivatedEvent.setAggregateType("Subscription");
            addonActivatedEvent.setAggregateId(subscription.getId().toString());
            addonActivatedEvent.setEventType("AddonActivated");

            // We can reuse SubscriptionActivatedEvent or just write a simple map as payload.
            // Using a structured JSON for the outbox
            String payload = String.format("{\"orderId\":%d,\"subscriptionId\":\"%s\",\"addonCode\":\"%s\",\"msisdn\":\"%s\",\"status\":\"ACTIVATED\"}",
                    event.getOrderId(), subscription.getId(), event.getTariffCode(), subscription.getMsisdn());
            
            addonActivatedEvent.setPayload(payload);
            addonActivatedEvent.setStatus(OutboxStatus.PENDING);
            outboxEventRepository.save(addonActivatedEvent);

            log.info("[SUBSCRIPTION] AddonActivated outbox eventi başarıyla yazıldı. SubscriptionId: {}", subscription.getId());
        } catch (Exception e) {
            throw new RuntimeException("AddonActivated event'i outbox'a yazılamadı", e);
        }
    }

    @Transactional
    public void changeTariff(TariffChangeRequestedEvent event) {
        log.info("[SUBSCRIPTION] Tarife değişikliği işleniyor. OrderId: {}, SubscriptionId: {}", event.getOrderId(), event.getSubscriptionId());

        if (event.getSubscriptionId() == null || event.getOrderId() == null) {
            throw new IllegalArgumentException("Tarife değişikliği için subscriptionId ve orderId boş olamaz.");
        }

        String idempotencyKey = "idempotency:tariff_change:" + event.getSubscriptionId() + ":" + event.getOrderId();
        RBucket<Boolean> bucket = redissonClient.getBucket(idempotencyKey);
        
        boolean isFirstProcess = bucket.setIfAbsent(true, Duration.ofDays(7));
        if (!isFirstProcess) {
            log.info("[SUBSCRIPTION] Tarife değişikliği daha önce işlenmiş (Idempotent). İşlem atlanıyor. OrderId: {}", event.getOrderId());
            return;
        }

        Subscription subscription = subscriptionRepository.findById(event.getSubscriptionId())
                .orElseThrow(() -> new IllegalArgumentException("Abonelik bulunamadı: " + event.getSubscriptionId()));

        log.info("[SUBSCRIPTION] Abonelik bulundu. Mevcut tarife: {}, Yeni tarife: {}", subscription.getTariffCode(), event.getNewTariffCode());
        subscription.setTariffCode(event.getNewTariffCode());
        subscriptionRepository.save(subscription);

        log.info("[SUBSCRIPTION] Tarife başarıyla güncellendi. OrderId: {}", event.getOrderId());
    }
}
