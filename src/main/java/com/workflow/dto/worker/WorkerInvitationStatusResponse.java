package com.workflow.dto.worker;

import com.workflow.common.constant.InvitationStatus;

import java.time.LocalDateTime;

public record WorkerInvitationStatusResponse(
        Long invitationId,
        String email,
        String token,
        InvitationStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime usedAt
) {}
