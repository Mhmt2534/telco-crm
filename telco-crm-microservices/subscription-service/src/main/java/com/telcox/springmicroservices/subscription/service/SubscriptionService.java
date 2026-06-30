package com.telcox.springmicroservices.subscription.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.logging.Logger;

import com.telcox.springmicroservices.subscription.dto.CreateSubscriptionRequest;
import com.telcox.springmicroservices.subscription.dto.SubscriptionResponse;
import com.telcox.springmicroservices.subscription.entity.Subscription;
import com.telcox.springmicroservices.subscription.entity.SubscriptionStatus;
import com.telcox.springmicroservices.subscription.repository.SubscriptionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private static final Logger log = Logger.getLogger(SubscriptionService.class.getName());

    private final SubscriptionRepository subscriptionRepository;
    private final MsisdnAllocationService msisdnAllocationService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               MsisdnAllocationService msisdnAllocationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.msisdnAllocationService = msisdnAllocationService;
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

        // 1) Dağıtık kilit altında boş numara tahsis et
        String msisdn = msisdnAllocationService.allocateFreeMsisdn();

        // 2) Abonelik kaydını oluştur
        Subscription subscription = new Subscription();
        subscription.setCustomerId(request.customerId());
        subscription.setMsisdn(msisdn);
        subscription.setTariffCode(request.tariffCode());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setActivatedAt(OffsetDateTime.now());

        subscriptionRepository.save(subscription);

        // 3) Numarayı ALLOCATED olarak onayla
        msisdnAllocationService.confirmAllocation(msisdn);

        log.info("[SUBSCRIPTION] Abonelik başarıyla oluşturuldu. ID: " + subscription.getId()
                + ", MSISDN: " + msisdn);

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
        subscription.setTerminatedAt(OffsetDateTime.now());
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
                subscription.getCreatedAt()
        );
    }
}
