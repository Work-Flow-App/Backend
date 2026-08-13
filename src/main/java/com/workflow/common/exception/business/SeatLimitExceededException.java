package com.workflow.common.exception.business;

import com.workflow.common.exception.base.PaymentRequiredException;

public class SeatLimitExceededException extends PaymentRequiredException {
    public SeatLimitExceededException(String message) {
        super(message);
    }
}
