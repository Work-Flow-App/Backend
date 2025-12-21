package com.workflow.dto.asset;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetStatistics {
    private long totalAssets;
    private long availableAssets;
    private long assetsInUse;
    private BigDecimal totalPurchaseValue;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalDepreciation;
    private double averageDepreciationRate;
}
