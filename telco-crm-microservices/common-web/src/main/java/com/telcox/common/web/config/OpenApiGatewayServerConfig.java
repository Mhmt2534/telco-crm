package com.telcox.common.web.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Forces every microservice's OpenAPI spec to advertise a <strong>relative</strong> server URL
 * ({@code "/"}) instead of the auto-detected hostname and port.
 *
 * <p>When the Swagger UI is served through the API Gateway (aggregation mode),
 * "Try it out" requests are sent relative to the page's origin – i.e. the Gateway at
 * {@code localhost:8080} – rather than to the service's internal address
 * (e.g. {@code http://ARSEN:9004}). This eliminates CORS / network errors.</p>
 *
 * <p>This configuration activates only when {@code springdoc-openapi} is on the classpath
 * ({@link ConditionalOnClass}) and the application is a Servlet-based web application.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenAPI.class)
public class OpenApiGatewayServerConfig {

    @Bean
    public OpenAPI gatewayRelativeServerOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/").description("API Gateway (relative)")));
    }
}
