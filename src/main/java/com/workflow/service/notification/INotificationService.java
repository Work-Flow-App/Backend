package com.workflow.service.notification;

import com.workflow.common.constant.notification.NotificationPriority;
import com.workflow.common.constant.notification.NotificationType;
import com.workflow.entity.auth.User;
import java.util.Map;

public interface INotificationService {
    void createNotification(User recipient, NotificationType type, String title, String message,
            String targetUrl, String entityType, Long entityId,
            NotificationPriority priority, Map<String, Object> metadata);
}