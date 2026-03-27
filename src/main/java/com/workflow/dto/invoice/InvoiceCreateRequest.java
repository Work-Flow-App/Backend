package com.workflow.dto.invoice;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class InvoiceCreateRequest {

    @NotEmpty(message = "At least one line item must be selected")
    private List<Long> lineItemIds;
}
