package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class EstimateDocumentNotFoundException extends NotFoundException {
    public EstimateDocumentNotFoundException(String message) {
        super(message);
    }
}
