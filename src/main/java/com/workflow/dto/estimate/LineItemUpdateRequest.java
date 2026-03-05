package com.workflow.dto.estimate;

import com.workflow.common.constant.CoreOrSub;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineItemUpdateRequest {

    private String productCode;

    private String productDescription;

    private String additionalDetails;

    @DecimalMin(value = "0", message = "Unit price must be zero or greater")
    private BigDecimal unitPrice;

    private CoreOrSub coreOrSub;

    @DecimalMin(value = "0", inclusive = false, message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @DecimalMin(value = "0", message = "VAT rate must be zero or greater")
    private BigDecimal vatRate;
}
