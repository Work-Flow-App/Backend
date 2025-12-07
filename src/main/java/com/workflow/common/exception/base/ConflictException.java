package com.workflow.common.exception.base;

/**
 * Base exception for HTTP 409 Conflict responses.
 * Use this when a resource already exists or there's a conflict with current state.
 */
public abstract class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}