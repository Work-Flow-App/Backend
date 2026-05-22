package com.workflow.dto.company;

import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.InvitationStatus;

import java.time.LocalDateTime;

public record MemberInvitationStatusResponse(
        Long id,
        String email,
        CompanyRole companyRole,
        InvitationStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime usedAt
) {}
