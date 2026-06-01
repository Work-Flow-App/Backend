package com.workflow.dto.company;

import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.InvitationStatus;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record MemberInvitationCheckResponse(
        boolean valid,
        String email,
        String companyName,
        CompanyRole companyRole,
        InvitationStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime expiresAt
) {}
