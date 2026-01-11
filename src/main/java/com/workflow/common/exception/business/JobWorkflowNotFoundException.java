package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class JobWorkflowNotFoundException extends NotFoundException {
    public JobWorkflowNotFoundException(String message) {
        super(message);
    }
}
