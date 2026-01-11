package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class WorkflowStepNotFoundException extends NotFoundException {
    public WorkflowStepNotFoundException(String message) {
        super(message);
    }
}
