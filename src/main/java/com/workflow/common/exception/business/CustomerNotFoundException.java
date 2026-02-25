package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class CustomerNotFoundException extends NotFoundException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}