package com.workflow.dto.asset;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetResponse {
    private Long id;
    private Long companyId;
    private String name;
    private String description;
    private String serialNumber;
    private String assetTag;
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private BigDecimal depreciationRate;
    private BigDecimal salvageValue;
    private String currentLocation;
    private Double latitude;
    private Double longitude;
    private boolean available;
    private boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
