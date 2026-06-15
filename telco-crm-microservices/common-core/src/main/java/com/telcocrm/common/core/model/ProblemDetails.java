package com.telcocrm.common.core.model;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * RFC 7807 Problem Details payload, extended with {@code correlationId} and {@code timestamp}.
 * This is the single error contract every service returns. Built by the global exception
 * handler in {@code common-web}; kept here so non-web modules can reference the same shape.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetails(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String correlationId,
        OffsetDateTime timestamp,
        List<FieldViolation> errors) {

    /** A single field-level validation violation. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldViolation(String field, String message) {
    }
}
