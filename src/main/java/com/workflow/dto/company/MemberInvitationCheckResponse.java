package com.workflow.dto.company;

import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.InvitationStatus;

import java.time.LocalDateTime;

public record MemberInvitationCheckResponse(
        boolean valid,
        String email,
        String companyName,
        CompanyRole companyRole,
        InvitationStatus status,
        LocalDateTime expiresAt
) {}
