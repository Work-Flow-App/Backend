package com.workflow.dto.estimatedocument;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EstimateDocumentCreateRequest {

    @NotEmpty(message = "At least one line item must be selected")
    private List<Long> lineItemIds;

    private LocalDate validUntil;

    @Size(max = 100, message = "Reference cannot exceed 100 characters")
    private String reference;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
