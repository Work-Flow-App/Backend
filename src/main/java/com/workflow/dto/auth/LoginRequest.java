package com.workflow.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        String userName,

        @NotBlank(message = "Password is required")
        String password) {
}
