package com.workflow.dto.invoice;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InvoiceCreateRequest {

    @NotEmpty(message = "At least one line item must be selected")
    private List<Long> lineItemIds;

    private LocalDate dueDate;

    @Size(max = 100, message = "Reference cannot exceed 100 characters")
    private String reference;
}
