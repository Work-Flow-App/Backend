package com.workflow.dto.worker;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveRequestUpdateRequest(
        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String reason
) {}
