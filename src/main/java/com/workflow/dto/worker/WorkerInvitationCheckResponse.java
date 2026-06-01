package com.workflow.dto.worker;

import com.workflow.common.constant.InvitationStatus;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record WorkerInvitationCheckResponse(
        boolean valid,
        String email,
        String companyName,
        InvitationStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime expiresAt
) {
}
