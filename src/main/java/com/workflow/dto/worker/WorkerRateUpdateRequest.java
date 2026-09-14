package com.workflow.dto.worker;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record WorkerRateUpdateRequest(
        @NotNull(message = "Hourly rate is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Hourly rate cannot be negative")
        BigDecimal hourlyRate,

        // Optional — not every worker is entitled to overtime pay
        @DecimalMin(value = "0.0", inclusive = true, message = "Overtime rate cannot be negative")
        BigDecimal overtimeRate
) {}
