package com.workflow.scheduling;

import com.workflow.service.auth.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupJob {

    private final RefreshTokenService refreshTokenService;

    /**
     * Run cleanup every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        log.info("Starting refresh token cleanup job");
        int deleted = refreshTokenService.cleanupExpiredTokens();
        log.info("Refresh token cleanup job completed. Deleted {} tokens", deleted);
    }
}