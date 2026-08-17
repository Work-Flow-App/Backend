package com.workflow.service.notification;

import com.workflow.common.constant.notification.NotificationPriority;
import com.workflow.common.constant.notification.NotificationType;
import com.workflow.dto.notification.NotificationEventDto;
import com.workflow.dto.notification.NotificationResponse;
import com.workflow.entity.auth.User;
import com.workflow.entity.notification.Notification;
import com.workflow.repository.notification.NotificationRepository;
import com.workflow.event.notification.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void createNotification(User recipient, NotificationType type, String title, String message,
            String targetUrl, String entityType, Long entityId,
            NotificationPriority priority, Map<String, Object> metadata) {

        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .title(title)
                .message(message)
                .targetUrl(targetUrl)
                .entityType(entityType)
                .entityId(entityId)
                .priority(priority)
                .metadata(metadata)
                .build();

        notification = notificationRepository.save(notification);

        // 1. Build the payload for the WebSocket
        NotificationResponse payload = NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .targetUrl(notification.getTargetUrl())
                .priority(notification.getPriority())
                .metadata(notification.getMetadata())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();

        // 2. Build the Event DTO (safely extracting the username inside the
        // transaction)
        NotificationEventDto eventDto = NotificationEventDto.builder()
                .userId(recipient.getId())
                .username(recipient.getUsername())
                .type(notification.getType())
                .responsePayload(payload)
                .build();

        // 3. Publish the DTO, not the Entity
        eventPublisher.publishEvent(new NotificationCreatedEvent(this, eventDto));
    }
}