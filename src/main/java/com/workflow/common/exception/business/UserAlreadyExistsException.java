package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class UserAlreadyExistsException extends ConflictException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

