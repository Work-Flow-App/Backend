package com.workflow.common.exception.business;

import com.workflow.common.exception.base.BadRequestException;

public class InvalidLeaveRequestStateException extends BadRequestException {
    public InvalidLeaveRequestStateException(String message) {
        super(message);
    }
}
