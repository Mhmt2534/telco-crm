package com.telcox.springmicroservices.customer.domain.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerKYCApprovedEvent {
    private Long customerId;
    private String phone;
    private String keycloakUserId;
    private String timestamp;
}
