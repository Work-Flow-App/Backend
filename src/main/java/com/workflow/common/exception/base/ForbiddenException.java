package com.workflow.common.exception.base;

public abstract class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
