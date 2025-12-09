package com.workflow.common.exception.base;

/**
 * Base exception for HTTP 404 Not Found responses.
 * Use this when a requested resource doesn't exist.
 */
public abstract class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}