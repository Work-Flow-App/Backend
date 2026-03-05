package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class EstimateNotFoundException extends NotFoundException {
    public EstimateNotFoundException(String message) {
        super(message);
    }
}
