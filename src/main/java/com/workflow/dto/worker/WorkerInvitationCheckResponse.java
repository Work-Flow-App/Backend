package com.workflow.dto.worker;

import com.workflow.common.constant.InvitationStatus;

import java.time.LocalDateTime;

public record WorkerInvitationCheckResponse(
        boolean valid,
        String email,
        String companyName,
        InvitationStatus status,
        LocalDateTime expiresAt
) {
}
