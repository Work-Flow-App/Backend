package com.workflow.dto.estimate;

import com.workflow.common.constant.financial.LineItemStatus;
import com.workflow.entity.financial.Estimate;
import com.workflow.entity.financial.EstimateLineItem;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
    private List<EstimateLineItemResponse> lineItems;
    private Set<Long> invoicedLineItemIds;
    private BigDecimal totalNet;
    private BigDecimal totalVat;
    private BigDecimal grandTotal;
    private BigDecimal waitingApprovalValue;
    private BigDecimal approvedValue;
    private BigDecimal invoicedValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EstimateResponse fromEntity(Estimate estimate, Set<Long> invoicedLineItemIds) {
        List<EstimateLineItemResponse> items = estimate.getLineItems().stream()
                .map(EstimateLineItemResponse::fromEntity)
                .collect(Collectors.toList());

        BigDecimal totalNet = estimate.getLineItems().stream()
                .map(EstimateLineItem::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVat = estimate.getLineItems().stream()
                .map(EstimateLineItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = estimate.getLineItems().stream()
                .map(EstimateLineItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal waitingApprovalValue = estimate.getLineItems().stream()
                .filter(eli -> eli.getStatus() == LineItemStatus.WAITING_APPROVAL)
                .map(EstimateLineItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal approvedValue = estimate.getLineItems().stream()
                .filter(eli -> eli.getStatus() == LineItemStatus.APPROVED)
                .map(EstimateLineItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal invoicedValue = estimate.getLineItems().stream()
                .filter(eli -> eli.getStatus() == LineItemStatus.INVOICED)
                .map(EstimateLineItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EstimateResponse.builder()
                .id(estimate.getId())
                .jobId(estimate.getJob().getId())
                .companyId(estimate.getCompany().getId())
                .notes(estimate.getNotes())
                .lineItems(items)
                .invoicedLineItemIds(invoicedLineItemIds)
                .totalNet(totalNet)
                .totalVat(totalVat)
                .grandTotal(grandTotal)
                .waitingApprovalValue(waitingApprovalValue)
                .approvedValue(approvedValue)
                .invoicedValue(invoicedValue)
                .createdAt(estimate.getCreatedAt())
                .updatedAt(estimate.getUpdatedAt())
                .build();
    }
}
