package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

/**
 * Exception thrown when a template field is not found.
 */
public class FieldNotFoundException extends NotFoundException {
    public FieldNotFoundException(String message) {
        super(message);
    }
}
