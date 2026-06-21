package com.telcox.common.web.exception;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.telcox.common.core.exception.BaseBusinessException;
import com.telcox.common.core.exception.ErrorCode;
import com.telcox.common.core.model.ProblemDetails;

import jakarta.validation.ConstraintViolationException;

/**
 * Central exception handler producing a uniform RFC 7807 {@link ProblemDetails} response
 * (content type {@code application/problem+json}) for every service. Imported via auto-config,
 * so individual services get it for free without re-declaring advice.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_PREFIX = "https://telco.example/errors/";

    @ExceptionHandler(BaseBusinessException.class)
    public ResponseEntity<ProblemDetails> handleBusiness(BaseBusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        HttpStatus status = HttpStatus.valueOf(code.httpStatus());
        log.warn("Business exception [{}]: {}", code.code(), ex.getMessage());
        return build(status, code.code(), code.code().toLowerCase().replace('_', '-'),
                ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handleValidation(MethodArgumentNotValidException ex) {
        List<ProblemDetails.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.code(), "validation-error",
                "Request validation failed", violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetails> handleConstraint(ConstraintViolationException ex) {
        List<ProblemDetails.FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> new ProblemDetails.FieldViolation(
                        v.getPropertyPath() == null ? null : v.getPropertyPath().toString(),
                        v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.code(), "validation-error",
                "Request validation failed", violations);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.code(), "internal-error",
                "An unexpected error occurred", null);
    }

    private ProblemDetails.FieldViolation toViolation(FieldError fieldError) {
        return new ProblemDetails.FieldViolation(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ProblemDetails> build(HttpStatus status, String code, String typeSuffix,
            String detail, List<ProblemDetails.FieldViolation> errors) {
        ProblemDetails body = new ProblemDetails(
                TYPE_PREFIX + typeSuffix,
                status.getReasonPhrase(),
                status.value(),
                detail,
                currentRequestUri(),
                code,
                MDC.get(CorrelationIdMdcKey.CORRELATION_ID),
                OffsetDateTime.now(),
                errors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    private String currentRequestUri() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest().getRequestURI();
        }
        return null;
    }

    /** Local mirror of the MDC key to avoid a hard dependency on the filter class ordering. */
    private static final class CorrelationIdMdcKey {
        private static final String CORRELATION_ID = "correlationId";
    }
}
