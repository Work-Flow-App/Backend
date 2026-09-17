package com.workflow.service.paddle;

import com.workflow.common.constant.PlanType;
import com.workflow.dto.paddle.CheckoutSessionResponse;
import com.workflow.dto.paddle.PaddleCustomerResponse;
import com.workflow.dto.paddle.PaddlePortalSessionResponse;
import com.workflow.dto.paddle.PaddleSubscriptionResponse;

public interface IPaddleService {

    PaddleCustomerResponse createCustomer(String email, String name);

    CheckoutSessionResponse generateCheckoutUrl(
            String paddleCustomerId, Long companyId, PlanType planType, int extraSeats, int extraStorageBlocks);

    /**
     * Updates the line items on an already-active Paddle subscription (PATCH /subscriptions/{id}) —
     * used to add/adjust extra seats or storage blocks after initial checkout. Unlike
     * generateCheckoutUrl (which creates a brand-new transaction), this call applies synchronously
     * against the customer's existing saved payment method; Paddle bills the prorated difference
     * immediately and returns the updated subscription in the same response.
     */
    PaddleSubscriptionResponse updateSubscriptionAddons(
            String paddleSubscriptionId, PlanType planType, int extraSeats, int extraStorageBlocks);

    PaddleSubscriptionResponse getSubscription(String subscriptionId);

    void cancelSubscription(String subscriptionId);

    PaddlePortalSessionResponse generatePortalUrl(String paddleCustomerId);
}
