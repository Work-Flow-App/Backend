package com.workflow.dto.worker;

public record WorkerInviteResponse(
        Long workerId,
        String workerName,
        String email,
        String message
) {}