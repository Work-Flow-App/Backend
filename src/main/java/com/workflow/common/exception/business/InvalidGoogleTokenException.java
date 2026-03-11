package com.workflow.common.exception.business;

public class InvalidGoogleTokenException extends RuntimeException {
    public InvalidGoogleTokenException(String message) {
        super(message);
    }
}
