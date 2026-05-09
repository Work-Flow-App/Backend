package com.workflow.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.dto.auth.LoginRequest;
import com.workflow.dto.auth.SignupRequest;
import com.workflow.entity.auth.User;
import com.workflow.repository.auth.UserRepository;
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
                Role.ADMIN, null
        );

        // When/Then - Should be rejected with validation error
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.role").value("Role must be COMPANY or WORKER"));
    }

    @Test
    void signupShouldCreateUserThatCanLoginWithCompanyRole() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
                "companyuser",
                "company@example.com",
                "password123",
                Role.COMPANY, null
        );

        // Signup — returns 201 + message, user is disabled pending email verification
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        // Simulate email verification by enabling the user directly
        enableUser("companyuser");

        // Login to get JWT token
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("companyuser", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        // Then — verify token contains correct role
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        assertThat(jwtService.extractRole(accessToken)).isEqualTo("ROLE_COMPANY");
    }

    @Test
    void signupShouldCreateUserThatCanLoginWithWorkerRole() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
                "workeruser",
                "worker@example.com",
                "password123",
                Role.WORKER, null
        );

        // Signup
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        // Simulate email verification
        enableUser("workeruser");

        // Login to get JWT token
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("workeruser", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        // Then — verify token contains correct role
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        assertThat(jwtService.extractRole(accessToken)).isEqualTo("ROLE_WORKER");
    }

    @Test
    void loginShouldReturnJwtTokenWithCorrectRole() throws Exception {
        // Given - signup then enable user
        SignupRequest signupRequest = new SignupRequest(
                "testuser",
                "test@example.com",
                "password123",
                Role.COMPANY, null
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        enableUser("testuser");

        // When - Login
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        // Then
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        assertThat(jwtService.extractRole(accessToken)).isEqualTo("ROLE_COMPANY");
        assertThat(jwtService.extractUserName(accessToken)).isEqualTo("testuser");
    }

    @Test
    void refreshTokenShouldReturnNewJwtWithSameRole() throws Exception {
        // Given - signup, enable, login to get tokens
        SignupRequest signupRequest = new SignupRequest(
                "testuser",
                "test@example.com",
                "password123",
                Role.WORKER, null
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        enableUser("testuser");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("refreshToken").asText();

        // When - Use refresh token
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        // Then
        String newAccessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("accessToken").asText();
        assertThat(jwtService.extractRole(newAccessToken)).isEqualTo("ROLE_WORKER");
        assertThat(jwtService.extractUserName(newAccessToken)).isEqualTo("testuser");
    }

    // Helper: simulate email verification by enabling user directly
    private void enableUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setEnabled(true);
        userRepository.save(user);
    }
}
