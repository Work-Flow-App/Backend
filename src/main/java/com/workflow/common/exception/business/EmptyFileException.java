package com.workflow.common.exception.business;

import com.workflow.common.exception.base.BadRequestException;

public class EmptyFileException extends BadRequestException {

    public EmptyFileException(String message) {
        super(message);
    }
}