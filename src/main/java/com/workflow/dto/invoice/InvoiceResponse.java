package com.workflow.dto.invoice;

import com.workflow.dto.financial.JobLineItemSnapshotResponse;
import com.workflow.entity.financial.Invoice;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private Long id;
    private Long estimateId;
    private Long companyId;
    private String invoiceNumber;
    private LocalDate dueDate;
    private String reference;
    private String presignedUrl;
    private List<JobLineItemSnapshotResponse> lineItems;
    private BigDecimal totalNet;
    private BigDecimal totalVat;
    private BigDecimal grandTotal;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;

    public static InvoiceResponse fromEntity(Invoice invoice, String presignedUrl) {
        List<JobLineItemSnapshotResponse> items = invoice.getLineItemSnapshots().stream()
                .map(JobLineItemSnapshotResponse::fromEntity)
                .collect(Collectors.toList());

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .estimateId(invoice.getEstimate().getId())
                .companyId(invoice.getCompany().getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .dueDate(invoice.getDueDate())
                .reference(invoice.getReference())
                .presignedUrl(presignedUrl)
                .lineItems(items)
                .totalNet(invoice.getTotalNet())
                .totalVat(invoice.getTotalVat())
                .grandTotal(invoice.getGrandTotal())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}
