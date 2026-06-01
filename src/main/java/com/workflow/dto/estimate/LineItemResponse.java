package com.workflow.dto.estimate;

import com.workflow.entity.financial.LineItem;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineItemResponse {

    private Long id;
    private Long companyId;
    private String productCode;
    private String productDescription;
    private String additionalDetails;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal vatRate;
    private BigDecimal netAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;

    public static LineItemResponse fromEntity(LineItem item) {
        return LineItemResponse.builder()
                .id(item.getId())
                .companyId(item.getCompany().getId())
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
