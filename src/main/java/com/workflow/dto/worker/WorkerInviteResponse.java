package com.workflow.dto.worker;

import java.time.LocalDateTime;

public record WorkerInviteResponse(
        String email,
        String message,
        LocalDateTime expiresAt
) {}