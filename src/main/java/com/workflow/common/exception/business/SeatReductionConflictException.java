package com.workflow.common.exception.business;

import com.workflow.common.exception.base.ConflictException;

/**
 * Thrown when a requested decrease in extraUserSeats would drop the effective seat limit below
 * the company's currently in-use seats (active workers + pending invitations). Distinct from
 * SeatLimitExceededException (402, "you need to buy more seats to proceed") — this is a 409:
 * the request is well-formed, but conflicts with current resource usage.
 */
public class SeatReductionConflictException extends ConflictException {
    public SeatReductionConflictException(String message) {
        super(message);
    }
}
