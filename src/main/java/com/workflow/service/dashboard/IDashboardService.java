package com.workflow.service.dashboard;

import com.workflow.dto.dashboard.FinancialSummaryResponse;

public interface IDashboardService {
    FinancialSummaryResponse getFinancialSummary(Long companyId);
}
