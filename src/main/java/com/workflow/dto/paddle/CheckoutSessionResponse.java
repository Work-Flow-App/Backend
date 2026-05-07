package com.workflow.dto.paddle;

public record CheckoutSessionResponse(CheckoutData data) {

    public record CheckoutData(String id, Checkout checkout) {}

    public record Checkout(String url) {}
}