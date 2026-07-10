package com.workflow.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompanyPostCreateRequest(
        @NotBlank(message = "Post content cannot be empty")
        String content,
        @NotNull
        Boolean isPublic
) {}