package com.telcox.springmicroservices.payment.controller;

import com.telcox.springmicroservices.payment.dto.PaymentRequest;
import com.telcox.springmicroservices.payment.dto.PaymentResponse;
import com.telcox.springmicroservices.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment operations")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Initiate a new payment", description = "Processes a payment request idempotently")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment successfully processed or cached response returned",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or missing Idempotency-Key",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Idempotency conflict (request already in progress)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Parameter(description = "Idempotency key to prevent double charging", required = true)
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @Parameter(description = "User ID propagated from API Gateway")
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "Correlation ID propagated from API Gateway")
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @Valid @RequestBody PaymentRequest request) {

        try {
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            if (userId != null) {
                MDC.put("userId", userId);
            }

            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("Idempotency-Key header is required");
            }

            log.info("Received payment request for Invoice ID: {} with Idempotency-Key: {}", request.getInvoiceId(), idempotencyKey);
            
            PaymentResponse response = paymentService.initiatePayment(request, idempotencyKey, userId);
            return ResponseEntity.ok(response);
            
        } finally {
            MDC.remove("correlationId");
            MDC.remove("userId");
        }
    }
}
