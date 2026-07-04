package com.telcox.springmicroservices.cdrsimulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


import java.util.List;

/**
 * application.yml içindeki simulator.* değerlerini okuyan konfigürasyon sınıfı.
 * Lombok kullanılmadan saf Java getter/setter ile yazılmıştır.
 */
@ConfigurationProperties(prefix = "simulator")
public class SimulatorProperties {

    /** Simülatörü aktif/pasif yapar. false ise hiçbir event üretilmez. */
    private boolean enabled = true;

    /** Event'lerin basılacağı Kafka topic adı. */
    private String topic = "telco.usage.events";

    /** Saniyede kaç adet CdrRecorded event'i basılacağı. */
    private int eventsPerSecond = 100;

    /** Hangi MSISDN'ler adına CDR üretileceğinin listesi. */
    private List<String> targetMsisdns = List.of("5321112233");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getEventsPerSecond() {
        return eventsPerSecond;
    }

    public void setEventsPerSecond(int eventsPerSecond) {
        this.eventsPerSecond = eventsPerSecond;
    }

    public List<String> getTargetMsisdns() {
        return targetMsisdns;
    }

    public void setTargetMsisdns(List<String> targetMsisdns) {
        this.targetMsisdns = targetMsisdns;
    }
}
