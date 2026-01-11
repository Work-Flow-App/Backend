package com.workflow.common.exception.business;

import com.workflow.common.exception.base.ConflictException;

public class WorkflowNotStartedException extends ConflictException {
    public WorkflowNotStartedException(String message) {
        super(message);
    }
}
