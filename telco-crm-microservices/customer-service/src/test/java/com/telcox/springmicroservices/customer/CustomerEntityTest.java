package com.telcox.springmicroservices.customer;

import com.telcox.springmicroservices.customer.domain.Customer;
import com.telcox.springmicroservices.customer.domain.enums.CustomerType;
import com.telcox.springmicroservices.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=update")
class CustomerEntityTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @Transactional
    void shouldSaveAndEncryptCustomer() {
        // given
        Customer customer = Customer.builder()
                .firstName("Ali")
                .lastName("Veli")
                .identityNumber("12345678901")
                .type(CustomerType.INDIVIDUAL)
                .phone("905551234567")
                .email("ali@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        // when
        Customer saved = customerRepository.saveAndFlush(customer);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPublicId()).isNotNull();
        assertThat(saved.getIdentityNumber()).isEqualTo("12345678901");
        
        // Soft delete test
        assertThat(saved.isDeleted()).isFalse();
    }
}
