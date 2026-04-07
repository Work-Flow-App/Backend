package com.workflow.repository.auth;

import com.workflow.entity.auth.EmailVerificationToken;
import com.workflow.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user = :user")
    void deleteAllByUser(@Param("user") User user);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM EmailVerificationToken t WHERE t.used = true OR t.expiresAt < :cutoff")
    int deleteExpiredAndUsedTokens(@Param("cutoff") LocalDateTime cutoff);
}
