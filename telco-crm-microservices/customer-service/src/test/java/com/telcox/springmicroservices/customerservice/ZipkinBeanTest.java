package com.telcox.springmicroservices.customerservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import com.telcox.springmicroservices.customer.CustomerServiceApplication;

import zipkin2.reporter.Sender;
import zipkin2.reporter.Reporter;

@SpringBootTest(classes = CustomerServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ZipkinBeanTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void checkZipkinBeans() {
        System.out.println("=========================================");
        System.out.println("Checking Sender bean:");
        try {
            Sender sender = context.getBean(Sender.class);
            System.out.println("Sender: " + sender.getClass().getName());
        } catch (Exception e) {
            System.out.println("No Sender bean found: " + e.getMessage());
        }

        System.out.println("Checking Reporter bean:");
        try {
            Reporter reporter = context.getBean(Reporter.class);
            System.out.println("Reporter: " + reporter.getClass().getName());
        } catch (Exception e) {
            System.out.println("No Reporter bean found: " + e.getMessage());
        }
        System.out.println("=========================================");
    }
}
