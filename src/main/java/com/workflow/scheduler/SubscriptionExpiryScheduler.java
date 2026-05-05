package com.workflow.scheduler;

import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.repository.company.CompanySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryScheduler {

    private final CompanySubscriptionRepository subscriptionRepository;
    private final PaddleConfigProperties paddleProps;

    /**
     * Expires trials that have passed their trialEndsAt timestamp.
     * Runs daily at 02:00 UTC.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    public void expireTrials() {
        LocalDateTime now = LocalDateTime.now();
        List<CompanySubscription> expired = subscriptionRepository
                .findExpiredByStatus(SubscriptionStatus.TRIAL, now);

        if (expired.isEmpty()) {
            log.debug("expireTrials: no expired trials found");
            return;
        }

        for (CompanySubscription sub : expired) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
        }
        subscriptionRepository.saveAll(expired);

        log.info("expireTrials: expired {} trial subscriptions", expired.size());
    }

    /**
     * Expires PAST_DUE subscriptions that have exceeded the grace period.
     * Runs daily at 02:15 UTC (after expireTrials).
     */
    @Scheduled(cron = "0 15 2 * * *", zone = "UTC")
    @Transactional
    public void expirePastDue() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(paddleProps.getPastDueGraceDays());
        List<CompanySubscription> expired = subscriptionRepository.findExpiredPastDue(cutoff);

        if (expired.isEmpty()) {
            log.debug("expirePastDue: no past-due subscriptions beyond grace period");
            return;
        }

        for (CompanySubscription sub : expired) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
        }
        subscriptionRepository.saveAll(expired);

        log.info("expirePastDue: expired {} past-due subscriptions (grace={}d)", expired.size(),
                paddleProps.getPastDueGraceDays());
    }
}
