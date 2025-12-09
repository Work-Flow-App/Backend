package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class CompanyAlreadyExistsException extends ConflictException {
    public CompanyAlreadyExistsException(String message) {
        super(message);
    }
}