package com.workflow.common.constant.form;

public enum FormSubmissionStatus {
    DRAFT, // Company is filling parts before sending
    SENT, // Sent to worker, waiting for them to start
    IN_PROGRESS, // Worker has started filling it out
    SUBMITTED // Fully completed and submitted
}