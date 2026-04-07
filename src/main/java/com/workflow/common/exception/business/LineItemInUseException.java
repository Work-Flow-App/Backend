package com.workflow.common.exception.business;

import com.workflow.common.exception.base.ConflictException;

public class LineItemInUseException extends ConflictException {
    public LineItemInUseException(String message) {
        super(message);
    }
}
