package com.workflow.dto.auth;

public record LoginRequest(
        String userName,
        String password) {
}
