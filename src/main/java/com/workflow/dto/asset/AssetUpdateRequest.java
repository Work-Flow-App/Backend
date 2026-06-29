package com.workflow.dto.asset;

import com.workflow.dto.job.AddressRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @DecimalMin(value = "0.01", message = "Purchase price must be greater than 0")
    private BigDecimal purchasePrice;

    @PastOrPresent(message = "Purchase date cannot be in the future")
    private LocalDate purchaseDate;

    @DecimalMin(value = "0.00", message = "Depreciation rate must be at least 0")
    @DecimalMax(value = "100.00", message = "Depreciation rate must not exceed 100")
    private BigDecimal depreciationRate;

    @DecimalMin(value = "0.00", message = "Salvage value must not be negative")
    private BigDecimal salvageValue;

    private AddressRequest warehouseAddress;
}