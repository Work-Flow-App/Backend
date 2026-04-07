package com.workflow.common.exception.business;

import com.workflow.common.exception.base.UnauthorizedException;

public class InvalidRefreshTokenException extends UnauthorizedException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}