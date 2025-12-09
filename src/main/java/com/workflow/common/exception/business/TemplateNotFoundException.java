package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class TemplateNotFoundException extends NotFoundException {
    public TemplateNotFoundException(String message) {
        super(message);
    }
}
