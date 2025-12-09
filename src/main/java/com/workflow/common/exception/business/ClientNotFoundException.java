package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class ClientNotFoundException extends NotFoundException {
    public ClientNotFoundException(String message) {
        super(message);
    }
}
