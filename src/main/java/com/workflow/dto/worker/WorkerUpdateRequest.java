package com.workflow.dto.worker;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkerUpdateRequest(
        @NotBlank(message = "Worker name is required")
        @Size(min = 2, max = 100, message = "Worker name must be between 2 and 100 characters")
        String name,

        @Size(max = 10, message = "Initials cannot exceed 10 characters")
        String initials,

        @Size(max = 20, message = "Telephone cannot exceed 20 characters")
        String telephone,

        @Size(max = 20, message = "Mobile cannot exceed 20 characters")
        String mobile,

        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        String email
) {}