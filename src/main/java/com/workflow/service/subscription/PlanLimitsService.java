package com.workflow.service.subscription;

import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.config.properties.PlanLimitsProperties;
import com.workflow.entity.company.CompanySubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanLimitsService implements IPlanLimitsService {

    private final PlanLimitsProperties planLimits;

    @Override
    public int getEffectiveMaxUsers(CompanySubscription subscription) {
        return tierFor(subscription).getMaxUsers() + subscription.getExtraUserSeats();
    }

    @Override
    public int getEffectiveJobsPerMonth(CompanySubscription subscription) {
        return tierFor(subscription).getJobsPerMonth();
    }

    @Override
    public long getEffectiveStorageLimitBytes(CompanySubscription subscription) {
        PlanLimitsProperties.Tier tier = tierFor(subscription);
        return tier.getStorageLimitBytes()
                + (subscription.getExtraStorageBlocks() * tier.getStorageOverageBlockBytes());
    }

    private PlanLimitsProperties.Tier tierFor(CompanySubscription subscription) {
        if (subscription == null) {
            throw new InvalidRequestException("CompanySubscription must not be null");
        }
        return switch (subscription.getPlanType()) {
            case FREE -> planLimits.getFree();
            case STARTER -> planLimits.getStarter();
            case PROFESSIONAL -> planLimits.getProfessional();
        };
    }
}
