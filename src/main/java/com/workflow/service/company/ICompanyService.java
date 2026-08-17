package com.workflow.service.company;

import com.workflow.dto.company.CompanyDashboardResponse;
import com.workflow.dto.company.CompanyProfileResponse;
import com.workflow.dto.company.CompanyProfileUpdateRequest;
import com.workflow.dto.company.UsageSummaryResponse;
import com.workflow.entity.company.Company;

public interface ICompanyService {
    CompanyProfileResponse updateProfile(CompanyProfileUpdateRequest request, Long userId);
    CompanyProfileResponse getProfile(Long userId);
    CompanyDashboardResponse getDashboard(Long userId);
    UsageSummaryResponse getUsageSummary(Long userId);
    Company findCompanyByUserId(Long userId);
}