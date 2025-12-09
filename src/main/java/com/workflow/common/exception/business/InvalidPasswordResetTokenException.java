package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class InvalidPasswordResetTokenException extends BadRequestException {
    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }
}