package com.workflow.dto.financial;

import com.workflow.common.constant.financial.SnapshotType;
import com.workflow.entity.financial.JobLineItemSnapshot;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobLineItemSnapshotResponse {

    private Long id;
    private SnapshotType type;
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

    public static JobLineItemSnapshotResponse fromEntity(JobLineItemSnapshot snap) {
        return JobLineItemSnapshotResponse.builder()
                .id(snap.getId())
                .type(snap.getType())
                .sourceLineItemId(snap.getSourceLineItemId())
                .productCode(snap.getProductCode())
                .productDescription(snap.getProductDescription())
                .additionalDetails(snap.getAdditionalDetails())
                .unitPrice(snap.getUnitPrice())
                .quantity(snap.getQuantity())
                .vatRate(snap.getVatRate())
                .netAmount(snap.getNetAmount())
                .vatAmount(snap.getVatAmount())
                .totalAmount(snap.getTotalAmount())
                .createdAt(snap.getCreatedAt())
                .build();
    }
}
