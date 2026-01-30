package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class WorkflowAlreadyStartedException extends ConflictException {
    public WorkflowAlreadyStartedException(String message) {
        super(message);
    }
}
