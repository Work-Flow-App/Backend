package com.workflow.common.constant.workflow;

public enum SlaStatus {
    NOT_APPLICABLE,  // No SLA set or step hasn't started
    ON_TRACK,        // Running normally
    ATTENTION_NEEDED,// Passed expected duration, but not maximum
    BREACHED         // Passed maximum deadline
}