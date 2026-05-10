package com.workflow.common.exception.business;

import com.workflow.common.exception.base.ConflictException;

public class WorkflowInUseException extends ConflictException {
    public WorkflowInUseException(String message) {
        super(message);
    }
}
