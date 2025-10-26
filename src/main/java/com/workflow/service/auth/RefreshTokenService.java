package com.workflow.service.auth;

import com.workflow.common.exception.customException.InvalidRefreshTokenException;
import com.workflow.config.properties.JwtConfigProperties;
import com.workflow.entity.RefreshToken;
import com.workflow.entity.User;
import com.workflow.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfigProperties jwtConfigProperties;

    /**
     * Create a new refresh token for a user
     */
    @Transactional
    public RefreshToken createRefreshToken(User user, HttpServletRequest request) {
        // Check if user has too many active tokens
        long activeTokenCount = refreshTokenRepository.countActiveTokensByUser(user, LocalDateTime.now());
        if (activeTokenCount >= jwtConfigProperties.getRefreshToken().getMaxActiveTokens()) {
            // Revoke oldest token
            List<RefreshToken> userTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
            userTokens.stream()
                    .min((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()))
                    .ifPresent(oldestToken -> {
                        oldestToken.revoke();
                        refreshTokenRepository.save(oldestToken);
                        log.info("Revoked oldest token for user {} due to max active tokens limit", user.getUsername());
                    });
        }

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(jwtConfigProperties.getRefreshToken().getExpirationDays()))
                .deviceInfo(extractDeviceInfo(request))
                .ipAddress(extractIpAddress(request))
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for user {} from IP {}", user.getUsername(), savedToken.getIpAddress());
        return savedToken;
    }

    /**
     * Validate and return refresh token
     */
    @Transactional
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            if (refreshToken.isRevoked()) {
                log.warn("Attempted to use revoked refresh token for user {}", refreshToken.getUser().getUsername());
                throw new InvalidRefreshTokenException("Refresh token has been revoked");
            }
            if (refreshToken.isExpired()) {
                log.warn("Attempted to use expired refresh token for user {}", refreshToken.getUser().getUsername());
                throw new InvalidRefreshTokenException("Refresh token has expired");
            }
        }

        // Update last used timestamp
        refreshToken.updateLastUsed();
        refreshTokenRepository.save(refreshToken);

        log.debug("Validated refresh token for user {}", refreshToken.getUser().getUsername());
        return refreshToken;
    }

    /**
     * Rotate refresh token (create new one, revoke old one)
     */
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken, HttpServletRequest request) {
        // Revoke old token
        oldToken.revoke();
        refreshTokenRepository.save(oldToken);

        // Create new token
        RefreshToken newToken = createRefreshToken(oldToken.getUser(), request);
        log.info("Rotated refresh token for user {}", oldToken.getUser().getUsername());
        return newToken;
    }

    /**
     * Revoke a specific refresh token
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
            log.info("Revoked refresh token for user {}", refreshToken.getUser().getUsername());
        });
    }

    /**
     * Revoke all user's refresh tokens (logout from all devices)
     */
    @Transactional
    public int revokeAllUserTokens(User user) {
        int revokedCount = refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());
        log.info("Revoked {} refresh tokens for user {}", revokedCount, user.getUsername());
        return revokedCount;
    }

    /**
     * Get all active sessions for a user
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> getUserActiveSessions(User user) {
        return refreshTokenRepository.findByUserAndRevokedFalse(user);
    }

    /**
     * Cleanup expired and revoked tokens (scheduled job)
     */
    @Transactional
    public int cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
        log.info("Cleaned up {} expired/revoked refresh tokens", deleted);
        return deleted;
    }

    // Helper methods
    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 500)) : "Unknown";
    }

    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}