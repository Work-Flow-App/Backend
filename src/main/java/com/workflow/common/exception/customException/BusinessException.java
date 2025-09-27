package com.workflow.common.exception.customException;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
