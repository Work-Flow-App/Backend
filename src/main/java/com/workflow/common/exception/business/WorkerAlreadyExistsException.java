package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class WorkerAlreadyExistsException extends ConflictException {
    public WorkerAlreadyExistsException(String message) {
        super(message);
    }
}