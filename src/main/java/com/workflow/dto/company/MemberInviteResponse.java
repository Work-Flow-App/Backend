package com.workflow.dto.company;

import com.workflow.common.constant.CompanyRole;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record MemberInviteResponse(
        String email,
        CompanyRole companyRole,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime expiresAt
) {}
