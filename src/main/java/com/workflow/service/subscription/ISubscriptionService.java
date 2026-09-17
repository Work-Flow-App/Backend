package com.workflow.service.subscription;

import com.workflow.common.constant.PlanType;
import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.entity.company.CompanySubscription;

public interface ISubscriptionService {

    void initTrial(Long companyId);

    CompanySubscription getStatus(Long companyId);

    CheckoutResult createCheckoutSession(Long companyId, PlanType planType, int extraSeats, int extraStorageBlocks);

    String getPortalUrl(Long companyId);

    void cancelSubscription(Long companyId);

    /**
     * Adjusts extra seats/storage blocks on an already-active Paddle subscription. Values are
     * ABSOLUTE ("set to N"), not incremental; null means "leave this field unchanged" — see
     * UpdateSubscriptionAddonsRequest javadoc for why null and 0 are deliberately distinct here.
     */
    SubscriptionAddonsResult updateAddons(Long companyId, Integer extraSeats, Integer extraStorageBlocks);

    record CheckoutResult(String transactionId, String checkoutUrl) {}

    record SubscriptionAddonsResult(
            PlanType planType, SubscriptionStatus status, int extraUserSeats, int extraStorageBlocks) {}
}
