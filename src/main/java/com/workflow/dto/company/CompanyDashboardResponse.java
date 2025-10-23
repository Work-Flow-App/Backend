package com.workflow.dto.company;

public record CompanyDashboardResponse(
        Long companyId,
        String companyName,
        long totalWorkers,
        long totalClients,
        long activeWorkers,
        long archivedWorkers
) {}