package com.workflow.dto.estimate;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkLineItemRequest {

    @NotNull(message = "Line item ID is required")
    private Long lineItemId;
}
