package com.workflow.dto.asset;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetValueResponse {
    private Long assetId;
    private String assetName;
    private BigDecimal purchasePrice;
    private BigDecimal currentValue;
    private BigDecimal totalDepreciation;
    private BigDecimal depreciationRate;
    private BigDecimal salvageValue;
    private LocalDate purchaseDate;
    private long daysOwned;
    private double yearsOwned;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime valueAsOfDate;
}
