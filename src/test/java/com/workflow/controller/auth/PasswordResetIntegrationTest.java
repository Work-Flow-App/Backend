package com.workflow.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.dto.auth.password.ForgotPasswordRequest;
import com.workflow.dto.auth.password.ResetPasswordRequest;
import com.workflow.entity.PasswordResetToken;
import com.workflow.entity.RefreshToken;
import com.workflow.entity.User;
import com.workflow.repository.PasswordResetTokenRepository;
import com.workflow.repository.RefreshTokenRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String originalEncodedPassword;

    @BeforeEach
    void setUp() {
        // Clear database
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        originalEncodedPassword = passwordEncoder.encode("oldPassword123");
        testUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("testuser")
                .email("test@example.com")
                .password(originalEncodedPassword)
                .role(Role.WORKER)
                .enabled(true)
                .build();
        userRepository.save(testUser);
    }

    // ============= POST /api/v1/auth/forgot-password Tests =============

    @Test
    void shouldRequestPasswordResetSuccessfully() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        // When/Then
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").value(containsString("verification code has been sent")));

        // Verify token was created
        var tokens = passwordResetTokenRepository.findAll();
        assertEquals(1, tokens.size());
        assertEquals(testUser.getId(), tokens.get(0).getUser().getId());
        assertFalse(tokens.get(0).isUsed());
    }

    @Test
    void shouldReturnSuccessForNonExistentEmail() throws Exception {
        // Given - non-existent email
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        // When/Then - Should return success to prevent user enumeration
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("verification code has been sent")));

        // Verify no token was created
        var tokens = passwordResetTokenRepository.findAll();
        assertEquals(0, tokens.size());
    }

    @Test
    void shouldReturn400ForInvalidEmailFormat() throws Exception {
        // Given - invalid email format
        ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email");

        // When/Then
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.email").value(containsString("Invalid email format")));
    }

    @Test
    void shouldReturn400ForMissingEmail() throws Exception {
        // Given - missing email
        String emptyRequest = "{}";

        // When/Then
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.email").value(containsString("Email is required")));
    }

    @Test
    void shouldCreateMultipleTokensForSameUser() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        // When - Request password reset 3 times
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // Then - Should have 3 tokens
        var tokens = passwordResetTokenRepository.findAll();
        assertEquals(3, tokens.size());
        tokens.forEach(token -> assertEquals(testUser.getId(), token.getUser().getId()));
    }

    @Test
    void shouldDeleteOldestTokenWhenMaxLimitReached() throws Exception {
        // Given - Create 3 tokens (reaching max limit)
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
            Thread.sleep(100); // Ensure different creation times
        }

        // When - Request one more token (should delete oldest)
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then - Should still have 3 tokens (oldest deleted, new one added)
        var tokens = passwordResetTokenRepository.findAll();
        assertEquals(3, tokens.size());
    }

    // ============= POST /api/v1/auth/reset-password Tests =============

    @Test
    void shouldResetPasswordSuccessfully() throws Exception {
        // Given - Create a valid reset token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .verificationCode("123456")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        passwordResetTokenRepository.saveAndFlush(resetToken);

        // Store the old password hash for comparison
        String oldPasswordHash = testUser.getPassword();

        ResetPasswordRequest request = new ResetPasswordRequest(
                "test@example.com",
                "123456",
                "newPassword123"
        );

        // When
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Password has been reset successfully")));

        // Then - Force flush and clear to ensure we're reading from database
        userRepository.flush();

        // Verify password was changed in the database
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        System.out.println("Old password hash: " + oldPasswordHash.substring(0, 20));
        System.out.println("New password hash: " + updatedUser.getPassword().substring(0, 20));
        System.out.println("Password matches 'newPassword123': " + passwordEncoder.matches("newPassword123", updatedUser.getPassword()));

        assertNotEquals(oldPasswordHash, updatedUser.getPassword(), "Password hash should have changed");
        assertTrue(passwordEncoder.matches("newPassword123", updatedUser.getPassword()), "New password should match");

        // Verify token was marked as used
        PasswordResetToken updatedToken = passwordResetTokenRepository.findById(resetToken.getId()).orElseThrow();
        assertTrue(updatedToken.isUsed());
        assertNotNull(updatedToken.getUsedAt());
    }

    @Test
    void shouldReturn400ForInvalidToken() throws Exception {
        // Given - non-existent token
        ResetPasswordRequest request = new ResetPasswordRequest(
                "test@example.com",
                "invalid-code",
                "newPassword123"
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")));
    }

    @Test
    void shouldReturn400ForExpiredToken() throws Exception {
        // Given - expired token
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .verificationCode("111111")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .used(false)
                .createdAt(LocalDateTime.now().minusMinutes(70))
                .build();
        passwordResetTokenRepository.save(expiredToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                "test@example.com",
                "111111",
                "newPassword123"
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Verification code has expired"));

        // Verify password was NOT changed
        User user = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(originalEncodedPassword, user.getPassword());
    }

    @Test
    void shouldReturn400ForUsedToken() throws Exception {
        // Given - already used token
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .verificationCode("222222")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .used(true)
                .createdAt(LocalDateTime.now())
                .usedAt(LocalDateTime.now())
                .build();
        passwordResetTokenRepository.save(usedToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                "test@example.com",
                "222222",
                "newPassword123"
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("already been used")));

        // Verify password was NOT changed
        User user = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(originalEncodedPassword, user.getPassword());
    }

    @Test
    void shouldReturn400ForShortPassword() throws Exception {
        // Given
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .verificationCode("333333")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                "test@example.com",
                "333333",
                "short" // Less than 8 characters
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.newPassword").exists())
                .andExpect(jsonPath("$.validationErrors.newPassword").value(containsString("at least 8 characters")));
    }

    @Test
    void shouldReturn400ForMissingToken() throws Exception {
        // Given - missing email and verification code
        String incompleteRequest = """
                {
                    "newPassword": "newPassword123"
                }
                """;

        // When/Then
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleteRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void shouldNotReuseTokenAfterSuccessfulReset() throws Exception {
        // Given
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .verificationCode("444444")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                "test@example.com",
                "444444",
                "newPassword123"
        );

        // When - First reset (should succeed)
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then - Second reset with same token (should fail)
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already been used")));
    }

    @Test
    void shouldRevokeAllRefreshTokensOnPasswordReset() throws Exception {
        // Given - Create some refresh tokens
        RefreshToken refreshToken1 = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .deviceInfo("Device 1")
                .build();

        RefreshToken refreshToken2 = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .deviceInfo("Device 2")
                .build();

        refreshTokenRepository.save(refreshToken1);
        refreshTokenRepository.save(refreshToken2);

        // Create reset token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .verificationCode("555555")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                "test@example.com",
                "555555",
                "newPassword123"
        );

        // When - Reset password
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then - Verify all refresh tokens were revoked
        RefreshToken updatedToken1 = refreshTokenRepository.findById(refreshToken1.getId()).orElseThrow();
        RefreshToken updatedToken2 = refreshTokenRepository.findById(refreshToken2.getId()).orElseThrow();

        assertTrue(updatedToken1.isRevoked());
        assertTrue(updatedToken2.isRevoked());
    }

    // ============= End-to-End Flow Tests =============

    @Test
    void shouldCompleteFullPasswordResetFlow() throws Exception {
        // Step 1: Request password reset
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest("test@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk());

        // Step 2: Get the token from database (simulating email click)
        PasswordResetToken token = passwordResetTokenRepository.findAll().get(0);
        assertNotNull(token);

        // Step 3: Reset password with token
        ResetPasswordRequest resetRequest = new ResetPasswordRequest(
                "test@example.com",
                token.getVerificationCode(),
                "brandNewPassword456"
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Password has been reset successfully")));

        // Step 4: Verify password was changed and token was used
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("brandNewPassword456", updatedUser.getPassword()));

        PasswordResetToken updatedToken = passwordResetTokenRepository.findById(token.getId()).orElseThrow();
        assertTrue(updatedToken.isUsed());
    }

    // ============= Security Tests =============

    @Test
    void shouldNotExposeUserExistenceOnForgotPassword() throws Exception {
        // Given - existing and non-existing emails
        ForgotPasswordRequest existingEmail = new ForgotPasswordRequest("test@example.com");
        ForgotPasswordRequest nonExistingEmail = new ForgotPasswordRequest("nonexistent@example.com");

        // When/Then - Both should return same response
        String response1 = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingEmail)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistingEmail)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Both should return same generic message
        assertTrue(response1.contains("verification code has been sent"));
        assertTrue(response2.contains("verification code has been sent"));
    }

    @Test
    void shouldEncodeNewPasswordBeforeSaving() throws Exception {
        // Given
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .verificationCode("666666")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                "test@example.com",
                "666666",
                "plainTextPassword"
        );

        // When
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then - Verify password is encoded (BCrypt)
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertNotEquals("plainTextPassword", updatedUser.getPassword());
        assertTrue(updatedUser.getPassword().startsWith("$2a$")); // BCrypt prefix
    }

    @Test
    void shouldHandleSpecialCharactersInPassword() throws Exception {
        // Given
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .verificationCode("777777")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        passwordResetTokenRepository.save(resetToken);

        String passwordWithSpecialChars = "P@ssw0rd!#$%^&*()";
        ResetPasswordRequest request = new ResetPasswordRequest(
                "test@example.com",
                "777777",
                passwordWithSpecialChars
        );

        // When
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then - Verify password works
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(passwordWithSpecialChars, updatedUser.getPassword()));
    }

    @Test
    void shouldRejectEmptyRequestBodies() throws Exception {
        // Test forgot-password
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // Test reset-password
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}