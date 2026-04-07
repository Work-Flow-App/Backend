package com.workflow.dto.asset;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCreateRequest {

    @NotBlank(message = "Asset name is required")
    @Size(min = 2, max = 150, message = "Asset name must be between 2 and 150 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    private String serialNumber;

    @Size(max = 50, message = "Asset tag must not exceed 50 characters")
    private String assetTag;

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.01", message = "Purchase price must be greater than 0")
    private BigDecimal purchasePrice;

    @NotNull(message = "Purchase date is required")
    @PastOrPresent(message = "Purchase date cannot be in the future")
    private LocalDate purchaseDate;

    @NotNull(message = "Depreciation rate is required")
    @DecimalMin(value = "0.00", message = "Depreciation rate must be at least 0")
    @DecimalMax(value = "100.00", message = "Depreciation rate must not exceed 100")
    private BigDecimal depreciationRate; // percent 0-100

    // salvageValue < purchasePrice is a cross-field rule — validated in the service
    private BigDecimal salvageValue;
}
