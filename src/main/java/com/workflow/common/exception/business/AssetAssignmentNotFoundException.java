package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class AssetAssignmentNotFoundException extends NotFoundException {
    public AssetAssignmentNotFoundException(String message) {
        super(message);
    }
}
