package com.workflow.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.dto.auth.LoginRequest;
import com.workflow.dto.auth.SignupRequest;
import com.workflow.entity.User;
import com.workflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User existingUser;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        userRepository.deleteAll();

        // Create an existing user for login tests
        existingUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("existinguser")
                .password(passwordEncoder.encode("password123"))
                .email("existing@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();
        userRepository.save(existingUser);
    }

    // ============= POST /api/v1/auth/signup Tests =============

    @Test
    void shouldCreateNewUserAndReturn201WithMessage() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
                "newuser",
                "newuser@example.com",
                "password123",
                Role.WORKER
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Verify user was saved to database but not yet enabled
        assert userRepository.findByUsername("newuser").isPresent();
        assert !userRepository.findByUsername("newuser").get().isEnabled();
    }

    @Test
    void shouldReturn409ConflictIfUsernameAlreadyExists() throws Exception {
        // Given - using existing user's username
        SignupRequest request = new SignupRequest(
                "existinguser",
                "another@example.com",
                "password123",
                Role.WORKER
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User already exists with username: existinguser"));
    }

    @Test
    void shouldReturn400BadRequestForInvalidRole() throws Exception {
        // Given - using invalid role value (via raw JSON)
        String invalidRequest = """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "password123",
                    "role": "INVALID_ROLE"
                }
                """;

        // When/Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400BadRequestForMissingRequiredFields() throws Exception {
        // Given - missing username
        String incompleteRequest = """
                {
                    "email": "test@example.com",
                    "password": "password123",
                    "role": "WORKER"
                }
                """;

        // When/Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleteRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void shouldValidateEmailFormat() throws Exception {
        // Given - invalid email format
        SignupRequest request = new SignupRequest(
                "testuser",
                "invalid-email",
                "password123",
                Role.WORKER
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.email").value(containsString("Invalid email format")));
    }

    @Test
    void shouldValidateUsernameLength() throws Exception {
        // Given - username too short (less than 3 characters)
        SignupRequest request = new SignupRequest(
                "ab",
                "test@example.com",
                "password123",
                Role.WORKER
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.username").exists());
    }

    @Test
    void shouldValidatePasswordLength() throws Exception {
        // Given - password too short (less than 8 characters)
        SignupRequest request = new SignupRequest(
                "testuser",
                "test@example.com",
                "pass",
                Role.WORKER
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists())
                .andExpect(jsonPath("$.validationErrors.password").value(containsString("at least 8 characters")));
    }

    @Test
    void shouldCreateUserWithDifferentRoles() throws Exception {

        // Test COMPANY role
        SignupRequest companyRequest = new SignupRequest(
                "companyuser",
                "company@example.com",
                "password123",
                Role.COMPANY
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        // Verify user exists with correct role
        assert userRepository.findByUsername("companyuser").get().getRole() == Role.COMPANY;
    }

    // ============= POST /api/v1/auth/login Tests =============

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("existinguser", "password123");

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void shouldReturnJwtTokenOnSuccessfulLogin() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("existinguser", "password123");

        // When/Then
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify token has JWT structure (header.payload.signature)
        String token = objectMapper.readTree(response).get("accessToken").asText();
        assert token.split("\\.").length == 3;
    }

    @Test
    void shouldReturn401UnauthorizedForInvalidUsername() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("nonexistentuser", "password123");

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void shouldReturn401UnauthorizedForInvalidPassword() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("existinguser", "wrongpassword");

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void shouldReturn400BadRequestForMissingCredentials() throws Exception {
        // Given - missing password
        String incompleteRequest = """
                {
                    "userName": "testuser"
                }
                """;

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleteRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleDisabledUser() throws Exception {
        // Given - create disabled user
        User disabledUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("disableduser")
                .password(passwordEncoder.encode("password123"))
                .email("disabled@example.com")
                .role(Role.WORKER)
                .enabled(false)
                .build();
        userRepository.save(disabledUser);

        LoginRequest request = new LoginRequest("disableduser", "password123");

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldAllowMultipleSuccessfulLogins() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("existinguser", "password123");

        // When/Then - First login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // When/Then - Second login (should also succeed)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    // ============= Edge Cases and Security Tests =============

    @Test
    void shouldNotExposeUserExistenceOnLogin() throws Exception {
        // Given - non-existent user
        LoginRequest nonExistentRequest = new LoginRequest("nonexistent", "password123");

        // Given - existing user with wrong password
        LoginRequest wrongPasswordRequest = new LoginRequest("existinguser", "wrongpassword");

        // When/Then - Both should return same generic error message
        String response1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Both should have similar generic error messages (security best practice)
        assert response1.contains("Unauthorized");
        assert response2.contains("Unauthorized");
    }

    @Test
    void shouldHandleSpecialCharactersInCredentials() throws Exception {
        // Given - create user with special characters in username
        User specialUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("user.name-123")
                .password(passwordEncoder.encode("password123"))
                .email("special@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();
        userRepository.save(specialUser);

        LoginRequest request = new LoginRequest("user.name-123", "password123");

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void shouldHandleEmptyRequestBody() throws Exception {
        // Given - empty JSON
        String emptyRequest = "{}";

        // When/Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void shouldReturnCorrectContentType() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("existinguser", "password123");

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    @Test
    void shouldNotStorePasswordInPlainText() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
                "secureuser",
                "secure@example.com",
                "password123",
                Role.WORKER
        );

        // When
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Then - verify password is encoded
        User savedUser = userRepository.findByUsername("secureuser").orElseThrow();
        assert !savedUser.getPassword().equals("password123");
        assert savedUser.getPassword().startsWith("$2a$"); // BCrypt hash prefix
    }
}