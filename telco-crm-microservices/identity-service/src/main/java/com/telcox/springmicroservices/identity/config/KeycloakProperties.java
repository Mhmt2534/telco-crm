package com.telcox.springmicroservices.identity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Keycloak bağlantı ve client parametrelerini application.yml'den okur.
 * <p>
 * Örnek config (application-dev.yml):
 * <pre>
 * keycloak:
 *   server-url: http://localhost:9011
 *   realm: telco-crm-realm
 *   client-id: telco-crm-client
 *   client-secret: ${KEYCLOAK_CLIENT_SECRET}
 * </pre>
 * <p>
 * client-secret için: {@code setx KEYCLOAK_CLIENT_SECRET <değer>}
 */
@Data
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    /** Keycloak sunucu adresi. Host'ta 9011, Docker ağında keycloak:8080. */
    private String serverUrl;

    /** Proje realm adı: telco-crm-realm */
    private String realm;

    /** Staff/Customer login işlemi için kullanılan Keycloak client kimliği. */
    private String clientId;

    /**
     * Keycloak client secret.
     * Güvenlik gereği asla kaynak koduna yazılmaz; env variable ile enjekte edilir:
     * {@code setx KEYCLOAK_CLIENT_SECRET <değer>}
     */
    private String clientSecret;

    /**
     * Keycloak token endpoint URL'ini döner.
     * Örn: http://localhost:9011/realms/telco-crm-realm/protocol/openid-connect/token
     */
    public String getTokenUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    /**
     * Keycloak logout endpoint URL'ini döner.
     * Örn: http://localhost:9011/realms/telco-crm-realm/protocol/openid-connect/logout
     */
    public String getLogoutUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }
}
