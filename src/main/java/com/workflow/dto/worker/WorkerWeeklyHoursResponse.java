package com.workflow.dto.worker;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkerWeeklyHoursResponse(
        Long workerId,
        LocalDate weekStart,
        LocalDate weekEnd,
        BigDecimal totalHours,
        boolean hasOpenVisit
) {}
