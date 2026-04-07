package com.workflow.common.exception.business;

import com.workflow.common.exception.base.ForbiddenException;

public class UnauthorizedWorkflowAccessException extends ForbiddenException {
    public UnauthorizedWorkflowAccessException(String message) {
        super(message);
    }
}
