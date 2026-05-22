package com.workflow.dto.company;

import com.workflow.common.constant.CompanyRole;

import java.time.LocalDateTime;

public record MemberInviteResponse(
        String email,
        CompanyRole companyRole,
        String message,
        LocalDateTime expiresAt
) {}
