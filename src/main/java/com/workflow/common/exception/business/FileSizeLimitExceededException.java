package com.workflow.common.exception.business;

import com.workflow.common.exception.base.BadRequestException;

public class FileSizeLimitExceededException extends BadRequestException {

    public FileSizeLimitExceededException(String message) {
        super(message);
    }
}
