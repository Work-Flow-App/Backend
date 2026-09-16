package com.workflow.dto.company;

import com.workflow.common.constant.PlanType;
import com.workflow.common.constant.SubscriptionStatus;

public record SubscriptionAddonsResponse(
        PlanType planType,
        SubscriptionStatus status,
        int extraUserSeats,
        int extraStorageBlocks
) {}
