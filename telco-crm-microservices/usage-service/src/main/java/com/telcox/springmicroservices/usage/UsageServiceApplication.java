package com.telcox.springmicroservices.usage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients
public class UsageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsageServiceApplication.class, args);
    }
}
