package com.workflow.repository.auth;

import com.workflow.entity.auth.RefreshToken;
import com.workflow.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    List<RefreshToken> findByUser(User user);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt WHERE rt.user = :user")
    int revokeAllUserTokens(@Param("user") User user, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    int deleteExpiredAndRevokedTokens(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE refresh_tokens SET revoked = true, revoked_at = :now
            WHERE id = (
                SELECT id FROM (
                    SELECT id FROM refresh_tokens
                    WHERE user_id = :userId AND revoked = false AND expires_at > :now
                    ORDER BY created_at ASC LIMIT 1
                ) AS sub
            )
            """, nativeQuery = true)
    int revokeOldestActiveToken(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    boolean existsByTokenAndRevokedFalse(String token);
}