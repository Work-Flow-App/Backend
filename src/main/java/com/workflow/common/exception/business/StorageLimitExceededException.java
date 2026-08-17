package com.workflow.common.exception.business;

import com.workflow.common.exception.base.PaymentRequiredException;

public class StorageLimitExceededException extends PaymentRequiredException {
    public StorageLimitExceededException(String message) {
        super(message);
    }
}
