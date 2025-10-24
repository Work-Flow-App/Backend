package com.workflow.service.auth;

import com.workflow.common.constant.Role;
import com.workflow.config.JwtConfigProperties;
import com.workflow.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify role-based JWT token functionality
 */
class JwtRoleTest {

    private JwtService jwtService;
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        JwtConfigProperties jwtConfigProperties = new JwtConfigProperties();
        jwtConfigProperties.getAccessToken().setExpirationMinutes(15);

        jwtService = new JwtService(jwtConfigProperties);
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", TEST_SECRET);
    }

    @Test
    void shouldEmbedAdminRoleInJwtToken() {
        // Given
        User adminUser = User.builder()
                .username("admin")
                .password("password")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        // When
        String token = jwtService.generateToken(adminUser);
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedRole).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void shouldEmbedCompanyRoleInJwtToken() {
        // Given
        User companyUser = User.builder()
                .username("company")
                .password("password")
                .email("company@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();

        // When
        String token = jwtService.generateToken(companyUser);
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedRole).isEqualTo("ROLE_COMPANY");
    }

    @Test
    void shouldEmbedWorkerRoleInJwtToken() {
        // Given
        User workerUser = User.builder()
                .username("worker")
                .password("password")
                .email("worker@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();

        // When
        String token = jwtService.generateToken(workerUser);
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedRole).isEqualTo("ROLE_WORKER");
    }

    @Test
    void shouldExtractUsernameAndRoleFromToken() {
        // Given
        User user = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();

        // When
        String token = jwtService.generateToken(user);
        String extractedUsername = jwtService.extractUserName(token);
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedUsername).isEqualTo("testuser");
        assertThat(extractedRole).isEqualTo("ROLE_COMPANY");
    }

    @Test
    void shouldValidateTokenWithCorrectRole() {
        // Given
        User user = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_WORKER");
    }
}