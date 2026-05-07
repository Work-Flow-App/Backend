package com.workflow.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaddleCustomerResponse(CustomerData data) {

    public record CustomerData(
            String id,
            String email,
            String name
    ) {}
}