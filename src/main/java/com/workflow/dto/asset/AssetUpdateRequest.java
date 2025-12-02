package com.workflow.dto.asset;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetUpdateRequest {
    private String name;
    private String description;
    private String serialNumber;
    private String assetTag;
    private BigDecimal salvageValue;
    private String currentLocation;
    private Double latitude;
    private Double longitude;
    // cannot update purchasePrice, purchaseDate, depreciationRate per requirements
}
