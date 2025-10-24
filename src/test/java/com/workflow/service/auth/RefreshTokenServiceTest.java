package com.workflow.service.auth;

import com.workflow.common.constant.Role;
import com.workflow.common.exception.customException.InvalidRefreshTokenException;
import com.workflow.config.JwtConfigProperties;
import com.workflow.entity.RefreshToken;
import com.workflow.entity.User;
import com.workflow.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private HttpServletRequest httpRequest;

    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uuid("test-uuid")
                .username("testuser")
                .email("test@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();

        // Create JwtConfigProperties with test values
        JwtConfigProperties jwtConfigProperties = new JwtConfigProperties();
        jwtConfigProperties.getRefreshToken().setExpirationDays(30);
        jwtConfigProperties.getRefreshToken().setMaxActiveTokens(5);

        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtConfigProperties);
    }

    // ============= createRefreshToken Tests =============

    @Test
    void createRefreshToken_ShouldCreateTokenSuccessfully() {
        // Arrange
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Chrome");
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(refreshTokenRepository.countActiveTokensByUser(any(), any())).thenReturn(0L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RefreshToken token = refreshTokenService.createRefreshToken(testUser, httpRequest);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotEmpty();
        assertThat(token.getUser()).isEqualTo(testUser);
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(token.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(31));
        assertThat(token.getDeviceInfo()).isEqualTo("Mozilla/5.0 Chrome");
        assertThat(token.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(token.isRevoked()).isFalse();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_ShouldRevokeOldestToken_WhenMaxTokensReached() {
        // Arrange
        RefreshToken oldestToken = RefreshToken.builder()
                .id(1L)
                .token("oldest-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now().minusDays(10))
                .revoked(false)
                .build();

        RefreshToken newerToken = RefreshToken.builder()
                .id(2L)
                .token("newer-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now().minusDays(5))
                .revoked(false)
                .build();

        List<RefreshToken> userTokens = List.of(oldestToken, newerToken);

        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(refreshTokenRepository.countActiveTokensByUser(any(), any())).thenReturn(5L);
        when(refreshTokenRepository.findByUserAndRevokedFalse(testUser)).thenReturn(userTokens);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RefreshToken newToken = refreshTokenService.createRefreshToken(testUser, httpRequest);

        // Assert
        assertThat(newToken).isNotNull();
        assertThat(oldestToken.isRevoked()).isTrue();
        assertThat(oldestToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // Once for revoke, once for new token
    }

    @Test
    void createRefreshToken_ShouldHandleXForwardedFor() {
        // Arrange
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(refreshTokenRepository.countActiveTokensByUser(any(), any())).thenReturn(0L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RefreshToken token = refreshTokenService.createRefreshToken(testUser, httpRequest);

        // Assert
        assertThat(token.getIpAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    void createRefreshToken_ShouldHandleNullUserAgent() {
        // Arrange
        when(httpRequest.getHeader("User-Agent")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(refreshTokenRepository.countActiveTokensByUser(any(), any())).thenReturn(0L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RefreshToken token = refreshTokenService.createRefreshToken(testUser, httpRequest);

        // Assert
        assertThat(token.getDeviceInfo()).isEqualTo("Unknown");
    }

    // ============= validateRefreshToken Tests =============

    @Test
    void validateRefreshToken_ShouldValidateAndUpdateLastUsed() {
        // Arrange
        RefreshToken validToken = RefreshToken.builder()
                .id(1L)
                .token("valid-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(validToken));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RefreshToken result = refreshTokenService.validateRefreshToken("valid-token");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLastUsedAt()).isNotNull();
        verify(refreshTokenRepository).save(validToken);
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenNotFound() {
        // Arrange
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("invalid-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenRevoked() {
        // Arrange
        RefreshToken revokedToken = RefreshToken.builder()
                .token("revoked-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(true)
                .revokedAt(LocalDateTime.now())
                .build();

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revokedToken));

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("revoked-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenExpired() {
        // Arrange
        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("expired");
    }

    // ============= rotateRefreshToken Tests =============

    @Test
    void rotateRefreshToken_ShouldRevokeOldAndCreateNew() {
        // Arrange
        RefreshToken oldToken = RefreshToken.builder()
                .id(1L)
                .token("old-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(15))
                .revoked(false)
                .build();

        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.countActiveTokensByUser(any(), any())).thenReturn(0L);

        // Act
        RefreshToken newToken = refreshTokenService.rotateRefreshToken(oldToken, httpRequest);

        // Assert
        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(oldToken.getRevokedAt()).isNotNull();
        assertThat(newToken.getToken()).isNotEqualTo(oldToken.getToken());
        assertThat(newToken.getUser()).isEqualTo(testUser);
        verify(refreshTokenRepository, times(2)).save(any()); // Once for revoke, once for new token
    }

    // ============= revokeRefreshToken Tests =============

    @Test
    void revokeRefreshToken_ShouldRevokeToken() {
        // Arrange
        RefreshToken token = RefreshToken.builder()
                .token("token-to-revoke")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        refreshTokenService.revokeRefreshToken("token-to-revoke");

        // Assert
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revokeRefreshToken_ShouldDoNothing_WhenTokenNotFound() {
        // Arrange
        when(refreshTokenRepository.findByToken("non-existent")).thenReturn(Optional.empty());

        // Act
        refreshTokenService.revokeRefreshToken("non-existent");

        // Assert
        verify(refreshTokenRepository, never()).save(any());
    }

    // ============= revokeAllUserTokens Tests =============

    @Test
    void revokeAllUserTokens_ShouldRevokeAllTokens() {
        // Arrange
        when(refreshTokenRepository.revokeAllUserTokens(eq(testUser), any())).thenReturn(3);

        // Act
        int revokedCount = refreshTokenService.revokeAllUserTokens(testUser);

        // Assert
        assertThat(revokedCount).isEqualTo(3);
        verify(refreshTokenRepository).revokeAllUserTokens(eq(testUser), any(LocalDateTime.class));
    }

    // ============= getUserActiveSessions Tests =============

    @Test
    void getUserActiveSessions_ShouldReturnActiveSessions() {
        // Arrange
        List<RefreshToken> activeSessions = new ArrayList<>();
        activeSessions.add(RefreshToken.builder().token("token1").user(testUser).build());
        activeSessions.add(RefreshToken.builder().token("token2").user(testUser).build());

        when(refreshTokenRepository.findByUserAndRevokedFalse(testUser)).thenReturn(activeSessions);

        // Act
        List<RefreshToken> result = refreshTokenService.getUserActiveSessions(testUser);

        // Assert
        assertThat(result).hasSize(2);
        verify(refreshTokenRepository).findByUserAndRevokedFalse(testUser);
    }

    // ============= cleanupExpiredTokens Tests =============

    @Test
    void cleanupExpiredTokens_ShouldDeleteExpiredTokens() {
        // Arrange
        when(refreshTokenRepository.deleteExpiredAndRevokedTokens(any())).thenReturn(10);

        // Act
        int deleted = refreshTokenService.cleanupExpiredTokens();

        // Assert
        assertThat(deleted).isEqualTo(10);
        verify(refreshTokenRepository).deleteExpiredAndRevokedTokens(any(LocalDateTime.class));
    }
}