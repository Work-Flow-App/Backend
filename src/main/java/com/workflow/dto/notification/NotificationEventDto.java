package com.workflow.dto.notification;

import com.workflow.common.constant.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventDto {
    private Long userId;
    private String username;
    private NotificationType type;
    private NotificationResponse responsePayload;
}