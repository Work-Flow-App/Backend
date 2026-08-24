package com.workflow.dto.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.workflow.common.constant.notification.NotificationPriority;
import com.workflow.common.constant.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;

    private NotificationType type;

    private String title;

    private String message;

    private String targetUrl;

    private NotificationPriority priority;

    private Map<String, Object> metadata;

    private boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
}