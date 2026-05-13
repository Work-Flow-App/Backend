package com.workflow.dto.worker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkerPasswordResetRequest(
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword,

        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        String newUsername
) {}