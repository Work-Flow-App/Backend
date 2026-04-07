package com.workflow.common.exception.base;

public abstract class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
