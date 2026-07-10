package com.telcox.springmicroservices.notification.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceOverdueEvent {
    private UUID invoiceId;
    private UUID customerId;
    private String customerName;
    private String msisdn;
    private String email;
    private BigDecimal amount;
    private String dueDate;
}
