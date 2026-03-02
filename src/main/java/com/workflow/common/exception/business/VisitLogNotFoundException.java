package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class VisitLogNotFoundException extends NotFoundException {
    public VisitLogNotFoundException(String message) {
        super(message);
    }
}