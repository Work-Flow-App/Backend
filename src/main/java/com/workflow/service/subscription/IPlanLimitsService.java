package com.workflow.service.subscription;

import com.workflow.entity.company.CompanySubscription;

public interface IPlanLimitsService {

    int getEffectiveMaxUsers(CompanySubscription subscription);

    int getEffectiveJobsPerMonth(CompanySubscription subscription);

    long getEffectiveStorageLimitBytes(CompanySubscription subscription);
}
