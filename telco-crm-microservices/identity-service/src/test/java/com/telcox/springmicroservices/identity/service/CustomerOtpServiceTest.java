package com.telcox.springmicroservices.identity.service;

import com.telcox.springmicroservices.identity.client.CustomerServiceClient;
import com.telcox.springmicroservices.identity.client.dto.CustomerOtpInfo;
import com.telcox.springmicroservices.identity.config.KeycloakProperties;
import com.telcox.springmicroservices.identity.domain.OtpAttempt;
import com.telcox.springmicroservices.identity.domain.OtpAttemptRepository;
import com.telcox.springmicroservices.identity.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class CustomerOtpServiceTest {

    private static final String E164_PHONE = "905321234567";
    private static final String LOCAL_PHONE = "5321234567";
    private static final String OTP_KEY = "otp:" + E164_PHONE;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private OtpAttemptRepository otpAttemptRepository;

    @Mock
    private CustomerServiceClient customerServiceClient;

    private CustomerOtpService customerOtpService;
    private MockRestServiceServer keycloakServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        keycloakServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        KeycloakProperties keycloakProperties = new KeycloakProperties();
        keycloakProperties.setServerUrl("http://keycloak.test");
        keycloakProperties.setRealm("telco-crm-realm");
        keycloakProperties.setClientId("telco-crm-client");
        keycloakProperties.setClientSecret("test-secret");

        customerOtpService = new CustomerOtpService(
                redisTemplate,
                otpAttemptRepository,
                customerServiceClient,
                restClientBuilder.build(),
                keycloakProperties
        );
    }

    @Test
    void normalizesTurkishCountryCodeToLocalFormat() {
        assertThat(PhoneNumberNormalizer.normalizeToLocalFormat(E164_PHONE)).isEqualTo(LOCAL_PHONE);
        assertThat(PhoneNumberNormalizer.normalizeToLocalFormat("+" + E164_PHONE)).isEqualTo(LOCAL_PHONE);
        assertThat(PhoneNumberNormalizer.normalizeToLocalFormat(LOCAL_PHONE)).isEqualTo(LOCAL_PHONE);
    }

    @Test
    void requestOtpUsesLocalPhoneForCustomerLookupAndE164PhoneForRedisKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(customerServiceClient.getCustomerByPhone(LOCAL_PHONE)).thenReturn(approvedCustomer());

        customerOtpService.requestOtp(E164_PHONE);

        verify(customerServiceClient).getCustomerByPhone(LOCAL_PHONE);
        verify(valueOperations).set(matches("otp:" + E164_PHONE), matches("[0-9]{6}"),
                org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(5)));
    }

    @Test
    void verifyOtpReadsE164KeyAndUsesLocalPhoneForCustomerLookup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(otpAttemptRepository.findByPhone(E164_PHONE))
                .thenReturn(Optional.of(OtpAttempt.forPhone(E164_PHONE)));
        when(valueOperations.get(OTP_KEY)).thenReturn("123456");
        when(customerServiceClient.getCustomerByPhone(LOCAL_PHONE)).thenReturn(approvedCustomer());

        keycloakServer.expect(once(), requestTo(
                        "http://keycloak.test/realms/telco-crm-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"access-token","token_type":"Bearer"}
                        """, MediaType.APPLICATION_JSON));

        TokenResponse token = customerOtpService.verifyOtp(E164_PHONE, "123456");

        assertThat(token.getAccessToken()).isEqualTo("access-token");
        verify(otpAttemptRepository).findByPhone(E164_PHONE);
        verify(valueOperations).get(OTP_KEY);
        verify(redisTemplate).delete(OTP_KEY);
        verify(customerServiceClient).getCustomerByPhone(LOCAL_PHONE);
        keycloakServer.verify();
    }

    private CustomerOtpInfo approvedCustomer() {
        CustomerOtpInfo customer = new CustomerOtpInfo();
        customer.setPhone(LOCAL_PHONE);
        customer.setKycApproved(true);
        customer.setKeycloakUserId("customer-keycloak-id");
        customer.setInternalKeycloakPassword("internal-password");
        return customer;
    }
}
