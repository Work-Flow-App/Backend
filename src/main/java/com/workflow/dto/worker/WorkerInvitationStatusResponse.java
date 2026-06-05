package com.workflow.dto.worker;

import com.workflow.common.constant.InvitationStatus;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record WorkerInvitationStatusResponse(
        Long invitationId,
        String email,
        InvitationStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime expiresAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime usedAt
) {}
