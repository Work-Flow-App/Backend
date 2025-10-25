package com.workflow.scheduler;

import com.workflow.service.password.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to cleanup expired and used password reset tokens
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetTokenCleanupScheduler {

    private final PasswordResetService passwordResetService;

    /**
     * Run cleanup job daily at 2:00 AM
     * Deletes all expired and used tokens older than 1 day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of password reset tokens");

        try {
            int deletedCount = passwordResetService.cleanupExpiredTokens();
            log.info("Scheduled cleanup completed. Deleted {} password reset tokens", deletedCount);
        } catch (Exception e) {
            log.error("Error during scheduled password reset token cleanup", e);
        }
    }

    /**
     * Alternative: Run cleanup every 6 hours (uncomment to use)
     * Comment out the daily cron job above if using this
     */
    // @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    // public void cleanupExpiredTokensEvery6Hours() {
    //     log.info("Starting scheduled cleanup of password reset tokens (every 6 hours)");
    //     try {
    //         int deletedCount = passwordResetService.cleanupExpiredTokens();
    //         log.info("Scheduled cleanup completed. Deleted {} password reset tokens", deletedCount);
    //     } catch (Exception e) {
    //         log.error("Error during scheduled password reset token cleanup", e);
    //     }
    // }
}