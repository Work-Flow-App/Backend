package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class AssetNotFoundException extends NotFoundException {
    public AssetNotFoundException(String message) {
        super(message);
    }
}
