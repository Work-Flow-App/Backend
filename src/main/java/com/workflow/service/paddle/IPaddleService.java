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

    PaddleSubscriptionResponse getSubscription(String subscriptionId);

    void cancelSubscription(String subscriptionId);

    PaddlePortalSessionResponse generatePortalUrl(String paddleCustomerId);
}
