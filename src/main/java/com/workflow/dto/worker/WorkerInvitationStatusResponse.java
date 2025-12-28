package com.workflow.dto.worker;

import java.time.LocalDateTime;

public record WorkerInvitationStatusResponse(
        Long invitationId,
        String email,
        String status,  // "PENDING", "ACCEPTED", "EXPIRED"
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime usedAt
) {}
