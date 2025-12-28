package com.workflow.dto.worker;

public record WorkerSignupResponse(
        Long workerId,
        String name,
        String email,
        String username,
        String companyName,
        String message
) {}
