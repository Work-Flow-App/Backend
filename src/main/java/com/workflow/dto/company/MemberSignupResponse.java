package com.workflow.dto.company;

import com.workflow.common.constant.CompanyRole;

public record MemberSignupResponse(
        Long memberId,
        String username,
        String email,
        String companyName,
        CompanyRole companyRole,
        String message
) {}
