package com.workflow.dto.estimate;

import com.workflow.entity.financial.Estimate;
import com.workflow.entity.financial.LineItem;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimateResponse {

    private Long id;
    private Long jobId;
    private Long companyId;
    private String notes;
    private List<LineItemResponse> lineItems;
    private BigDecimal totalNet;
    private BigDecimal totalVat;
    private BigDecimal grandTotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EstimateResponse fromEntity(Estimate estimate) {
        List<LineItemResponse> items = estimate.getLineItems().stream()
                .map(LineItemResponse::fromEntity)
                .collect(Collectors.toList());

        BigDecimal totalNet = estimate.getLineItems().stream()
                .map(LineItem::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVat = estimate.getLineItems().stream()
                .map(LineItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = estimate.getLineItems().stream()
                .map(LineItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EstimateResponse.builder()
                .id(estimate.getId())
                .jobId(estimate.getJob().getId())
                .companyId(estimate.getCompany().getId())
                .notes(estimate.getNotes())
                .lineItems(items)
                .totalNet(totalNet)
                .totalVat(totalVat)
                .grandTotal(grandTotal)
                .createdAt(estimate.getCreatedAt())
                .updatedAt(estimate.getUpdatedAt())
                .build();
    }
}
