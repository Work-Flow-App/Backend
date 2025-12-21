package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class WorkerNotFoundException extends NotFoundException {
    public WorkerNotFoundException(String message) {
        super(message);
    }
}