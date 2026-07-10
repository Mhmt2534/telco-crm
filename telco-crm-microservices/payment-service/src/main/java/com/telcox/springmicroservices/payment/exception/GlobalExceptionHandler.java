package com.telcox.springmicroservices.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdempotencyException.class)
    public ProblemDetail handleIdempotencyException(IdempotencyException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Idempotency Conflict");
        problemDetail.setType(URI.create("https://telcox.com/errors/idempotency-conflict"));
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://telcox.com/errors/bad-request"));
        return problemDetail;
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public org.springframework.http.ResponseEntity<org.springframework.http.ProblemDetail> handleMissingHeaderException(
            org.springframework.web.bind.MissingRequestHeaderException ex,
            jakarta.servlet.http.HttpServletRequest request) {

        org.springframework.http.ProblemDetail problemDetail = org.springframework.http.ProblemDetail
                .forStatusAndDetail(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        String.format("Required HTTP header '%s' is missing", ex.getHeaderName()));

        problemDetail.setTitle("Bad Request");
        problemDetail.setType(java.net.URI.create("https://telco.example/errors/bad-request"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));

        // Loglardaki correlationId'yi buraya da mapleyelim (Mevcut altyapındaki
        // pattern'e göre)
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId != null) {
            problemDetail.setProperty("correlationId", correlationId);
        }

        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }
}
