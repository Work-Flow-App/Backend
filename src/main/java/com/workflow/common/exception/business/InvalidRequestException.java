package com.workflow.common.exception.business;

import com.workflow.common.exception.base.BadRequestException;

/**
 * Generic bad-request exception for cases that do not warrant a more specific type.
 */
public class InvalidRequestException extends BadRequestException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
