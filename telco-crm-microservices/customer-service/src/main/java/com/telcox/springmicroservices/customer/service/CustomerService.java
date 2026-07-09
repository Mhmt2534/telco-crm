package com.telcox.springmicroservices.customer.service;

import com.telcox.common.core.exception.DuplicateResourceException;
import com.telcox.springmicroservices.customer.domain.Address;
import com.telcox.springmicroservices.customer.domain.Customer;
import com.telcox.springmicroservices.customer.dto.CustomerRegistrationRequest;
import com.telcox.springmicroservices.customer.dto.CustomerResponse;
import com.telcox.springmicroservices.customer.mapper.CustomerMapper;
import com.telcox.springmicroservices.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final KeycloakUserService keycloakUserService;
    private final com.telcox.springmicroservices.customer.repository.OutboxEventRepository outboxEventRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public void approveKyc(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new com.telcox.common.core.exception.ResourceNotFoundException("Müşteri bulunamadı: " + id));

        if (customer.getStatus() != com.telcox.springmicroservices.customer.domain.enums.CustomerStatus.PENDING) {
            throw new com.telcox.common.core.exception.BusinessRuleException("Yalnızca PENDING statüsündeki müşteriler onaylanabilir.");
        }

        // Generate random internal password
        String internalPassword = java.util.UUID.randomUUID().toString();
        
        // Create user in Keycloak
        String keycloakUserId = keycloakUserService.createCustomerUser(
            customer.getPhone(), 
            internalPassword,
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail()
        );

        // Update Customer Entity
        customer.setStatus(com.telcox.springmicroservices.customer.domain.enums.CustomerStatus.ACTIVE);
        customer.setKeycloakUserId(keycloakUserId);
        customer.setInternalKeycloakPassword(internalPassword);

        customerRepository.save(customer);

        // Create Outbox Event
        com.telcox.springmicroservices.customer.domain.events.CustomerKYCApprovedEvent eventPayload = com.telcox.springmicroservices.customer.domain.events.CustomerKYCApprovedEvent.builder()
                .customerId(customer.getId())
                .phone(customer.getPhone())
                .keycloakUserId(keycloakUserId)
                .timestamp(java.time.Instant.now().toString())
                .build();

        try {
            com.telcox.springmicroservices.customer.domain.OutboxEvent outboxEvent = new com.telcox.springmicroservices.customer.domain.OutboxEvent();
            outboxEvent.setAggregateType("Customer");
            outboxEvent.setAggregateId(String.valueOf(customer.getId()));
            outboxEvent.setEventType("CustomerKYCApproved");
            outboxEvent.setPayload(objectMapper.writeValueAsString(eventPayload));

            outboxEventRepository.save(outboxEvent);
            log.info("KYC onaylandı ve Keycloak kullanıcısı oluşturuldu. Müşteri ID: {}", customer.getId());
            outboxEventPublisher.publishCustomerUpdated(customer, "KYC_APPROVED");
        } catch (Exception e) {
            throw new RuntimeException("Event serialize edilemedi", e);
        }
    }

    @Transactional
    public CustomerResponse registerCustomer(CustomerRegistrationRequest request) {
        log.info("Müşteri kaydı başlatıldı. Kimlik Numarası (maskeli): {}", customerMapper.maskIdentityNumber(request.getIdentityNumber()));

        if (customerRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            throw new DuplicateResourceException("Bu T.C. Kimlik Numarası ile zaten bir kayıt mevcut");
        }

        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Bu telefon numarası ile zaten bir kayıt mevcut");
        }

        Customer customer = customerMapper.toEntity(request);
        
        if (request.getAddresses() != null) {
            request.getAddresses().forEach(addressDto -> {
                Address address = customerMapper.toAddressEntity(addressDto);
                customer.addAddress(address);
            });
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Müşteri başarıyla kaydedildi. Müşteri ID: {}", savedCustomer.getId());
        outboxEventPublisher.publishCustomerRegistered(savedCustomer);

        return customerMapper.toResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Müşteri bulunamadı: " + id));
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, com.telcox.springmicroservices.customer.dto.CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Müşteri bulunamadı: " + id));

        if (request.getIdentityNumber() != null || request.getPhone() != null) {
            throw new com.telcox.common.core.exception.BusinessRuleException("TCKN ve telefon numarası güncellenemez");
        }

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());

        if (request.getAddresses() != null) {
            customer.getAddresses().clear();
            request.getAddresses().forEach(addressDto -> {
                Address address = customerMapper.toAddressEntity(addressDto);
                customer.addAddress(address);
            });
        }

        Customer updatedCustomer = customerRepository.save(customer);
        outboxEventPublisher.publishCustomerUpdated(updatedCustomer, "PROFILE_UPDATE");
        return customerMapper.toResponse(updatedCustomer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Müşteri bulunamadı: " + id));
        customer.setDeleted(true);
        customerRepository.save(customer);
        outboxEventPublisher.publishCustomerUpdated(customer, "CUSTOMER_DELETED");
    }

    @Transactional(readOnly = true)
    public com.telcox.springmicroservices.customer.dto.InternalCustomerResponse getInternalCustomerByPhone(String phone) {
        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Müşteri bulunamadı: " + phone));

        if (customer.getStatus() != com.telcox.springmicroservices.customer.domain.enums.CustomerStatus.ACTIVE) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Aktif müşteri bulunamadı: " + phone);
        }

        return com.telcox.springmicroservices.customer.dto.InternalCustomerResponse.builder()
                .phone(customer.getPhone())
                .keycloakUserId(customer.getKeycloakUserId())
                .internalKeycloakPassword(customer.getInternalKeycloakPassword())
                .kycApproved(true)
                .build();
    }
}
