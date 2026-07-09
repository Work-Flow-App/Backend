package com.workflow.dto.worker;

import java.time.LocalDate;

import com.workflow.common.constant.worker.LeaveType;

public record LeaveCalendarEntryResponse(
        Long workerId,
        String workerName,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate
) {}
