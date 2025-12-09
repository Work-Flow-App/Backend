package com.workflow.service.auth;

import com.workflow.common.exception.business.InvalidPasswordResetTokenException;
import com.workflow.entity.PasswordResetToken;
import com.workflow.entity.User;
import com.workflow.repository.PasswordResetTokenRepository;
import com.workflow.repository.RefreshTokenRepository;
import com.workflow.repository.UserRepository;
import com.workflow.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${password-reset.token.expiration-minutes}")
    private int expirationMinutes;

    @Value("${password-reset.token.max-active-per-user}")
    private int maxActiveTokensPerUser;

    /**
     * Create password reset token and send email
     * Returns success message even if email doesn't exist (security)
     */
    @Transactional
    public void createPasswordResetToken(String email) {
        // Find user by email
        User user = userRepository.findByEmail(email).orElse(null);

        // If user doesn't exist, still return success (don't reveal user existence)
        if (user == null) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        // Check if user has too many active tokens
        long activeTokenCount = passwordResetTokenRepository.countActiveTokensByUser(user, LocalDateTime.now());
        if (activeTokenCount >= maxActiveTokensPerUser) {
            // Delete oldest unused tokens
            List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findActiveTokensByUser(user, LocalDateTime.now());
            activeTokens.stream()
                    .min((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()))
                    .ifPresent(oldest -> {
                        passwordResetTokenRepository.delete(oldest);
                        log.info("Deleted oldest password reset token for user: {}", user.getUsername());
                    });
        }

        // Generate 6-digit verification code
        String verificationCode = generateVerificationCode();

        // Create and save token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .verificationCode(verificationCode)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send email asynchronously
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), verificationCode);

        log.info("Password reset token created for user: {}", user.getUsername());
    }

    /**
     * Validate password reset code for a specific user (private helper method)
     */
    private void validateResetCode(PasswordResetToken resetToken, String email) {
        // Verify the code belongs to the user with this email
        if (!resetToken.getUser().getEmail().equalsIgnoreCase(email)) {
            log.warn("Verification code does not match email: {}", email);
            throw new InvalidPasswordResetTokenException("Invalid verification code");
        }

        if (!resetToken.isValid()) {
            if (resetToken.isUsed()) {
                log.warn("Attempted to use already used verification code for user: {}", resetToken.getUser().getUsername());
                throw new InvalidPasswordResetTokenException("Verification code has already been used");
            }
            if (resetToken.isExpired()) {
                log.warn("Attempted to use expired verification code for user: {}", resetToken.getUser().getUsername());
                throw new InvalidPasswordResetTokenException("Verification code has expired");
            }
        }

        log.debug("Verification code validated for user: {}", resetToken.getUser().getUsername());
    }

    /**
     * Reset password using verification code and email
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        log.info("Starting password reset for email: {}", email);

        // Find and validate token within the same transaction
        PasswordResetToken resetToken = passwordResetTokenRepository.findByVerificationCode(code)
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid verification code"));

        // Validate the token
        validateResetCode(resetToken, email);

        // Get user (EAGER fetch ensures user is loaded and managed)
        User user = resetToken.getUser();

        // Update password
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);

        // Mark token as used
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        // IMPORTANT: Flush all pending changes to database BEFORE bulk UPDATE query
        // This prevents the bulk UPDATE from clearing unsaved changes
        userRepository.flush();
        passwordResetTokenRepository.flush();

        // Revoke all refresh tokens (force re-login on all devices)
        // This uses a bulk UPDATE query which may clear the persistence context
        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());

        log.info("Password reset successfully for user: {}. All sessions invalidated.", user.getUsername());
    }

    /**
     * Cleanup expired and used tokens (scheduled job)
     */
    @Transactional
    public int cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1); // Delete tokens older than 1 day
        int deleted = passwordResetTokenRepository.deleteExpiredAndUsedTokens(cutoff);
        log.info("Cleaned up {} expired/used password reset tokens", deleted);
        return deleted;
    }

    /**
     * Revoke all unused tokens for a user
     */
    @Transactional
    public void revokeUserTokens(User user) {
        passwordResetTokenRepository.deleteUnusedTokensByUser(user);
        log.info("Revoked all unused password reset tokens for user: {}", user.getUsername());
    }

    /**
     * Generate a random 6-digit verification code
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Range: 100000 to 999999
        return String.valueOf(code);
    }
}