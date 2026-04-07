package com.workflow.dto.asset;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetUpdateRequest {

    @Size(min = 2, max = 150, message = "Asset name must be between 2 and 150 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    private String serialNumber;

    @Size(max = 50, message = "Asset tag must not exceed 50 characters")
    private String assetTag;

    // salvageValue < purchasePrice is a cross-field rule — validated in the service
    @DecimalMin(value = "0.00", message = "Salvage value must not be negative")
    private BigDecimal salvageValue;

    // cannot update purchasePrice, purchaseDate, depreciationRate per requirements
}
