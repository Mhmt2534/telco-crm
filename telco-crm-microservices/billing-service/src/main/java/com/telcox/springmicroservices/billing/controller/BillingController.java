package com.telcox.springmicroservices.billing.controller;

import com.telcox.springmicroservices.billing.scheduler.BillRunScheduler;
import com.telcox.springmicroservices.billing.scheduler.InvoiceOverdueScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillRunScheduler billRunScheduler;
    private final InvoiceOverdueScheduler invoiceOverdueScheduler;

    public BillingController(BillRunScheduler billRunScheduler,
                             InvoiceOverdueScheduler invoiceOverdueScheduler) {
        this.billRunScheduler = billRunScheduler;
        this.invoiceOverdueScheduler = invoiceOverdueScheduler;
    }

    @PostMapping("/runs")
    public ResponseEntity<String> triggerBillRun() {
        billRunScheduler.runDailyBilling();
        return ResponseEntity.ok("Bill run triggered successfully");
    }

    @PostMapping("/overdue-check")
    public ResponseEntity<String> triggerOverdueCheck() {
        invoiceOverdueScheduler.runOverdueCheck();
        return ResponseEntity.ok("Overdue check triggered successfully");
    }
}
