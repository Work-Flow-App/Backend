package com.workflow.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Body for PATCH /subscriptions/{id} — updates the line items on an already-active Paddle
 * subscription. Paddle replaces the subscription's entire item set with what's sent here, so
 * callers must always include every item that should remain (base plan + any addons with
 * quantity > 0), not just the ones that changed. Reuses GenerateCheckoutLinkRequest.CheckoutItem
 * (price_id + quantity) rather than duplicating an identical shape.
 */
public record PaddleSubscriptionUpdateRequest(
        List<GenerateCheckoutLinkRequest.CheckoutItem> items,
        @JsonProperty("proration_billing_mode") String prorationBillingMode
) {}
