package com.telcocrm.common.web.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import com.telcocrm.common.core.constant.HeaderConstants;
import com.telcocrm.common.core.context.UserContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Establishes the per-request correlation/identity context:
 * <ul>
 *   <li>reads (or generates) the correlation id and echoes it back on the response,</li>
 *   <li>reads the gateway-injected {@code X-User-Id} / {@code X-User-Roles} headers,</li>
 *   <li>publishes everything to {@link MDC} (for JSON logs) and {@link UserContext},</li>
 *   <li>clears both at the end of the request to prevent thread-pool leakage.</li>
 * </ul>
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(HeaderConstants.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String userId = request.getHeader(HeaderConstants.USER_ID);
        List<String> roles = parseRoles(request.getHeader(HeaderConstants.USER_ROLES));

        try {
            MDC.put(MDC_CORRELATION_ID, correlationId);
            if (userId != null && !userId.isBlank()) {
                MDC.put(MDC_USER_ID, userId);
            }
            UserContext.set(userId, roles, correlationId);
            response.setHeader(HeaderConstants.CORRELATION_ID, correlationId);

            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private List<String> parseRoles(String header) {
        if (header == null || header.isBlank()) {
            return List.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .toList();
    }
}
