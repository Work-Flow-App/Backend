package com.workflow.dto.estimatedocument;

import com.workflow.dto.financial.JobLineItemSnapshotResponse;
import com.workflow.entity.financial.EstimateDocument;
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
public class EstimateDocumentResponse {

    private Long id;
    private Long estimateId;
    private Long companyId;
    private String documentNumber;
    private LocalDate validUntil;
    private String reference;
    private String notes;
    private String presignedUrl;
    private List<JobLineItemSnapshotResponse> lineItems;
    private BigDecimal totalNet;
    private BigDecimal totalVat;
    private BigDecimal grandTotal;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;

    public static EstimateDocumentResponse fromEntity(EstimateDocument doc, String presignedUrl) {
        List<JobLineItemSnapshotResponse> items = doc.getLineItemSnapshots().stream()
                .map(JobLineItemSnapshotResponse::fromEntity)
                .collect(Collectors.toList());

        return EstimateDocumentResponse.builder()
                .id(doc.getId())
                .estimateId(doc.getEstimate().getId())
                .companyId(doc.getCompany().getId())
                .documentNumber(doc.getDocumentNumber())
                .validUntil(doc.getValidUntil())
                .reference(doc.getReference())
                .notes(doc.getNotes())
                .presignedUrl(presignedUrl)
                .lineItems(items)
                .totalNet(doc.getTotalNet())
                .totalVat(doc.getTotalVat())
                .grandTotal(doc.getGrandTotal())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
