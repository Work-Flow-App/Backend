package com.workflow.event.notification;

import com.workflow.dto.notification.NotificationEventDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationCreatedEvent extends ApplicationEvent {
    private final NotificationEventDto notificationDto;

    public NotificationCreatedEvent(Object source, NotificationEventDto notificationDto) {
        super(source);
        this.notificationDto = notificationDto;
    }
}