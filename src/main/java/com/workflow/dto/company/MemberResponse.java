package com.workflow.dto.company;

import com.workflow.common.constant.CompanyRole;

import java.time.LocalDateTime;

public record MemberResponse(
        Long memberId,
        Long userId,
        String username,
        String email,
        String name,
        CompanyRole companyRole,
        boolean active,
        LocalDateTime joinedAt
) {}
