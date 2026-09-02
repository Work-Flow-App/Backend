package com.workflow.scheduler;

import com.workflow.common.constant.notification.NotificationPriority;
import com.workflow.common.constant.notification.NotificationType;
import com.workflow.entity.asset.AssetJobAssignment;
import com.workflow.entity.auth.User;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.service.notification.INotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetSlaBreachScheduler {

    private final AssetJobAssignmentRepository assignmentRepository;
    private final INotificationService notificationService;

    // Runs every 6 hours (21600000 ms) since we are dealing with 'Days'
    @Scheduled(fixedDelay = 21600000)
    @Transactional
    public void checkAssetSlaBreaches() {
        log.info("Running Asset SLA Breach Checker...");

        // Only pulls active assignments that have an expected duration and aren't
        // flagged yet
        List<AssetJobAssignment> activeAssignments = assignmentRepository
                .findActiveAssignmentsWithUnnotifiedSlaBreach();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<AssetJobAssignment> breachedAssignments = new ArrayList<>();

        for (AssetJobAssignment assignment : activeAssignments) {
            long daysElapsed = Duration.between(assignment.getAssignedAt(), now).toDays();

            if (daysElapsed > assignment.getExpectedDurationDays()) {
                // 1. Flip the circuit breaker
                assignment.setSlaBreached(true);

                // 2. "Log" it to history by appending to the notes field (No System User
                // required)
                String breachMessage = String.format(
                        "\n[SYSTEM ALERT - %s] SLA Breached! Asset has been checked out for %d days (Limit: %d days).",
                        now.toLocalDate().toString(), daysElapsed, assignment.getExpectedDurationDays());

                String currentNotes = assignment.getNotes() == null ? "" : assignment.getNotes();
                assignment.setNotes(currentNotes + breachMessage);

                breachedAssignments.add(assignment);
                log.warn("SLA Breach logged for Asset Assignment ID: {}", assignment.getId());

                User companyUser = assignment.getAsset().getCompany().getUser();
                String title = "Asset SLA Breached!";
                String notifMessage = String.format(
                        "Asset '%s' has been assigned for %d days, exceeding the %d days limit.",
                        assignment.getAsset().getName(), daysElapsed, assignment.getExpectedDurationDays());
                Map<String, Object> metadata = Map.of(
                        "assignmentId", assignment.getId(),
                        "assetId", assignment.getAsset().getId());

                // Notify Company Admin
                notificationService.createNotification(
                        companyUser, NotificationType.ASSET_SLA_BREACHED, title, notifMessage,
                        "/assets/" + assignment.getAsset().getId(),
                        "AssetJobAssignment", assignment.getId(), NotificationPriority.URGENT, metadata);

                // Notify Assigned Worker (if any)
                if (assignment.getAssignedWorker() != null) {
                    notificationService.createNotification(
                            assignment.getAssignedWorker().getUser(), NotificationType.ASSET_SLA_BREACHED, title,
                            notifMessage,
                            "/assets/" + assignment.getAsset().getId(),
                            "AssetJobAssignment", assignment.getId(), NotificationPriority.URGENT, metadata);
                }
            }
        }

        // 3. Batch save updates
        if (!breachedAssignments.isEmpty()) {
            assignmentRepository.saveAll(breachedAssignments);
        }
    }
}