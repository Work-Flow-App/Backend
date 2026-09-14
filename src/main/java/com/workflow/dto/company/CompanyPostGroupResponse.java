package com.workflow.dto.company;

import java.time.LocalDateTime;

public record CompanyPostGroupResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt) {
}