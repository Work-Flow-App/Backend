package com.workflow.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaddleSubscriptionResponse(SubscriptionData data) {

    public record SubscriptionData(
            String id,
            String status,
            @JsonProperty("customer_id") String customerId,
            @JsonProperty("current_billing_period") BillingPeriod currentBillingPeriod
    ) {}

    public record BillingPeriod(
            @JsonProperty("starts_at") String startsAt,
            @JsonProperty("ends_at") String endsAt
    ) {}
}