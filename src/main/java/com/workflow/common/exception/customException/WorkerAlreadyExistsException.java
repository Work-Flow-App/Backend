package com.workflow.common.exception.customException;

public class WorkerAlreadyExistsException extends RuntimeException {
    public WorkerAlreadyExistsException(String message) {
        super(message);
    }
}