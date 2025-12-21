package com.workflow.common.exception.base;

/**
 * Base exception for HTTP 400 Bad Request responses.
 * Use this for invalid input, malformed requests, or invalid tokens.
 */
public abstract class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}