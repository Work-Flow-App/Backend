package com.workflow.common.exception.business;

import com.workflow.common.exception.base.ConflictException;

public class LeaveRequestOverlapException extends ConflictException {
    public LeaveRequestOverlapException(String message) {
        super(message);
    }
}
