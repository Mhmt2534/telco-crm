package com.telcox.springmicroservices.customer.dto;

import com.telcox.springmicroservices.customer.domain.enums.CustomerStatus;
import com.telcox.springmicroservices.customer.domain.enums.CustomerType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CustomerResponse {
    private Long id;
    private CustomerType type;
    private String firstName;
    private String lastName;
    private String identityNumber;
    private String maskedIdentityNumber;
    private LocalDate dateOfBirth;
    private String phone;
    private String email;
    private CustomerStatus status;
    private List<AddressDto> addresses;
}
