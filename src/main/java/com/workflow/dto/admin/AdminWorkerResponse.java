package com.workflow.dto.admin;

import java.time.LocalDateTime;

public record AdminWorkerResponse(
        Long id,
        String name,
        String email,
        Long companyId,
        LocalDateTime createdAt
) {}
