package com.workflow.dto.worker;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkerWeeklyHoursResponse(
        Long workerId,
        LocalDate weekStart,
        LocalDate weekEnd,
        BigDecimal totalHours,
        BigDecimal regularHours,
        BigDecimal overtimeHours,
        // Null when the worker has no hourlyRate/overtimeRate set — an unset rate means
        // "unknown," not "free," so this deliberately isn't defaulted to zero.
        BigDecimal regularPay,
        BigDecimal overtimePay,
        BigDecimal totalPay,
        boolean hasOpenVisit
) {}
