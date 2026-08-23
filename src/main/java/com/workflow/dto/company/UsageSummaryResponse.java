package com.workflow.dto.company;

public record UsageSummaryResponse(
        long jobsUsedThisMonth,
        int jobsLimit,
        boolean jobsWarningThresholdReached,
        long storageUsedBytes,
        long storageLimitBytes,
        boolean storageWarningThresholdReached,
        long activeWorkers,
        int seatsLimit,
        boolean seatsWarningThresholdReached
) {}
