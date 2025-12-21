package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class InvalidRefreshTokenException extends BadRequestException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}