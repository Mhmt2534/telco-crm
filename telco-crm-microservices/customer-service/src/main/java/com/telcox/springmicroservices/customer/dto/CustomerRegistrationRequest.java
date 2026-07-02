package com.telcox.springmicroservices.customer.dto;

import com.telcox.springmicroservices.customer.domain.enums.CustomerType;
import com.telcox.springmicroservices.customer.validation.ValidTckn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CustomerRegistrationRequest {

    @NotNull(message = "Müşteri tipi zorunludur")
    private CustomerType type;

    @NotBlank(message = "Ad zorunludur")
    private String firstName;

    @NotBlank(message = "Soyad zorunludur")
    private String lastName;

    @ValidTckn
    private String identityNumber;

    @NotNull(message = "Doğum tarihi zorunludur")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Telefon numarası zorunludur")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Geçersiz telefon numarası formatı")
    private String phone;

    @Email(message = "Geçersiz e-posta adresi")
    private String email;

    @Valid
    private List<AddressDto> addresses;
}
