package com.workflow.common.exception.business;

import com.workflow.common.exception.base.UnauthorizedException;

public class InvalidGoogleTokenException extends UnauthorizedException {
    public InvalidGoogleTokenException(String message) {
        super(message);
    }
}
