package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class NotificationNotFoundException extends NotFoundException {
    public NotificationNotFoundException(String message) {
        super(message);
    }
}