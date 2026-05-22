package com.workflow.common.security;

import com.workflow.common.constant.CompanyRole;

public record CompanyContext(Long companyId, Long userId, CompanyRole companyRole) {}
