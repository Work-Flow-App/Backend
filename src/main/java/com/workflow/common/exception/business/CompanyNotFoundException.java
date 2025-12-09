package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class CompanyNotFoundException extends NotFoundException {
    public CompanyNotFoundException(String message) {
        super(message);
    }
}