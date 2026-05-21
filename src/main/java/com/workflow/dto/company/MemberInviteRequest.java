package com.workflow.dto.company;

import com.workflow.common.constant.CompanyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberInviteRequest(
        @NotBlank @Email String email,
        @NotNull CompanyRole companyRole
) {}
