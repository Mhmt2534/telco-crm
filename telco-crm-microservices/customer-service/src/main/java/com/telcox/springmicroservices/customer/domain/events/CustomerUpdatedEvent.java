package com.telcox.springmicroservices.customer.domain.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerUpdatedEvent {
    private Long customerId;
    private String keycloakUserId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String status;
    private String changeReason;
    private String occurredAt;
}
