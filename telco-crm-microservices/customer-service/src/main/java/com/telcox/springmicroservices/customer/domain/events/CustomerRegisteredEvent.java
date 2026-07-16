package com.telcox.springmicroservices.customer.domain.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerRegisteredEvent {
    private UUID customerId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String type;
    private String status;
    private String occurredAt;
}
