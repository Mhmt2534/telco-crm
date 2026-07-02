package com.telcox.springmicroservices.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDto {

    @NotBlank(message = "Açık adres zorunludur")
    private String line1;

    @NotBlank(message = "Şehir zorunludur")
    private String city;

    @NotBlank(message = "İlçe zorunludur")
    private String district;

    private String postalCode;
    
    private boolean isDefault;
}
