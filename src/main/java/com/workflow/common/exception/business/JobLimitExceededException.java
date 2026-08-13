package com.workflow.common.exception.business;

import com.workflow.common.exception.base.PaymentRequiredException;

public class JobLimitExceededException extends PaymentRequiredException {
    public JobLimitExceededException(String message) {
        super(message);
    }
}
