package com.workflow.service.notification;

import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.NotificationNotFoundException;
import com.workflow.dto.notification.CursorPagedResponse;
import com.workflow.dto.notification.NotificationResponse;
import com.workflow.entity.notification.Notification;
import com.workflow.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationManagementService implements INotificationManagementService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<NotificationResponse> getUserNotifications(Long userId, Boolean unreadOnly, Long cursor,
            int size) {

        // Fetch one extra item to determine if there is a next page
        Pageable limitOnly = PageRequest.of(0, size + 1);
        List<Notification> notifications;

        if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByCursorAndIsRead(userId, false, cursor, limitOnly);
        } else {
            notifications = notificationRepository.findByCursor(userId, cursor, limitOnly);
        }

        // Determine hasNext and strip the extra item
        boolean hasNext = notifications.size() > size;
        if (hasNext) {
            notifications.remove(notifications.size() - 1);
        }

        // Get the cursor for the next request (the ID of the last item in this batch)
        Long nextCursor = notifications.isEmpty() ? null : notifications.get(notifications.size() - 1).getId();

        List<NotificationResponse> data = notifications.stream()
                .map(this::mapToResponse)
                .toList();

        return new CursorPagedResponse<>(data, nextCursor, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenActionException("You are not authorized to modify this notification");
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now(ZoneOffset.UTC));
        notificationRepository.save(notification);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .targetUrl(n.getTargetUrl())
                .priority(n.getPriority())
                .metadata(n.getMetadata())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}