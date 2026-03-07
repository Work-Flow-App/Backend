package com.workflow.common.exception.business;

import com.workflow.common.exception.base.BadRequestException;

public class InvalidTimeLogException extends BadRequestException {
    public InvalidTimeLogException(String message) {
        super(message);
    }
}