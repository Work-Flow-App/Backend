package com.workflow.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record GenerateCheckoutLinkRequest(
        List<CheckoutItem> items,
        @JsonProperty("customer_id") String customerId,
        @JsonProperty("custom_data") Map<String, String> customData,
        @JsonProperty("success_url") String successUrl,
        @JsonProperty("cancel_url") String cancelUrl
) {

    public record CheckoutItem(
            @JsonProperty("price_id") String priceId,
            int quantity
    ) {}
}