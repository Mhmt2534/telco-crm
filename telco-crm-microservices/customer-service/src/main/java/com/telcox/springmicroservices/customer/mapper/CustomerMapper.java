package com.telcox.springmicroservices.customer.mapper;

import com.telcox.springmicroservices.customer.domain.Address;
import com.telcox.springmicroservices.customer.domain.Customer;
import com.telcox.springmicroservices.customer.dto.AddressDto;
import com.telcox.springmicroservices.customer.dto.CustomerRegistrationRequest;
import com.telcox.springmicroservices.customer.dto.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "internalKeycloakPassword", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    Customer toEntity(CustomerRegistrationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    Address toAddressEntity(AddressDto dto);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "maskedIdentityNumber", source = "identityNumber", qualifiedByName = "maskIdentityNumber")
    CustomerResponse toResponse(Customer customer);

    AddressDto toAddressDto(Address address);

    @Named("maskIdentityNumber")
    default String maskIdentityNumber(String identityNumber) {
        if (identityNumber == null || identityNumber.length() < 11) {
            return null;
        }
        return identityNumber.substring(0, 2) + "*******" + identityNumber.substring(9);
    }
}
