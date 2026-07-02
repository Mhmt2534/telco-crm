package com.telcox.springmicroservices.customer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CustomerUpdateRequest {

    @NotBlank(message = "Ad zorunludur")
    private String firstName;

    @NotBlank(message = "Soyad zorunludur")
    private String lastName;

    @Email(message = "Geçersiz e-posta adresi")
    private String email;

    @Valid
    private List<AddressDto> addresses;

    // These fields are included only to catch if the client tries to update them
    private String identityNumber;
    private String phone;
}
