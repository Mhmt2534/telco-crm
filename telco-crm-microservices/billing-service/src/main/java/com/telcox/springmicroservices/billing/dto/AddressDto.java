package com.telcox.springmicroservices.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {
    private String line1;
    private String city;
    private String district;
    private String postalCode;
    private boolean isDefault;
}
