package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class InvoiceNotFoundException extends NotFoundException {
    public InvoiceNotFoundException(String message) {
        super(message);
    }
}
