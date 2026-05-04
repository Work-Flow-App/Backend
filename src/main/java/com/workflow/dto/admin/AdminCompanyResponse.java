package com.workflow.dto.admin;

import java.time.LocalDateTime;

public record AdminCompanyResponse(
        Long id,
        String name,
        String email,
        LocalDateTime createdAt
) {}
