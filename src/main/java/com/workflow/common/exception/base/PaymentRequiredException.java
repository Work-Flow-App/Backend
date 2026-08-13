package com.workflow.common.exception.base;

/**
 * Base exception for HTTP 402 Payment Required responses.
 * Use this when the requested action is blocked by billing/plan state — a lapsed subscription,
 * or a plan-tier limit (jobs, seats, storage) that requires upgrading to proceed.
 * Does NOT extend any of the other base exceptions (NotFound/Conflict/BadRequest/Forbidden) —
 * none of those map to 402.
 */
public abstract class PaymentRequiredException extends RuntimeException {
    public PaymentRequiredException(String message) {
        super(message);
    }
}
