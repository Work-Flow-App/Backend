package com.workflow.dto.company;

import com.workflow.common.constant.PlanType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutSessionRequest(
        @NotNull(message = "Plan type is required")
        PlanType planType,

        @Min(value = 0, message = "Extra seats cannot be negative")
        Integer extraSeats,

        @Min(value = 0, message = "Extra storage blocks cannot be negative")
        Integer extraStorageBlocks
) {
    public CreateCheckoutSessionRequest {
        if (extraSeats == null) {
            extraSeats = 0;
        }
        if (extraStorageBlocks == null) {
            extraStorageBlocks = 0;
        }
    }
}
