package com.workflow.repository;

import com.workflow.entity.PasswordResetToken;
import com.workflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find password reset token by verification code (global — retained for internal use)
     */
    Optional<PasswordResetToken> findByVerificationCode(String verificationCode);

    /**
     * Find password reset token scoped to a specific user by email — prevents cross-user code lookup
     */
    Optional<PasswordResetToken> findByVerificationCodeAndUser_Email(String verificationCode, String email);

    /**
     * Find all active (not used and not expired) tokens for a user
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.user = :user AND t.used = false AND t.expiresAt > :now")
    List<PasswordResetToken> findActiveTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Count active tokens for a user
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.user = :user AND t.used = false AND t.expiresAt > :now")
    long countActiveTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Delete all unused tokens for a user
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PasswordResetToken t WHERE t.user = :user AND t.used = false")
    void deleteUnusedTokensByUser(@Param("user") User user);

    /**
     * Delete expired and used tokens (cleanup job)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PasswordResetToken t WHERE t.used = true OR t.expiresAt < :cutoff")
    int deleteExpiredAndUsedTokens(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Find all tokens by user
     */
    List<PasswordResetToken> findByUser(User user);
}