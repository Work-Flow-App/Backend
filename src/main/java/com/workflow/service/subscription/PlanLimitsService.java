package com.workflow.service.subscription;

import com.workflow.common.constant.PlanType;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.config.properties.PlanLimitsProperties;
import com.workflow.entity.company.CompanySubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlanLimitsService implements IPlanLimitsService {

    // Transient stand-in used only when a CompanySubscription lookup comes back empty — never
    // persisted. FREE tier, 0 extras, matches the fail-closed decision for missing-subscription
    // limit-checking (see the Optional-accepting overloads below).
    private static final CompanySubscription FREE_TIER_DEFAULT = CompanySubscription.builder()
            .planType(PlanType.FREE)
            .extraUserSeats(0)
            .extraStorageBlocks(0)
            .build();

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

    @Override
    public int getEffectiveMaxUsers(Optional<CompanySubscription> subscription) {
        return getEffectiveMaxUsers(subscription.orElse(FREE_TIER_DEFAULT));
    }

    @Override
    public int getEffectiveJobsPerMonth(Optional<CompanySubscription> subscription) {
        return getEffectiveJobsPerMonth(subscription.orElse(FREE_TIER_DEFAULT));
    }

    @Override
    public long getEffectiveStorageLimitBytes(Optional<CompanySubscription> subscription) {
        return getEffectiveStorageLimitBytes(subscription.orElse(FREE_TIER_DEFAULT));
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
