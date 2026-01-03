package com.workflow.common.exception.business;

import com.workflow.common.exception.base.*;

public class InvalidWorkerInvitationException extends BadRequestException {
    public InvalidWorkerInvitationException(String message) {
        super(message);
    }
}
