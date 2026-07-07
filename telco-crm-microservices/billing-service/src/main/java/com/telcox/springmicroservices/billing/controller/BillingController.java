package com.telcox.springmicroservices.billing.controller;

import com.telcox.springmicroservices.billing.scheduler.BillRunScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillRunScheduler billRunScheduler;

    public BillingController(BillRunScheduler billRunScheduler) {
        this.billRunScheduler = billRunScheduler;
    }

    @PostMapping("/runs")
    public ResponseEntity<String> triggerBillRun() {
        billRunScheduler.runDailyBilling();
        return ResponseEntity.ok("Bill run triggered successfully");
    }
}
