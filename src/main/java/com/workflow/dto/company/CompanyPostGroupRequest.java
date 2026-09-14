package com.workflow.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyPostGroupRequest(
        @NotBlank(message = "Group name is required") @Size(max = 100, message = "Group name cannot exceed 100 characters") String name,

        @Size(max = 255, message = "Description cannot exceed 255 characters") String description) {
}