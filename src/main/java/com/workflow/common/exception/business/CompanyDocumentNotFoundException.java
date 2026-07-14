package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class CompanyDocumentNotFoundException extends NotFoundException {
    public CompanyDocumentNotFoundException(String message) {
        super(message);
    }
}