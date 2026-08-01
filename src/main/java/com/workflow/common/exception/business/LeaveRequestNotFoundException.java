package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class LeaveRequestNotFoundException extends NotFoundException {
    public LeaveRequestNotFoundException(String message) {
        super(message);
    }
}
