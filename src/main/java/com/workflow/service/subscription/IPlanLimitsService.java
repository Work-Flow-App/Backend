package com.workflow.service.subscription;

import com.workflow.entity.company.CompanySubscription;

import java.util.Optional;

public interface IPlanLimitsService {

    int getEffectiveMaxUsers(CompanySubscription subscription);

    int getEffectiveJobsPerMonth(CompanySubscription subscription);

    long getEffectiveStorageLimitBytes(CompanySubscription subscription);

    /**
     * Same as {@link #getEffectiveMaxUsers(CompanySubscription)}, but for callers whose
     * subscription lookup may come back empty. Falls back to FREE-tier limits with 0 extras —
     * this should never legitimately happen (every signup gets a CompanySubscription row), so
     * callers are expected to log loudly when passing an empty Optional here.
     */
    int getEffectiveMaxUsers(Optional<CompanySubscription> subscription);

    int getEffectiveJobsPerMonth(Optional<CompanySubscription> subscription);

    long getEffectiveStorageLimitBytes(Optional<CompanySubscription> subscription);
}
