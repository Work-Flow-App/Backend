package com.workflow.dto.admin;

import com.workflow.common.constant.job.JobStatus;

import java.time.LocalDateTime;

public record AdminJobResponse(
        Long id,
        String templateName,
        JobStatus status,
        Long companyId,
        LocalDateTime createdAt
) {}
