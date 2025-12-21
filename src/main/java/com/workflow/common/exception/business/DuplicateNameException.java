package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class DuplicateNameException extends ConflictException {
    public DuplicateNameException(String message) {
        super(message);
    }
}

