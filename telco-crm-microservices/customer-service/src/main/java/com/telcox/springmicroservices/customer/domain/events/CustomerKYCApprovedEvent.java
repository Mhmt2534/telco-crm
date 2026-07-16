package com.telcox.springmicroservices.customer.domain.events;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CustomerKYCApprovedEvent {
    private UUID customerId;
    private String phone;
    private String keycloakUserId;
    private String timestamp;
}
