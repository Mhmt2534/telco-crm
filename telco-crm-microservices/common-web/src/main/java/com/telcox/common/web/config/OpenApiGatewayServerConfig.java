package com.telcox.common.web.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Shared OpenAPI configuration automatically applied to every servlet-based microservice
 * that has {@code springdoc-openapi} on its classpath.
 *
 * <p>Uses {@link OpenApiCustomizer} (a post-processing callback) instead of providing a raw
 * {@link OpenAPI} bean. This ensures our customizations are applied <em>after</em> springdoc
 * assembles the spec from controller scanning, so they are never overwritten during the
 * internal merge step.</p>
 *
 * <p><strong>1 — Relative server URL:</strong> Forces the OpenAPI spec to advertise
 * {@code "/"} instead of the auto-detected hostname and port. When the Swagger UI is
 * served through the API Gateway (aggregation mode), "Try it out" requests are sent
 * relative to the page's origin (i.e. the Gateway at {@code localhost:8080}) rather than
 * to the service's internal address (e.g. {@code http://ARSEN:9004}).</p>
 *
 * <p><strong>2 — JWT Bearer security scheme:</strong> Registers a {@code bearerAuth}
 * security scheme and applies it globally. This makes the "Authorize" button and lock
 * icons appear in Swagger UI, allowing users to paste a JWT token that is automatically
 * attached to all subsequent "Try it out" requests.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenAPI.class)
public class OpenApiGatewayServerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Post-processes the assembled OpenAPI spec to inject the relative server URL
     * and the global JWT Bearer security scheme.
     */
    @Bean
    public OpenApiCustomizer gatewayOpenApiCustomizer() {
        return openApi -> {
            // 1. Override server URL to relative "/"
            openApi.servers(List.of(
                    new Server().url("/").description("API Gateway (relative)")));

            // 2. Inject JWT Bearer security scheme into components
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            components.addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                    .name(SECURITY_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Keycloak JWT token"));

            // 3. Apply security requirement globally to all endpoints
            openApi.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
        };
    }
}
