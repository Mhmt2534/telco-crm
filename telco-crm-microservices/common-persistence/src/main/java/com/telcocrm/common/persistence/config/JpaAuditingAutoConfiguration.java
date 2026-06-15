package com.telcocrm.common.persistence.config;

import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.telcocrm.common.core.context.UserContext;

import jakarta.persistence.EntityManager;

/**
 * Activates Spring Data JPA auditing for every service that depends on {@code common-persistence},
 * and supplies the auditor (the gateway-injected user id from {@link UserContext}). Registered via
 * {@code AutoConfiguration.imports} so it applies despite the cross-package layout.
 */
@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingAutoConfiguration {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            UserContext ctx = UserContext.get();
            if (ctx == null || ctx.getUserId() == null || ctx.getUserId().isBlank()) {
                return Optional.of("system");
            }
            return Optional.of(ctx.getUserId());
        };
    }
}
