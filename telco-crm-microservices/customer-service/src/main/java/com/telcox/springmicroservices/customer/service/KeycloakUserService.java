package com.telcox.springmicroservices.customer.service;

import com.telcox.common.core.exception.BaseBusinessException;
import com.telcox.springmicroservices.customer.config.KeycloakProperties;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private final Keycloak keycloak;
    private final KeycloakProperties keycloakProperties;

    private static final String CUSTOMER_ROLE = "CUSTOMER";

    /**
     * Creates a new user in Keycloak with the given phone as username and internal password.
     * Assigns the CUSTOMER role.
     *
     * @param phone    The customer's phone number (used as username)
     * @param password The randomly generated internal password
     * @param firstName The customer's first name
     * @param lastName The customer's last name
     * @param email The customer's email address
     * @return The ID of the newly created Keycloak user
     */
    public String createCustomerUser(String phone, String password, String firstName, String lastName, String email) {
        log.info("Keycloak üzerinde müşteri kullanıcısı oluşturuluyor. Username: {}", phone);
        
        RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
        UsersResource usersResource = realmResource.users();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(phone);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setEmailVerified(true);
        user.setEnabled(true);

        Response response = usersResource.create(user);
        
        if (response.getStatus() != 201) {
            String errorMsg = "Keycloak kullanıcı oluşturulamadı. HTTP Status: " + response.getStatus();
            log.error(errorMsg);
            throw new KeycloakException(errorMsg);
        }

        String userId = getCreatedId(response);
        log.info("Keycloak User oluşturuldu. ID: {}", userId);

        UserResource userResource = usersResource.get(userId);

        // Set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        
        userResource.resetPassword(credential);
        log.info("Keycloak kullanıcısı için şifre belirlendi. ID: {}", userId);

        // Assign CUSTOMER role
        try {
            RoleRepresentation customerRole = realmResource.roles().get(CUSTOMER_ROLE).toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(customerRole));
            log.info("Keycloak kullanıcısına CUSTOMER rolü atandı. ID: {}", userId);
        } catch (Exception e) {
            log.error("Keycloak rol atama sırasında hata oluştu. Role: {}", CUSTOMER_ROLE, e);
            throw new KeycloakException("Rol atama hatası: " + e.getMessage());
        }

        return userId;
    }

    private String getCreatedId(Response response) {
        String path = response.getLocation().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
    
    public static class KeycloakException extends BaseBusinessException {
        public KeycloakException(String message) {
            super(com.telcox.common.core.exception.ErrorCode.DEPENDENCY_UNAVAILABLE, message);
        }
    }
}
