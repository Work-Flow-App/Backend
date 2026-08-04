package com.workflow.dto.worker;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.workflow.common.constant.worker.LeaveRequestStatus;
import com.workflow.common.constant.worker.LeaveType;

public record LeaveRequestResponse(
        Long id,
        Long workerId,
        String workerName,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveRequestStatus status,
        String decisionByUsername,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime decisionAt,
        String decisionNote,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime updatedAt
) {}
