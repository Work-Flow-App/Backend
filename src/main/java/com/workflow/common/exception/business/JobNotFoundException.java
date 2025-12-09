package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class JobNotFoundException extends NotFoundException {
    public JobNotFoundException(String message) {
        super(message);
    }
}
