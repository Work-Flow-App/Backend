package com.workflow.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.dto.auth.LoginRequest;
import com.workflow.dto.auth.SignupRequest;
import com.workflow.repository.UserRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test to verify JWT tokens contain correct roles
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtRoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void signupShouldRejectAdminRoleCreation() throws Exception {
        // Given - ADMIN role should not be allowed during signup (security rule)
        SignupRequest request = new SignupRequest(
                "adminuser",
                "admin@example.com",
                "password123",
                Role.ADMIN
        );

        // When/Then - Should be rejected with validation error
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.role").value("Role must be COMPANY or WORKER"));
    }

    @Test
    void signupShouldReturnJwtTokenWithCompanyRole() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
                "companyuser",
                "company@example.com",
                "password123",
                Role.COMPANY
        );

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        // Then
        String responseJson = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson).get("accessToken").asText();

        String extractedRole = jwtService.extractRole(accessToken);
        assertThat(extractedRole).isEqualTo("ROLE_COMPANY");
    }

    @Test
    void signupShouldReturnJwtTokenWithWorkerRole() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
                "workeruser",
                "worker@example.com",
                "password123",
                Role.WORKER
        );

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        // Then
        String responseJson = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson).get("accessToken").asText();

        String extractedRole = jwtService.extractRole(accessToken);
        assertThat(extractedRole).isEqualTo("ROLE_WORKER");
    }

    @Test
    void loginShouldReturnJwtTokenWithCorrectRole() throws Exception {
        // Given - Create user via signup
        SignupRequest signupRequest = new SignupRequest(
                "testuser",
                "test@example.com",
                "password123",
                Role.COMPANY
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // When - Login with the same user
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        // Then - Verify token contains correct role
        String responseJson = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson).get("accessToken").asText();

        String extractedRole = jwtService.extractRole(accessToken);
        assertThat(extractedRole).isEqualTo("ROLE_COMPANY");

        String extractedUsername = jwtService.extractUserName(accessToken);
        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    void refreshTokenShouldReturnNewJwtWithSameRole() throws Exception {
        // Given - Signup to get initial tokens
        SignupRequest signupRequest = new SignupRequest(
                "testuser",
                "test@example.com",
                "password123",
                Role.WORKER
        );

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String signupJson = signupResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(signupJson).get("refreshToken").asText();

        // When - Use refresh token to get new access token
        String refreshRequestBody = String.format("{\"refreshToken\":\"%s\"}", refreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        // Then - Verify new token has same role
        String refreshJson = refreshResult.getResponse().getContentAsString();
        String newAccessToken = objectMapper.readTree(refreshJson).get("accessToken").asText();

        String extractedRole = jwtService.extractRole(newAccessToken);
        assertThat(extractedRole).isEqualTo("ROLE_WORKER");

        String extractedUsername = jwtService.extractUserName(newAccessToken);
        assertThat(extractedUsername).isEqualTo("testuser");
    }
}