package com.workflow.dto.estimate;

import com.workflow.common.constant.financial.LineItemStatus;
import com.workflow.entity.financial.EstimateLineItem;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimateLineItemResponse {

    private Long id;
    private Long estimateId;
    private LineItemStatus status;
    private Long sourceLineItemId;
    private String productCode;
    private String productDescription;
    private String additionalDetails;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal vatRate;
    private BigDecimal netAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EstimateLineItemResponse fromEntity(EstimateLineItem item) {
        return EstimateLineItemResponse.builder()
                .id(item.getId())
                .estimateId(item.getEstimate().getId())
                .status(item.getStatus())
                .sourceLineItemId(item.getSourceLineItemId())
                .productCode(item.getProductCode())
                .productDescription(item.getProductDescription())
                .additionalDetails(item.getAdditionalDetails())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .vatRate(item.getVatRate())
                .netAmount(item.getNetAmount())
                .vatAmount(item.getVatAmount())
                .totalAmount(item.getTotalAmount())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
