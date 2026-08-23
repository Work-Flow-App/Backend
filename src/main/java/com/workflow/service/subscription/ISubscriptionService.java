package com.workflow.service.subscription;

import com.workflow.common.constant.PlanType;
import com.workflow.entity.company.CompanySubscription;

public interface ISubscriptionService {

    void initTrial(Long companyId);

    CompanySubscription getStatus(Long companyId);

    CheckoutResult createCheckoutSession(Long companyId, PlanType planType, int extraSeats, int extraStorageBlocks);

    String getPortalUrl(Long companyId);

    void cancelSubscription(Long companyId);

    record CheckoutResult(String transactionId, String checkoutUrl) {}
}
