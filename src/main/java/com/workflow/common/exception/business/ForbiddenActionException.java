package com.workflow.common.exception.business;

import com.workflow.common.exception.base.ForbiddenException;

public class ForbiddenActionException extends ForbiddenException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
