package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class LineItemNotFoundException extends NotFoundException {
    public LineItemNotFoundException(String message) {
        super(message);
    }
}
