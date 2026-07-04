package com.telcox.springmicroservices.cdrsimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.telcox.springmicroservices.cdrsimulator.config.SimulatorProperties;

/**
 * CDR Simülatör uygulaması.
 * Başladığı anda Kafka'nın telco.usage.events topic'ine
 * saniyede (events-per-second) adet CdrRecorded eventi basmaya başlar.
 *
 * Durdurmak için CTRL+C yeterlidir.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SimulatorProperties.class)
public class CdrSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CdrSimulatorApplication.class, args);
    }
}
