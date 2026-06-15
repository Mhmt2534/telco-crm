package com.telcocrm.common.core.context;

import java.util.List;

/**
 * Thread-bound holder for the identity/correlation data injected by the gateway.
 *
 * <p>Populated per-request by the correlation filter in {@code common-web} and consumed by
 * the JPA {@code AuditorAware} in {@code common-persistence}, logging (MDC) and controllers.
 * Always {@link #clear()} at the end of a request to avoid thread-pool leakage.</p>
 */
public final class UserContext {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private final String userId;
    private final List<String> roles;
    private final String correlationId;

    private UserContext(String userId, List<String> roles, String correlationId) {
        this.userId = userId;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.correlationId = correlationId;
    }

    public static void set(String userId, List<String> roles, String correlationId) {
        HOLDER.set(new UserContext(userId, roles, correlationId));
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
