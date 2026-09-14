package com.workflow.common.exception.business;

public class GroupInUseException extends RuntimeException {
    public GroupInUseException(String message) {
        super(message);
    }
}