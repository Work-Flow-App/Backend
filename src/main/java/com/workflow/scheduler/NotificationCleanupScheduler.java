package com.workflow.scheduler;

import com.workflow.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    // Runs every day at 3:00 AM
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        log.info("Starting cleanup of read notifications older than {}", cutoff);

        int totalDeleted = 0;
        int deletedInBatch;

        do {
            // Delete 1000 records, commit the transaction, and return the deleted count
            deletedInBatch = notificationRepository.deleteReadNotificationsInBatches(cutoff);
            totalDeleted += deletedInBatch;

            if (deletedInBatch > 0) {
                log.debug("Deleted {} notifications in current batch. Total so far: {}", deletedInBatch, totalDeleted);

                // Give the database 50ms to process user traffic before hitting it with the
                // next delete
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Notification cleanup job was interrupted");
                    break;
                }
            }

            // If it deleted exactly 1000, there might be more. If it deleted < 1000, we're
            // done.
        } while (deletedInBatch == 1000);

        log.info("Successfully deleted a total of {} old notifications", totalDeleted);
    }
}