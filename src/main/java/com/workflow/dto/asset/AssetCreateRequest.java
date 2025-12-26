package com.workflow.dto.asset;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCreateRequest {
    private String name;
    private String description;
    private String serialNumber;
    private String assetTag;
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private BigDecimal depreciationRate; // percent 0-100
    private BigDecimal salvageValue;
}
