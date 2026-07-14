package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class CompanyPostNotFoundException extends NotFoundException {
    public CompanyPostNotFoundException(String message) {
        super(message);
    }
}