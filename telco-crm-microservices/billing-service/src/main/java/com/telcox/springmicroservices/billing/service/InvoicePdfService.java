package com.telcox.springmicroservices.billing.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.telcox.springmicroservices.billing.client.CustomerClient;
import com.telcox.springmicroservices.billing.dto.CustomerResponse;
import com.telcox.springmicroservices.billing.dto.AddressDto;
import com.telcox.springmicroservices.billing.entity.Invoice;
import com.telcox.springmicroservices.billing.entity.InvoiceLine;
import com.telcox.springmicroservices.billing.repository.InvoiceRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerClient customerClient;
    private final MinioClient minioClient;

    @Value("${app.minio.bucket-name:telcox-invoices}")
    private String bucketName;

    @Transactional(readOnly = true)
    public void generateAndUploadInvoicePdf(Long invoiceId) {
        log.info("Starting PDF generation for invoice id: {}", invoiceId);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
        if (invoiceOpt.isEmpty()) {
            log.error("Invoice with id {} not found for PDF generation", invoiceId);
            return;
        }

        Invoice invoice = invoiceOpt.get();

        CustomerResponse customer = null;
        try {
            customer = customerClient.getCustomerById(invoice.getCustomerId());
        } catch (Exception e) {
            log.warn("Failed to fetch customer info for customerId: {} via Feign, using fallback", invoice.getCustomerId(), e);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            Paragraph title = new Paragraph("TELCOX BILLING SYSTEM - INVOICE")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            float[] metaColumns = {1, 1};
            Table metaTable = new Table(metaColumns);

            String companyDetails = "Telcox Telecommunications A.S.\n" +
                    "Address: ITU Ari Teknokent, Maslak/Istanbul\n" +
                    "Phone: 0850 123 45 67\n" +
                    "Email: support@telcox.com";
            metaTable.addCell(new Cell().add(new Paragraph(companyDetails)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

            String invoiceNumStr = "Invoice ID: " + invoice.getId() + "\n" +
                    "Due Date: " + invoice.getDueDate().toString().substring(0, 10) + "\n" +
                    "Subscription ID: " + invoice.getSubscriptionId() + "\n" +
                    "Status: " + invoice.getStatus().name() + "\n";
            if (customer != null) {
                invoiceNumStr += "Customer Name: " + customer.getFirstName() + " " + customer.getLastName() + "\n" +
                        "Identity No: " + customer.getMaskedIdentityNumber() + "\n" +
                        "Email: " + customer.getEmail();
            } else {
                invoiceNumStr += "Customer ID: " + invoice.getCustomerId();
            }
            metaTable.addCell(new Cell().add(new Paragraph(invoiceNumStr)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            document.add(metaTable);
            document.add(new Paragraph("\n"));

            if (customer != null && customer.getAddresses() != null && !customer.getAddresses().isEmpty()) {
                AddressDto addr = customer.getAddresses().stream()
                        .filter(AddressDto::isDefault)
                        .findFirst()
                        .orElse(customer.getAddresses().get(0));
                document.add(new Paragraph("Billing Address:\n" + addr.getLine1() + ", " + addr.getDistrict() + "/" + addr.getCity()).setBold());
                document.add(new Paragraph("\n"));
            }

            float[] lineColumns = {3, 1, 1, 1};
            Table lineTable = new Table(lineColumns);
            lineTable.addCell(new Cell().add(new Paragraph("Description").setBold()));
            lineTable.addCell(new Cell().add(new Paragraph("Quantity").setBold()).setTextAlignment(TextAlignment.CENTER));
            lineTable.addCell(new Cell().add(new Paragraph("Unit Price").setBold()).setTextAlignment(TextAlignment.RIGHT));
            lineTable.addCell(new Cell().add(new Paragraph("Total").setBold()).setTextAlignment(TextAlignment.RIGHT));

            for (InvoiceLine line : invoice.getLines()) {
                lineTable.addCell(new Cell().add(new Paragraph(line.getDescription())));
                lineTable.addCell(new Cell().add(new Paragraph(String.valueOf(line.getQuantity()))).setTextAlignment(TextAlignment.CENTER));
                lineTable.addCell(new Cell().add(new Paragraph(line.getUnitPrice() + " TL")).setTextAlignment(TextAlignment.RIGHT));
                lineTable.addCell(new Cell().add(new Paragraph(line.getLineTotal() + " TL")).setTextAlignment(TextAlignment.RIGHT));
            }
            document.add(lineTable);
            document.add(new Paragraph("\n"));

            BigDecimal totalAmount = invoice.getAmount();
            BigDecimal subtotal = totalAmount.divide(BigDecimal.valueOf(1.20), 2, RoundingMode.HALF_UP);
            BigDecimal kdv = totalAmount.subtract(subtotal);

            float[] summaryColumns = {3, 1};
            Table summaryTable = new Table(summaryColumns);
            summaryTable.addCell(new Cell().add(new Paragraph("Subtotal (Excl. Tax)")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
            summaryTable.addCell(new Cell().add(new Paragraph(subtotal + " TL")).setTextAlignment(TextAlignment.RIGHT));
            summaryTable.addCell(new Cell().add(new Paragraph("KDV (20%)")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
            summaryTable.addCell(new Cell().add(new Paragraph(kdv + " TL")).setTextAlignment(TextAlignment.RIGHT));
            summaryTable.addCell(new Cell().add(new Paragraph("Grand Total")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setBold());
            summaryTable.addCell(new Cell().add(new Paragraph(totalAmount + " TL")).setTextAlignment(TextAlignment.RIGHT).setBold());
            document.add(summaryTable);

            document.close();
            log.info("PDF document successfully built for invoice id: {}", invoiceId);

            byte[] pdfBytes = out.toByteArray();
            String s3Path = "invoices/" + invoice.getCustomerId() + "/" + invoice.getId() + ".pdf";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(s3Path)
                            .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                            .contentType("application/pdf")
                            .build()
            );

            log.info("Invoice PDF successfully uploaded to MinIO at: bucket={}, path={}", bucketName, s3Path);

        } catch (Exception e) {
            log.error("Failed to generate and upload PDF for invoice id: {}", invoiceId, e);
            throw new RuntimeException("Invoice PDF generation/upload error", e);
        }
    }
}
