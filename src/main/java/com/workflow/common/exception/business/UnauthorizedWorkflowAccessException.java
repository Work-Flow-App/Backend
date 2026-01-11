package com.workflow.common.exception.business;

import com.workflow.common.exception.base.BadRequestException;

public class UnauthorizedWorkflowAccessException extends BadRequestException {
    public UnauthorizedWorkflowAccessException(String message) {
        super(message);
    }
}
