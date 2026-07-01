package com.telcox.springmicroservices.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * identity-service uygulama giriş noktası.
 * <p>
 * @EnableFeignClients: customer-service ile iletişim için Feign client stub'larını aktif eder.
 * @EnableDiscoveryClient: Eureka service registry'e kayıt için.
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.telcox.springmicroservices.identity.client")
@SpringBootApplication
public class IdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
