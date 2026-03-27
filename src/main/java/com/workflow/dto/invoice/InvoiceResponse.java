package com.workflow.dto.invoice;

import com.workflow.dto.estimate.LineItemResponse;
import com.workflow.entity.Invoice;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String presignedUrl;
    private List<LineItemResponse> lineItems;
    private BigDecimal totalNet;
    private BigDecimal totalVat;
    private BigDecimal grandTotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InvoiceResponse fromEntity(Invoice invoice, String presignedUrl) {
        List<LineItemResponse> items = invoice.getLineItems().stream()
                .map(LineItemResponse::fromEntity)
                .collect(Collectors.toList());

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .estimateId(invoice.getEstimate().getId())
                .companyId(invoice.getCompany().getId())
                .invoiceNumber(invoice.getInvoiceNumber())
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
