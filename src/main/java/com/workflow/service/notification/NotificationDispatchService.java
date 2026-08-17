package com.workflow.service.notification;

import com.workflow.dto.notification.NotificationEventDto;
import com.workflow.entity.notification.NotificationPreference;
import com.workflow.repository.notification.NotificationPreferenceRepository;
import com.workflow.event.notification.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("notificationExecutor")
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        // Retrieve the safe, detached DTO
        NotificationEventDto dto = event.getNotificationDto();
        Long userId = dto.getUserId();

        NotificationPreference pref = preferenceRepository
                .findByUserIdAndNotificationType(userId, dto.getType())
                .orElse(new NotificationPreference());

        if (pref.isInAppEnabled()) {
            dispatchToWebSocket(dto);
        }

        if (pref.isPushEnabled()) {
            dispatchToPushNotifications(dto);
        }

        if (pref.isEmailEnabled()) {
            dispatchToEmail(dto);
        }
    }

    private void dispatchToWebSocket(NotificationEventDto dto) {
        try {
            // No Hibernate proxy to worry about here!
            messagingTemplate.convertAndSendToUser(
                    dto.getUsername(),
                    "/queue/notifications",
                    dto.getResponsePayload());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user: {}", dto.getUsername(), e);
        }
    }

    private void dispatchToPushNotifications(NotificationEventDto dto) {
        // FCM logic using dto...
    }

    private void dispatchToEmail(NotificationEventDto dto) {
        // Email logic using dto...
    }
}