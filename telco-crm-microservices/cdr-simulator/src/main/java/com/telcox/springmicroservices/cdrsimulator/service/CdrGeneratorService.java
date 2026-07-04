package com.telcox.springmicroservices.cdrsimulator.service;

import com.telcox.springmicroservices.cdrsimulator.config.SimulatorProperties;
import com.telcox.springmicroservices.cdrsimulator.dto.CdrRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CDR Simülatör motoru.
 * Her saniyede bir uyanarak konfigürasyondaki sayı (events-per-second) kadar
 * CdrRecordedEvent üretir ve Kafka'ya basır.
 *
 * Kafka Mesaj Anahtarı (Key): MSISDN
 * Bu sayede aynı numaraya ait tüm kullanım kayıtları aynı Kafka partition'ına düşer.
 * Böylece usage-service'te sıralı (ordered) işleme garantisi sağlanır.
 */
@Service
public class CdrGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(CdrGeneratorService.class);

    private static final String[] CDR_TYPES = {"VOICE", "DATA", "SMS"};
    private static final Random RANDOM = new Random();
    private static final AtomicLong totalSent = new AtomicLong(0);

    private final SimulatorProperties properties;
    private final KafkaTemplate<String, CdrRecordedEvent> kafkaTemplate;

    public CdrGeneratorService(SimulatorProperties properties,
                                KafkaTemplate<String, CdrRecordedEvent> kafkaTemplate) {
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Her saniyede bir tetiklenir.
     * simulator.enabled=false ise hiçbir şey yapmaz.
     */
    @Scheduled(fixedRate = 1000)
    public void generateAndSend() {
        if (!properties.isEnabled()) {
            return;
        }

        List<String> msisdns = properties.getTargetMsisdns();
        if (msisdns == null || msisdns.isEmpty()) {
            log.warn("Hedef MSISDN listesi boş! application.yml'deki simulator.target-msisdns alanını kontrol edin.");
            return;
        }

        int count = properties.getEventsPerSecond();
        for (int i = 0; i < count; i++) {
            // Her event için rastgele bir MSISDN seç
            String msisdn = msisdns.get(RANDOM.nextInt(msisdns.size()));
            CdrRecordedEvent event = buildEvent(msisdn);

            // MSISDN'i key olarak kullanarak Kafka'ya gönder
            kafkaTemplate.send(properties.getTopic(), msisdn, event);
        }

        long total = totalSent.addAndGet(count);
        log.info("CDR Simülatör | Bu saniye gönderilen: {} | Toplam gönderilen: {} | Topic: {}",
                count, total, properties.getTopic());
    }

    /**
     * Rastgele gerçekçi bir CdrRecordedEvent oluşturur.
     */
    private CdrRecordedEvent buildEvent(String msisdn) {
        String type = CDR_TYPES[RANDOM.nextInt(CDR_TYPES.length)];
        double amount = switch (type) {
            case "VOICE" -> 1 + RANDOM.nextInt(120);      // 1 - 120 dakika
            case "DATA"  -> 1 + RANDOM.nextInt(10240);    // 1 - 10240 MB
            case "SMS"   -> 1.0;                           // Her SMS 1 birim
            default      -> 1.0;
        };

        CdrRecordedEvent event = new CdrRecordedEvent();
        event.setSubscriptionId(UUID.randomUUID());
        event.setMsisdn(msisdn);
        event.setType(type);
        event.setAmount(amount);
        event.setCdrRef("CDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        event.setRecordedAt(Instant.now());
        return event;
    }
}
