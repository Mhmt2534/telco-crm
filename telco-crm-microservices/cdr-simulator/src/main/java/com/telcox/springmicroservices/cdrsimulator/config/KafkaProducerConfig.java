package com.telcox.springmicroservices.cdrsimulator.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telcox.springmicroservices.cdrsimulator.dto.CdrRecordedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer konfigürasyonu.
 * Mesaj anahtarı (key): String - MSISDN (partition dağılımının doğru çalışması için)
 * Mesaj değeri (value): CdrRecordedEvent - Jackson ile JSON byte[] olarak serialize edilir.
 * Deprecated Spring Kafka JsonSerializer KULLANILMAZ; saf Kafka Serializer<T> arayüzü implement edilir.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * OffsetDateTime ve diğer Java 8 tarih tiplerini ISO-8601 formatında yazan ObjectMapper.
     */
    @Bean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Saf Kafka Serializer<CdrRecordedEvent>: Deprecated Spring wrapper kullanmaz.
     */
    @Bean
    public Serializer<CdrRecordedEvent> cdrEventSerializer(ObjectMapper kafkaObjectMapper) {
        return (topic, data) -> {
            if (data == null) {
                return new byte[0];
            }
            try {
                return kafkaObjectMapper.writeValueAsBytes(data);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("CdrRecordedEvent serialize hatası", e);
            }
        };
    }

    @Bean
    public ProducerFactory<String, CdrRecordedEvent> cdrProducerFactory(
            Serializer<CdrRecordedEvent> cdrEventSerializer) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Key: String (MSISDN)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Güvenilirlik: Tüm broker'lar onaylasın
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        // Performans: Toplu gönderim için linger süresi (ms)
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        // Toplu gönderim boyutu (16KB)
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        DefaultKafkaProducerFactory<String, CdrRecordedEvent> factory =
                new DefaultKafkaProducerFactory<>(config, new StringSerializer(), cdrEventSerializer);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, CdrRecordedEvent> cdrKafkaTemplate(
            ProducerFactory<String, CdrRecordedEvent> cdrProducerFactory) {
        return new KafkaTemplate<>(cdrProducerFactory);
    }
}
