package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class WorkflowNotFoundException extends NotFoundException {
    public WorkflowNotFoundException(String message) {
        super(message);
    }
}
