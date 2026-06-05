package com.workflow.dto.worker;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record WorkerInviteResponse(
        String email,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime expiresAt
) {}