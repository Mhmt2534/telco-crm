package com.telcox.springmicroservices.billing.controller;

import com.telcox.springmicroservices.billing.entity.Invoice;
import com.telcox.springmicroservices.billing.repository.InvoiceRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final MinioClient minioClient;

    @Value("${app.minio.bucket-name:telcox-invoices}")
    private String bucketName;

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Map<String, String>> getInvoicePdfUrl(@PathVariable("id") UUID id) {
        log.info("Request received to get PDF URL for invoice id: {}", id);

        Optional<Invoice> invoiceOpt = invoiceRepository.findByPublicId(id);
        if (invoiceOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }

        Invoice invoice = invoiceOpt.get();
        String s3Path = "invoices/" + invoice.getCustomerId() + "/" + invoice.getPublicId() + ".pdf";

        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(s3Path)
                            .expiry(600) // 10 minutes in seconds
                            .build()
            );

            log.info("Successfully generated presigned URL for invoice id: {}", id);
            return ResponseEntity.ok(Map.of("pdfUrl", url));
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for invoice id: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate PDF download URL", e);
        }
    }
}
