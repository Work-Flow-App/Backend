package com.workflow.dto.estimate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineItemCreateRequest {

    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotBlank(message = "Product description is required")
    private String productDescription;

    private String additionalDetails;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0", message = "Unit price must be zero or greater")
    private BigDecimal unitPrice;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0", inclusive = false, message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "VAT rate is required")
    @DecimalMin(value = "0", message = "VAT rate must be between 0 and 100")
    @DecimalMax(value = "100", message = "VAT rate must be between 0 and 100")
    private BigDecimal vatRate;
}
