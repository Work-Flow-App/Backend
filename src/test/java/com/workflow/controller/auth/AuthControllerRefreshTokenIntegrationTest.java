package com.workflow.controller.auth;

import com.workflow.AbstractControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.dto.auth.AuthenticationResponse;
import com.workflow.dto.auth.LoginRequest;
import com.workflow.dto.auth.LogoutRequest;
import com.workflow.dto.auth.RefreshTokenRequest;
import com.workflow.entity.auth.RefreshToken;
import com.workflow.entity.auth.User;
import com.workflow.repository.auth.RefreshTokenRepository;
import com.workflow.repository.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerRefreshTokenIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .email("test@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        userRepository.save(testUser);
    }

    // ============= Login Tests with Refresh Token =============

    @Test
    void login_ShouldReturnAccessAndRefreshTokens() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists())
                .andReturn();

        // Verify refresh token saved in database
        String responseBody = result.getResponse().getContentAsString();
        AuthenticationResponse response = objectMapper.readValue(responseBody, AuthenticationResponse.class);

        Optional<RefreshToken> savedToken = refreshTokenRepository.findByToken(response.getRefreshToken());
        assertThat(savedToken).isPresent();
        assertThat(savedToken.get().getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedToken.get().isRevoked()).isFalse();
        assertThat(savedToken.get().getDeviceInfo()).isNotNull();
        assertThat(savedToken.get().getIpAddress()).isNotNull();
    }

    // ============= Refresh Token Tests =============

    @Test
    void refreshToken_ShouldReturnNewAccessToken() throws Exception {
        // Arrange - Login first
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        AuthenticationResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(loginResponse.getRefreshToken());

        // Wait 1 second to ensure different JWT iat (issued at) timestamp
        Thread.sleep(1000);

        // Act & Assert
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.accessToken").value(not(loginResponse.getAccessToken())))
                .andReturn();

        AuthenticationResponse refreshResponse = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

        // Verify old refresh token is revoked (token rotation)
        Optional<RefreshToken> oldToken = refreshTokenRepository.findByToken(loginResponse.getRefreshToken());
        assertThat(oldToken).isPresent();
        assertThat(oldToken.get().isRevoked()).isTrue();

        // Verify new refresh token is active
        Optional<RefreshToken> newToken = refreshTokenRepository.findByToken(refreshResponse.getRefreshToken());
        assertThat(newToken).isPresent();
        assertThat(newToken.get().isRevoked()).isFalse();
        assertThat(newToken.get().getLastUsedAt()).isNull(); // Not used yet
    }

    @Test
    void refreshToken_ShouldFail_WhenTokenInvalid() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token-uuid");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void refreshToken_ShouldFail_WhenTokenRevoked() throws Exception {
        // Arrange - Login and logout
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        AuthenticationResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

       // Logout (revoke token) — requires valid access token
        LogoutRequest logoutRequest = new LogoutRequest(loginResponse.getRefreshToken());
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + loginResponse.getAccessToken())
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        // Try to use revoked token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(loginResponse.getRefreshToken());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    @Test
    void refreshToken_ShouldFail_WhenTokenMissing() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ============= Logout Tests =============

    @Test
    void logout_ShouldRevokeRefreshToken() throws Exception {
        // Arrange - Login first
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        AuthenticationResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

        LogoutRequest logoutRequest = new LogoutRequest(loginResponse.getRefreshToken());

        // Act — requires valid access token
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + loginResponse.getAccessToken())
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        // Assert - Token should be revoked
        Optional<RefreshToken> token = refreshTokenRepository.findByToken(loginResponse.getRefreshToken());
        assertThat(token).isPresent();
        assertThat(token.get().isRevoked()).isTrue();
        assertThat(token.get().getRevokedAt()).isNotNull();

        // Try to use revoked token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(loginResponse.getRefreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_ShouldSucceed_EvenWhenTokenNotFound() throws Exception {
        // Login to get a valid access token for the Authorization header
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password123"))))
                .andReturn();
        String accessToken = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthenticationResponse.class
        ).getAccessToken();

        // Attempt logout with a refresh token that doesn't exist — should still return 204
        LogoutRequest request = new LogoutRequest("non-existent-token");
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    // ============= Logout All Devices Tests =============

    @Test
    void logoutAll_ShouldRevokeAllUserTokens() throws Exception {
        // Arrange - Create multiple sessions by logging in multiple times
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        // First login
        MvcResult result1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();
        AuthenticationResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

        // Second login
        MvcResult result2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();
        AuthenticationResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

        // Verify both tokens exist and are active
        assertThat(refreshTokenRepository.findByToken(response1.getRefreshToken())).isPresent();
        assertThat(refreshTokenRepository.findByToken(response2.getRefreshToken())).isPresent();

        // Act - Logout from all devices using one of the access tokens
        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header("Authorization", "Bearer " + response1.getAccessToken()))
                .andExpect(status().isNoContent());

        // Flush and clear to ensure we read fresh data from database
        refreshTokenRepository.flush();

        // Assert - Both tokens should be revoked
        Optional<RefreshToken> token1 = refreshTokenRepository.findByToken(response1.getRefreshToken());
        Optional<RefreshToken> token2 = refreshTokenRepository.findByToken(response2.getRefreshToken());

        assertThat(token1).isPresent();
        assertThat(token1.get().isRevoked()).isTrue();

        assertThat(token2).isPresent();
        assertThat(token2.get().isRevoked()).isTrue();
    }

    @Test
    void logoutAll_ShouldRequireAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout-all"))
                .andExpect(status().isForbidden());
    }

    // ============= Token Rotation Tests =============

    @Test
    void multipleRefreshes_ShouldRotateTokensCorrectly() throws Exception {
        // Arrange - Login
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        AuthenticationResponse response1 = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

        // First refresh
        RefreshTokenRequest refreshRequest1 = new RefreshTokenRequest(response1.getRefreshToken());
        MvcResult refreshResult1 = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest1)))
                .andExpect(status().isOk())
                .andReturn();

        AuthenticationResponse response2 = objectMapper.readValue(
                refreshResult1.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

        // Second refresh with new token
        RefreshTokenRequest refreshRequest2 = new RefreshTokenRequest(response2.getRefreshToken());
        MvcResult refreshResult2 = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest2)))
                .andExpect(status().isOk())
                .andReturn();

        AuthenticationResponse response3 = objectMapper.readValue(
                refreshResult2.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

        // Assert - All old tokens should be revoked
        assertThat(refreshTokenRepository.findByToken(response1.getRefreshToken()).get().isRevoked()).isTrue();
        assertThat(refreshTokenRepository.findByToken(response2.getRefreshToken()).get().isRevoked()).isTrue();
        assertThat(refreshTokenRepository.findByToken(response3.getRefreshToken()).get().isRevoked()).isFalse();

        // Old tokens should not work
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest1)))
                .andExpect(status().isUnauthorized());
    }

    // ============= Device Info Tests =============

    @Test
    void login_ShouldCaptureDeviceInformation() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)")
                        .header("X-Forwarded-For", "203.0.113.1"))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        AuthenticationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

        RefreshToken savedToken = refreshTokenRepository.findByToken(response.getRefreshToken()).get();
        assertThat(savedToken.getDeviceInfo()).contains("iPhone");
        assertThat(savedToken.getIpAddress()).isEqualTo("203.0.113.1");
    }
}