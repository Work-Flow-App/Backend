package com.workflow.dto.company;

import com.workflow.common.constant.CompanyRole;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record MemberResponse(
        Long memberId,
        Long userId,
        String username,
        String email,
        String name,
        CompanyRole companyRole,
        boolean active,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime joinedAt
) {}
