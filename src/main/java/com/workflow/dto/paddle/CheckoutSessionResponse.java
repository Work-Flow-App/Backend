package com.workflow.dto.paddle;

public record CheckoutSessionResponse(CheckoutData data) {

    public record CheckoutData(String id, String url) {}
}