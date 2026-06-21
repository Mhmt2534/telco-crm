package com.telcox.common.core.exception;

/**
 * Platform-wide, generic error codes. The HTTP status is kept as a primitive int so that
 * {@code common-core} stays free of any web/servlet dependency.
 *
 * <p>Domain-specific error codes (e.g. CUSTOMER_KYC_PENDING) belong to the owning service,
 * not here. Services should extend {@link BaseBusinessException} with their own codes.</p>
 */
public enum ErrorCode {

    VALIDATION_ERROR("VALIDATION_ERROR", 400),
    UNAUTHORIZED("UNAUTHORIZED", 401),
    FORBIDDEN("FORBIDDEN", 403),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", 404),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", 409),
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", 422),
    DEPENDENCY_UNAVAILABLE("DEPENDENCY_UNAVAILABLE", 503),
    INTERNAL_ERROR("INTERNAL_ERROR", 500);

    private final String code;
    private final int httpStatus;

    ErrorCode(String code, int httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
