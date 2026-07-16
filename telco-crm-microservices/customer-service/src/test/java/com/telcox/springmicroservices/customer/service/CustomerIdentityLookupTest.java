package com.telcox.springmicroservices.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.customer.domain.Customer;
import com.telcox.springmicroservices.customer.domain.enums.CustomerStatus;
import com.telcox.springmicroservices.customer.mapper.CustomerMapper;
import com.telcox.springmicroservices.customer.repository.CustomerRepository;
import com.telcox.springmicroservices.customer.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerIdentityLookupTest {

    @Mock CustomerRepository customerRepository;
    @Mock CustomerMapper customerMapper;
    @Mock KeycloakUserService keycloakUserService;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock ObjectMapper objectMapper;
    @Mock OutboxEventPublisher outboxEventPublisher;
    @InjectMocks CustomerService customerService;

    @Test
    void mapsKeycloakSubjectToActiveCustomersPublicId() {
        UUID publicId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .publicId(publicId)
                .keycloakUserId("keycloak-subject")
                .status(CustomerStatus.ACTIVE)
                .build();
        when(customerRepository.findByKeycloakUserId("keycloak-subject"))
                .thenReturn(Optional.of(customer));

        assertThat(customerService.getCustomerIdentityByKeycloakUserId("keycloak-subject").customerId())
                .isEqualTo(publicId);
    }

    @Test
    void doesNotExposeAnInactiveCustomerMapping() {
        Customer customer = Customer.builder()
                .publicId(UUID.randomUUID())
                .keycloakUserId("keycloak-subject")
                .status(CustomerStatus.PENDING)
                .build();
        when(customerRepository.findByKeycloakUserId("keycloak-subject"))
                .thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.getCustomerIdentityByKeycloakUserId("keycloak-subject"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
