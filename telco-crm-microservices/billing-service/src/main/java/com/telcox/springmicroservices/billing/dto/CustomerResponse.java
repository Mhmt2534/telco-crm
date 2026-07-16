package com.telcox.springmicroservices.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private UUID id;
    private String type;
    private String firstName;
    private String lastName;
    private String identityNumber;
    private String maskedIdentityNumber;
    private LocalDate dateOfBirth;
    private String phone;
    private String email;
    private String status;
    private List<AddressDto> addresses;
}
