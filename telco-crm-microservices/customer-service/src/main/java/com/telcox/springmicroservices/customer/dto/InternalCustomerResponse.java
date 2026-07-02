package com.telcox.springmicroservices.customer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InternalCustomerResponse {
    private String phone;
    private String keycloakUserId;
    private String internalKeycloakPassword;
}
