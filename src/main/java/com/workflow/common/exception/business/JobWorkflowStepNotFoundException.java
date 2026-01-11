package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class JobWorkflowStepNotFoundException extends NotFoundException {
    public JobWorkflowStepNotFoundException(String message) {
        super(message);
    }
}
