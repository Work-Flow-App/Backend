package com.workflow.service.notification;

import com.workflow.dto.notification.CursorPagedResponse;
import com.workflow.dto.notification.NotificationResponse;

public interface INotificationManagementService {

    CursorPagedResponse<NotificationResponse> getUserNotifications(Long userId, Boolean unreadOnly, Long cursor,
            int size);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}