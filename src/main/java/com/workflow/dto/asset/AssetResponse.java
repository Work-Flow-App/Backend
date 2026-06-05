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
public class AssetResponse {
    private Long id;
    private Long assetRef;
    private Long companyId;
    private String name;
    private String description;
    private String serialNumber;
    private String assetTag;
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private BigDecimal depreciationRate;
    private BigDecimal salvageValue;
    private boolean available;
    private boolean archived;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;
}
