package com.workflow.common.exception.business;

import com.workflow.common.exception.base.NotFoundException;

public class WorkerCertificateNotFoundException extends NotFoundException {
    public WorkerCertificateNotFoundException(String message) {
        super(message);
    }
}
