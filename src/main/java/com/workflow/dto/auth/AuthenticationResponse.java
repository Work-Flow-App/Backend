package com.workflow.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn; // seconds
    private String errorMessage;

    public static AuthenticationResponse error(String errorMessage) {
        return AuthenticationResponse.builder()
                .errorMessage(errorMessage)
                .build();
    }

    public static AuthenticationResponse success(String accessToken, String refreshToken) {
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(60 * 60L) // 1 hour in seconds (will be configurable)
                .build();
    }
}