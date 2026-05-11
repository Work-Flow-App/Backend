package com.workflow.dto.invoice;

import com.workflow.common.constant.CoreOrSub;
import com.workflow.entity.financial.InvoiceLineItemSnapshot;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItemSnapshotResponse {

    private Long id;
    private Long sourceLineItemId;
    private String productCode;
    private String productDescription;
    private String additionalDetails;
    private BigDecimal unitPrice;
    private CoreOrSub coreOrSub;
    private BigDecimal quantity;
    private BigDecimal vatRate;
    private BigDecimal netAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;

    public static InvoiceLineItemSnapshotResponse fromEntity(InvoiceLineItemSnapshot snap) {
        return InvoiceLineItemSnapshotResponse.builder()
                .id(snap.getId())
                .sourceLineItemId(snap.getSourceLineItemId())
                .productCode(snap.getProductCode())
                .productDescription(snap.getProductDescription())
                .additionalDetails(snap.getAdditionalDetails())
                .unitPrice(snap.getUnitPrice())
                .coreOrSub(snap.getCoreOrSub())
                .quantity(snap.getQuantity())
                .vatRate(snap.getVatRate())
                .netAmount(snap.getNetAmount())
                .vatAmount(snap.getVatAmount())
                .totalAmount(snap.getTotalAmount())
                .build();
    }
}
