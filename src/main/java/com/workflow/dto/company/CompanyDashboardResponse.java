package com.workflow.dto.company;

/**
 * @param usageSummary Usage against the company's effective plan limits. Computed against
 *                      FREE-tier defaults (not omitted/null) if the company has no
 *                      CompanySubscription record — that's a backend data-integrity anomaly
 *                      that should never legitimately happen (every signup creates one), not a
 *                      normal state, and is logged loudly server-side when it occurs.
 */
public record CompanyDashboardResponse(
        Long companyId,
        String companyName,
        long totalWorkers,
        long totalClients,
        long activeWorkers,
        long archivedWorkers,
        UsageSummaryResponse usageSummary
) {}