package com.workflow.service.subscription;

import com.workflow.common.constant.PlanType;
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

    /**
     * Same as {@link #getEffectiveMaxUsers(CompanySubscription)}, but for callers evaluating a
     * candidate extraSeats value that has not been persisted yet (e.g. SubscriptionService.updateAddons
     * validating a requested decrease against current usage before committing it). Avoids the need to
     * mutate a managed CompanySubscription entity's fields just to run a "what-if" check.
     */
    int getEffectiveMaxUsers(PlanType planType, int extraSeats);

    /**
     * Same as {@link #getEffectiveStorageLimitBytes(CompanySubscription)}, but for a candidate
     * extraStorageBlocks value not yet persisted — see {@link #getEffectiveMaxUsers(PlanType, int)}.
     */
    long getEffectiveStorageLimitBytes(PlanType planType, int extraStorageBlocks);
}
