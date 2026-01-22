package com.workflow.common.exception.business;

import com.workflow.common.exception.base.BadRequestException;

/**
 * Exception thrown when attempting to delete a default template.
 * A default template cannot be deleted until another template is set as default.
 */
public class DefaultTemplateDeletionException extends BadRequestException {
    public DefaultTemplateDeletionException(String message) {
        super(message);
    }
}